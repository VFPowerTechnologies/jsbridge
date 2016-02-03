package com.vfpowertech.jsbridge.androidwebengine

import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView

class AndroidWebEngineInterface(private val webView: WebView) : com.vfpowertech.jsbridge.core.dispatcher.WebEngineInterface {
    private lateinit var dispatcher: com.vfpowertech.jsbridge.core.dispatcher.Dispatcher
    private val mainLooper = webView.context.mainLooper
    private val handler = Handler(mainLooper)

    private fun isMainThread(): Boolean =
        Looper.myLooper() == mainLooper

    private inline fun runInMainThread(crossinline body: () -> Unit): Unit {
        if (isMainThread())
            body()
        else
            handler.post { body() }
    }

    override fun runJS(js: String) {
        runInMainThread { webView.evaluateJavascript(js, null) }
    }

    override fun register(dispatcher: com.vfpowertech.jsbridge.core.dispatcher.Dispatcher) {
        this.dispatcher = dispatcher
        webView.addJavascriptInterface(this, "nativeDispatcher")
    }

    @JavascriptInterface
    fun call(serviceName: String, methodName: String, methodArgs: String, callbackId: String) {
        runInMainThread {
            dispatcher.call(serviceName, methodName, methodArgs, callbackId)
        }
    }

    @JavascriptInterface
    fun callbackFromJS(callbackId: String, isError: Boolean, jsonRetVal: String) {
        runInMainThread {
            dispatcher.callbackFromJS(callbackId, isError, jsonRetVal)
        }
    }
}