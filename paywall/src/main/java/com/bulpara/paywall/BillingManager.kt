package com.bulpara.paywall

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BillingManager(
    context: Context,
    private val productIds: ProductIds,
    private val verificationService: BillingVerificationService? = null,
) : PurchasesUpdatedListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var connectionTimeoutJob: kotlinx.coroutines.Job? = null

    private val _billingState = MutableStateFlow<BillingState>(BillingState.Disconnected)
    val billingState: StateFlow<BillingState> = _billingState.asStateFlow()

    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private val _products = MutableStateFlow<List<ProductDetails>>(emptyList())
    val products: StateFlow<List<ProductDetails>> = _products.asStateFlow()

    val monthlyProduct: ProductDetails?
        get() = _products.value.find { it.productId == productIds.monthly }

    val annualProduct: ProductDetails?
        get() = _products.value.find { it.productId == productIds.annual }

    private val premiumProducts = setOf(productIds.monthly, productIds.annual)

    private val appContext = context.applicationContext

    private var billingClient: BillingClient = buildClient()

    private fun buildClient(): BillingClient = BillingClient.newBuilder(appContext)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .enablePrepaidPlans()
                .build(),
        )
        .build()

    fun startConnection() {
        Log.d(TAG, "startConnection() called — current state: ${_billingState.value}")
        if (_billingState.value is BillingState.Connected ||
            _billingState.value is BillingState.Connecting
        ) {
            Log.d(TAG, "startConnection() SKIPPED — already ${_billingState.value}")
            return
        }

        _billingState.value = BillingState.Connecting

        // Guard against BillingClient silently hanging (common on first install)
        connectionTimeoutJob?.cancel()
        connectionTimeoutJob = scope.launch {
            delay(CONNECTION_TIMEOUT_MS)
            if (_billingState.value is BillingState.Connecting) {
                Log.d(TAG, "Connection timed out after ${CONNECTION_TIMEOUT_MS}ms — rebuilding client")
                _billingState.value = BillingState.Disconnected
                try { billingClient.endConnection() } catch (_: Exception) {}
                billingClient = buildClient()
                startConnection()
            }
        }

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                connectionTimeoutJob?.cancel()
                Log.d(TAG, "onBillingSetupFinished — responseCode: ${billingResult.responseCode}, message: ${billingResult.debugMessage}")
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    _billingState.value = BillingState.Connected
                    scope.launch { queryProductDetails() }
                    scope.launch { queryPurchases() }
                } else {
                    _billingState.value = BillingState.Error(
                        billingResult.debugMessage ?: "Billing setup failed",
                    )
                }
            }

            override fun onBillingServiceDisconnected() {
                connectionTimeoutJob?.cancel()
                Log.d(TAG, "onBillingServiceDisconnected — scheduling reconnect in ${RECONNECT_DELAY_MS}ms")
                _billingState.value = BillingState.Disconnected
                scope.launch {
                    delay(RECONNECT_DELAY_MS)
                    startConnection()
                }
            }
        })
    }

    fun endConnection() {
        billingClient.endConnection()
    }

    fun retryConnection() {
        Log.d(TAG, "retryConnection() called — state: ${_billingState.value}, products: ${_products.value.size}")
        if (_billingState.value !is BillingState.Connected) {
            startConnection()
        } else if (_products.value.isEmpty()) {
            scope.launch { queryProductDetails() }
        }
    }

    private suspend fun queryProductDetails() {
        Log.d(TAG, "queryProductDetails() called — querying: [${productIds.monthly}, ${productIds.annual}]")
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productIds.monthly)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productIds.annual)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            Log.d(TAG, "queryProductDetails() result — responseCode: ${billingResult.responseCode}, products: ${productDetailsList.size}, ids: ${productDetailsList.map { it.productId }}")
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                _products.value = productDetailsList
            } else {
                Log.e(TAG, "queryProductDetails() FAILED — ${billingResult.debugMessage}")
            }
        }
    }

    fun queryPurchases() {
        if (!billingClient.isReady) return

        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
        ) { billingResult, purchaseList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val hasActiveSub = purchaseList.any { purchase ->
                    purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                        purchase.products.any { it in premiumProducts }
                }
                _isPremium.value = hasActiveSub

                purchaseList
                    .filter {
                        it.purchaseState == Purchase.PurchaseState.PURCHASED &&
                            !it.isAcknowledged
                    }
                    .forEach { purchase -> acknowledgePurchase(purchase) }
            }
        }
    }

    fun launchPurchase(activity: Activity, productDetails: ProductDetails) {
        val offerToken = productDetails.subscriptionOfferDetails
            ?.firstOrNull { offer ->
                offer.pricingPhases.pricingPhaseList.any { it.priceAmountMicros == 0L }
            }?.offerToken
            ?: productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
            ?: return

        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
            .setOfferToken(offerToken)
            .build()

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()

        billingClient.launchBillingFlow(activity, billingFlowParams)
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?,
    ) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase ->
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        _isPremium.value = true
                        _billingState.value = BillingState.PurchaseSuccess
                        if (!purchase.isAcknowledged) {
                            acknowledgePurchase(purchase)
                        }
                    }
                }
            }

            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _billingState.value = BillingState.Cancelled
            }

            else -> {
                _billingState.value = BillingState.Error(
                    billingResult.debugMessage ?: "Purchase failed",
                )
            }
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.acknowledgePurchase(params) { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                verifyWithServer(purchase)
            }
        }
    }

    private fun verifyWithServer(purchase: Purchase) {
        val service = verificationService ?: return
        val productId = purchase.products.firstOrNull() ?: return
        scope.launch {
            try {
                service.verifyPurchase(
                    purchaseToken = purchase.purchaseToken,
                    productId = productId,
                )
            } catch (_: Exception) {
                // Fire-and-forget — BillingClient remains primary source of truth
            }
        }
    }

    // --- Price formatting helpers ---

    fun getFormattedPrice(productDetails: ProductDetails): String? {
        return productDetails.subscriptionOfferDetails
            ?.lastOrNull()
            ?.pricingPhases
            ?.pricingPhaseList
            ?.lastOrNull()
            ?.formattedPrice
    }

    fun getTrialPeriod(productDetails: ProductDetails): String? {
        val trialPhase = productDetails.subscriptionOfferDetails
            ?.firstOrNull()
            ?.pricingPhases
            ?.pricingPhaseList
            ?.firstOrNull { it.priceAmountMicros == 0L }
            ?: return null

        return trialPhase.billingPeriod.toReadablePeriod()
    }

    fun getMonthlyEquivalent(productDetails: ProductDetails): String? {
        val phase = productDetails.subscriptionOfferDetails
            ?.lastOrNull()
            ?.pricingPhases
            ?.pricingPhaseList
            ?.lastOrNull()
            ?: return null

        if (phase.billingPeriod == "P1Y") {
            val monthlyMicros = phase.priceAmountMicros / 12
            val monthlyDollars = monthlyMicros / 1_000_000.0
            val currencyCode = phase.priceCurrencyCode
            val formatted = String.format("%.2f", monthlyDollars)
            return "$formatted $currencyCode/mo"
        }
        return null
    }

    fun hasFreeTrial(productDetails: ProductDetails): Boolean {
        return productDetails.subscriptionOfferDetails?.any { offer ->
            offer.pricingPhases.pricingPhaseList.any { it.priceAmountMicros == 0L }
        } ?: false
    }

    companion object {
        private const val TAG = "BulparaPaywall"
        private const val RECONNECT_DELAY_MS = 3000L
        private const val CONNECTION_TIMEOUT_MS = 10_000L
    }
}

private fun String.toReadablePeriod(): String {
    return when {
        endsWith("D") -> {
            val days = removePrefix("P").removeSuffix("D").toIntOrNull() ?: return this
            "$days day${if (days > 1) "s" else ""}"
        }
        endsWith("W") -> {
            val weeks = removePrefix("P").removeSuffix("W").toIntOrNull() ?: return this
            "$weeks week${if (weeks > 1) "s" else ""}"
        }
        endsWith("M") && !contains("T") -> {
            val months = removePrefix("P").removeSuffix("M").toIntOrNull() ?: return this
            "$months month${if (months > 1) "s" else ""}"
        }
        endsWith("Y") -> {
            val years = removePrefix("P").removeSuffix("Y").toIntOrNull() ?: return this
            "$years year${if (years > 1) "s" else ""}"
        }
        else -> this
    }
}
