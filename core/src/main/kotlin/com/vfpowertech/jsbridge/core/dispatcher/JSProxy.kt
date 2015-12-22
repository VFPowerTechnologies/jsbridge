package com.vfpowertech.jsbridge.core.dispatcher

interface JSProxy {
    fun call(methodName: String, methodArgs: String, callbackId: String)
}