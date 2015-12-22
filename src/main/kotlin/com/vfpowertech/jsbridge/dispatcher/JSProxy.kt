package com.vfpowertech.jsbridge.dispatcher

interface JSProxy {
    fun call(methodName: String, methodArgs: String, callbackId: String)
}