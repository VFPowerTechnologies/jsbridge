package com.vfpowertech.jsbridge.android

import android.os.Handler
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.vfpowertech.jsbridge.core.dispatcher.Dispatcher
import com.vfpowertech.jsbridge.core.dispatcher.WebEngineInterface

class AndroidWebEngineInterface(private val webView: WebView) : WebEngineInterface {
    private lateinit var dispatcher: Dispatcher
    private val handler = Handler(webView.context.mainLooper)

    override fun runJS(js: String) {
        handler.post { webView.evaluateJavascript(js, null) }
    }

    override fun register(dispatcher: Dispatcher) {
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