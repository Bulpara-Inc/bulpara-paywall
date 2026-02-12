package com.bulpara.paywall

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.ProductDetails
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class PaywallPlan {
    ANNUAL,
    MONTHLY,
}

data class PaywallUiState(
    val selectedPlan: PaywallPlan = PaywallPlan.ANNUAL,
    val isPremium: Boolean = false,
    val isLoading: Boolean = true,
    val billingState: BillingState = BillingState.Disconnected,
    val monthlyProduct: ProductDetails? = null,
    val annualProduct: ProductDetails? = null,
    val monthlyPrice: String = "",
    val annualPrice: String = "",
    val annualMonthlyEquivalent: String? = null,
    val annualHasFreeTrial: Boolean = false,
    val trialPeriod: String = "",
    val ctaText: String = "Subscribe Now",
    val error: String? = null,
)

class PaywallViewModel(
    private val billingManager: BillingManager,
    private val config: PaywallConfig,
) : ViewModel() {

    init {
        // Ensure billing connection is active when paywall is shown
        billingManager.retryConnection()
    }

    private val _selectedPlan = MutableStateFlow(PaywallPlan.ANNUAL)
    val selectedPlan: StateFlow<PaywallPlan> = _selectedPlan.asStateFlow()

    val uiState: StateFlow<PaywallUiState> = combine(
        _selectedPlan,
        billingManager.isPremium,
        billingManager.billingState,
        billingManager.products,
    ) { plan, premium, billingState, products ->
        val productIds = config.productIds
        val fallback = config.fallbackPricing
        val monthly = products.find { it.productId == productIds.monthly }
        val annual = products.find { it.productId == productIds.annual }

        val monthlyPrice = monthly?.let { billingManager.getFormattedPrice(it) } ?: fallback.monthlyPrice
        val annualPrice = annual?.let { billingManager.getFormattedPrice(it) } ?: fallback.annualPrice
        val annualMonthly = annual?.let { billingManager.getMonthlyEquivalent(it) }
        val hasTrial = annual?.let { billingManager.hasFreeTrial(it) } ?: fallback.annualHasFreeTrial
        val trialPeriod = annual?.let { billingManager.getTrialPeriod(it) } ?: fallback.trialPeriod

        val ctaText = when {
            plan == PaywallPlan.ANNUAL && hasTrial -> "Start Free Trial"
            else -> "Subscribe Now"
        }

        PaywallUiState(
            selectedPlan = plan,
            isPremium = premium,
            isLoading = false, // Never block UI — show fallback prices while connecting
            billingState = billingState,
            monthlyProduct = monthly,
            annualProduct = annual,
            monthlyPrice = monthlyPrice,
            annualPrice = annualPrice,
            annualMonthlyEquivalent = annualMonthly,
            annualHasFreeTrial = hasTrial,
            trialPeriod = trialPeriod,
            ctaText = ctaText,
            error = (billingState as? BillingState.Error)?.message,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PaywallUiState(),
    )

    fun selectPlan(plan: PaywallPlan) {
        _selectedPlan.value = plan
    }

    fun purchaseSelectedPlan(activity: Activity) {
        val product = when (_selectedPlan.value) {
            PaywallPlan.ANNUAL -> billingManager.annualProduct
            PaywallPlan.MONTHLY -> billingManager.monthlyProduct
        } ?: run {
            billingManager.retryConnection()
            return
        }
        billingManager.launchPurchase(activity, product)
    }

    fun retryConnection() {
        billingManager.retryConnection()
    }
}
