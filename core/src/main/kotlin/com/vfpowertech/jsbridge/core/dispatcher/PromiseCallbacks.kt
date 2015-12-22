package com.vfpowertech.jsbridge.core.dispatcher

//for getting responses from native->js calls
data class PromiseCallbacks(
    val resolve: (String) -> Unit,
    val reject: (String) -> Unit
)