package com.vfpowertech.jsbridge.androidwebengine

import android.os.Handler
import android.webkit.JavascriptInterface
import android.webkit.WebView

class AndroidWebEngineInterface(private val webView: WebView) : com.vfpowertech.jsbridge.core.dispatcher.WebEngineInterface {
    private lateinit var dispatcher: com.vfpowertech.jsbridge.core.dispatcher.Dispatcher
    private val handler = Handler(webView.context.mainLooper)

    override fun runJS(js: String) {
        handler.post { webView.evaluateJavascript(js, null) }
    }

    override fun register(dispatcher: com.vfpowertech.jsbridge.core.dispatcher.Dispatcher) {
        this.dispatcher = dispatcher
        webView.addJavascriptInterface(this, "nativeDispatcher")
    }

    @JavascriptInterface
    fun call(serviceName: String, methodName: String, methodArgs: String, callbackId: String) {
        dispatcher.call(serviceName, methodName, methodArgs, callbackId)
    }

    @JavascriptInterface
    fun callbackFromJS(callbackId: String, isError: Boolean, jsonRetVal: String) {
        dispatcher.callbackFromJS(callbackId, isError, jsonRetVal)
    }
}