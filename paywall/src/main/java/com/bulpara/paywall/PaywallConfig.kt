package com.bulpara.paywall

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color

data class PaywallConfig(
    val productIds: ProductIds,
    val branding: PaywallBranding,
    val benefits: List<String>,
    val verificationService: BillingVerificationService? = null,
)

data class ProductIds(
    val monthly: String,
    val annual: String,
)

data class PaywallBranding(
    val title: String,
    @DrawableRes val logoResId: Int,
    val gradientColors: List<Color>,
    val accentColor: Color = Color(0xFFFFD700),
)

interface BillingVerificationService {
    suspend fun verifyPurchase(purchaseToken: String, productId: String): Boolean
}
