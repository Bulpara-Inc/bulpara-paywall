package com.bulpara.paywall

sealed class BillingState {
    data object Disconnected : BillingState()
    data object Connecting : BillingState()
    data object Connected : BillingState()
    data object PurchaseSuccess : BillingState()
    data object Cancelled : BillingState()
    data class Error(val message: String) : BillingState()
}
