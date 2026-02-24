package com.bulpara.paywall

import android.app.Activity
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.ProductDetails
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlin.math.roundToInt

data class TierDisplayData(
    val name: String,
    val badge: String?,
    val price: String,
    val period: String,
    val creditsLabel: String?,
    val benefits: List<String>,
    val isCurrentPlan: Boolean,
)

data class TieredPaywallUiState(
    val tiers: List<TierDisplayData> = emptyList(),
    val selectedTierIndex: Int = 0,
    val billingPeriod: BillingPeriod = BillingPeriod.ANNUAL,
    val isLoading: Boolean = true,
    val billingState: BillingState = BillingState.Disconnected,
    val ctaText: String = "Subscribe Now",
    val savingsPercent: Int? = null,
    val error: String? = null,
    val isPremium: Boolean = false,
    val activeTier: String? = null,
)

class TieredPaywallViewModel(
    private val billingManager: BillingManager,
    private val config: TieredPaywallConfig,
) : ViewModel() {

    init {
        Log.d(TAG, "TieredPaywallViewModel created")
        billingManager.retryConnection()
    }

    private val _selectedTierIndex = MutableStateFlow(config.recommendedTierIndex)
    val selectedTierIndex: StateFlow<Int> = _selectedTierIndex.asStateFlow()

    private val _billingPeriod = MutableStateFlow(config.defaultBillingPeriod)
    val billingPeriod: StateFlow<BillingPeriod> = _billingPeriod.asStateFlow()

    val uiState: StateFlow<TieredPaywallUiState> = combine(
        _selectedTierIndex,
        _billingPeriod,
        billingManager.billingState,
        billingManager.products,
        billingManager.activeTier,
    ) { selectedIdx, period, billingState, products, activeTier ->
        val isPremium = activeTier != null || billingManager.isPremium.value

        val tierDisplays = config.tiers.map { tier ->
            val monthlyProduct = products.find { it.productId == tier.monthlyProductId }
            val annualProduct = products.find { it.productId == tier.annualProductId }

            val (price, periodLabel) = when (period) {
                BillingPeriod.MONTHLY -> {
                    val p = monthlyProduct?.let { billingManager.getFormattedPrice(it) }
                        ?: tier.fallbackMonthlyPrice
                    p to "/month"
                }
                BillingPeriod.ANNUAL -> {
                    val p = annualProduct?.let { billingManager.getFormattedPrice(it) }
                        ?: tier.fallbackAnnualPrice
                    p to "/year"
                }
            }

            TierDisplayData(
                name = tier.name,
                badge = tier.badge,
                price = price,
                period = periodLabel,
                creditsLabel = tier.creditsLabel,
                benefits = tier.benefits,
                isCurrentPlan = activeTier == tier.name,
            )
        }

        val savingsPercent = calculateSavingsPercent(
            config.tiers.getOrNull(selectedIdx),
            products,
        )

        val selectedTier = config.tiers.getOrNull(selectedIdx)
        val ctaText = when {
            activeTier == selectedTier?.name -> "Current Plan"
            selectedTier != null -> "Subscribe to ${selectedTier.name}"
            else -> "Subscribe Now"
        }

        TieredPaywallUiState(
            tiers = tierDisplays,
            selectedTierIndex = selectedIdx,
            billingPeriod = period,
            isLoading = products.isEmpty() && billingState !is BillingState.Error,
            billingState = billingState,
            ctaText = ctaText,
            savingsPercent = savingsPercent,
            error = (billingState as? BillingState.Error)?.message,
            isPremium = isPremium,
            activeTier = activeTier,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = TieredPaywallUiState(
            selectedTierIndex = config.recommendedTierIndex,
            billingPeriod = config.defaultBillingPeriod,
        ),
    )

    fun selectTier(index: Int) {
        _selectedTierIndex.value = index
    }

    fun selectBillingPeriod(period: BillingPeriod) {
        _billingPeriod.value = period
    }

    fun purchaseSelectedTier(activity: Activity) {
        val tier = config.tiers.getOrNull(_selectedTierIndex.value) ?: return
        val productId = when (_billingPeriod.value) {
            BillingPeriod.MONTHLY -> tier.monthlyProductId
            BillingPeriod.ANNUAL -> tier.annualProductId
        }
        val product = billingManager.getProductDetails(productId) ?: run {
            billingManager.retryConnection()
            return
        }
        billingManager.launchPurchase(activity, product)
    }

    fun retryConnection() {
        billingManager.retryConnection()
    }

    private fun calculateSavingsPercent(
        tier: PaywallTier?,
        products: List<ProductDetails>,
    ): Int? {
        tier ?: return null
        val monthly = products.find { it.productId == tier.monthlyProductId } ?: return null
        val annual = products.find { it.productId == tier.annualProductId } ?: return null
        val monthlyMicros = billingManager.getPriceAmountMicros(monthly) ?: return null
        val annualMicros = billingManager.getPriceAmountMicros(annual) ?: return null

        val yearlyCostMonthly = monthlyMicros * 12
        if (yearlyCostMonthly <= 0) return null
        val savings = ((yearlyCostMonthly - annualMicros).toDouble() / yearlyCostMonthly * 100)
        return savings.roundToInt().takeIf { it > 0 }
    }

    companion object {
        private const val TAG = "BulparaPaywall"
    }
}
