package com.bulpara.paywall

data class TieredPaywallConfig(
    val tiers: List<PaywallTier>,
    val branding: PaywallBranding,
    val defaultBillingPeriod: BillingPeriod = BillingPeriod.ANNUAL,
    val recommendedTierIndex: Int = 1,
    val termsUrl: String = "",
    val privacyUrl: String = "",
    val verificationService: BillingVerificationService? = null,
)

data class PaywallTier(
    val name: String,
    val badge: String? = null,
    val monthlyProductId: String,
    val annualProductId: String,
    val benefits: List<String>,
    val creditsLabel: String? = null,
    val fallbackMonthlyPrice: String = "",
    val fallbackAnnualPrice: String = "",
)

enum class BillingPeriod { MONTHLY, ANNUAL }
