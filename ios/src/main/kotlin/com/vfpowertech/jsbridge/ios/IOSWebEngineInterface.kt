package com.vfpowertech.jsbridge.ios

import com.vfpowertech.jsbridge.core.dispatcher.Dispatcher
import com.vfpowertech.jsbridge.core.dispatcher.WebEngineInterface
import org.robovm.apple.foundation.Foundation
import org.robovm.apple.foundation.NSArray
import org.robovm.apple.foundation.NSNumber
import org.robovm.apple.webkit.WKScriptMessage
import org.robovm.apple.webkit.WKScriptMessageHandlerAdapter
import org.robovm.apple.webkit.WKUserContentController
import org.robovm.apple.webkit.WKWebView
import org.slf4j.LoggerFactory

class IOSWebEngineInterface(private val webView: WKWebView) : WKScriptMessageHandlerAdapter(), WebEngineInterface {
    private val log = LoggerFactory.getLogger(javaClass)
    private lateinit var dispatcher: Dispatcher

    init {
        val userContentController = webView.configuration.userContentController
        //since there's no way to hook console.log
        userContentController.addScriptMessageHandler(this, "log")
        userContentController.addScriptMessageHandler(this, "call")
        userContentController.addScriptMessageHandler(this, "callbackFromJS")
    }

    override fun runJS(js: String) {
        webView.evaluateJavaScript(js, { obj, error ->
            if (error != null) {
                Foundation.log("Error during js execution: $error")
            }
        })
    }

    override fun register(dispatcher: Dispatcher) {
        this.dispatcher = dispatcher
    }

    override fun didReceiveScriptMessage(wkUserContentController: WKUserContentController, wkScriptMessage: WKScriptMessage) {
        val name = wkScriptMessage.name
        val body = wkScriptMessage.body
        when (name) {
            "call" -> {
                val args = body as NSArray<*>
                dispatcher.call(
                        args[0].toString(),
                        args[1].toString(),
                        args[2].toString(),
                        args[3].toString())
            }

            "callbackFromJS" -> {
                val args = body as NSArray<*>

                dispatcher.callbackFromJS(
                        args[0].toString(),
                        (args[1] as NSNumber).booleanValue(),
                        args[2].toString())
            }

            else -> log.error("Unsupported handler: {}", name)
        }
    }
}