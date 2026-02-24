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
                    config = config,
                ) as T
            }
        }
    }

    fun createTieredBillingManager(
        context: Context,
        config: TieredPaywallConfig,
    ): BillingManager {
        val manager = BillingManager(
            context = context.applicationContext,
            productIds = ProductIds("", ""),
            verificationService = config.verificationService,
        )
        manager.configureTiers(config.tiers)
        return manager
    }

    fun createTieredViewModelFactory(
        billingManager: BillingManager,
        config: TieredPaywallConfig,
    ): ViewModelProvider.Factory {
        return object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return TieredPaywallViewModel(
                    billingManager = billingManager,
                    config = config,
                ) as T
            }
        }
    }
}
