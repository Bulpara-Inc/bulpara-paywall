package com.bulpara.paywall

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

object PaywallEntryPoint {

    fun createBillingManager(context: Context, config: PaywallConfig): BillingManager {
        return BillingManager(
            context = context.applicationContext,
            productIds = config.productIds,
            verificationService = config.verificationService,
        )
    }

    fun createViewModelFactory(
        billingManager: BillingManager,
        config: PaywallConfig,
    ): ViewModelProvider.Factory {
        return object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return PaywallViewModel(
                    billingManager = billingManager,
                    productIds = config.productIds,
                ) as T
            }
        }
    }
}
