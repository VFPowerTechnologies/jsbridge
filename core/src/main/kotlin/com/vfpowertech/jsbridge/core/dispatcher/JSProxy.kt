package com.vfpowertech.jsbridge.core.dispatcher

interface JSProxy {
    val name: String

    fun call(methodName: String, methodArgs: String, callbackId: String)
}