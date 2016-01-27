package com.vfpowertech.jsbridge.ios

import com.vfpowertech.jsbridge.core.dispatcher.Dispatcher
import org.robovm.apple.foundation.NSBundle
import org.robovm.apple.foundation.NSURL
import org.robovm.apple.foundation.NSURLRequest
import org.robovm.apple.uikit.*
import org.robovm.apple.webkit.WKUserContentController
import org.robovm.apple.webkit.WKWebView
import org.robovm.apple.webkit.WKWebViewConfiguration
import java.io.File

class WebViewController : UIViewController() {
    override fun loadView() {
        val bounds = UIScreen.getMainScreen().bounds
        val contentView = UIView(bounds)
        contentView.backgroundColor = UIColor.darkGray()

        view = contentView

        val configuration = WKWebViewConfiguration()
        val userContentController = WKUserContentController()
        configuration.userContentController = userContentController

        val webView = WKWebView(contentView.frame, configuration)

        val engineInterface = IOSWebEngineInterface(webView)
        val dispatcher = Dispatcher(engineInterface)

        val testService = com.vfpowertech.jsbridge.core.services.TestService()
        dispatcher.registerService("TestService", com.vfpowertech.jsbridge.core.services.jstojava.TestServiceToJavaProxy(testService, dispatcher))

        val jsTestService = com.vfpowertech.jsbridge.core.services.js.javatojs.JSTestServiceToJSProxy(dispatcher)

        webView.navigationDelegate = NavDelegate(engineInterface, jsTestService)

        val path = NSBundle.getMainBundle().findResourcePath("index", "html")
        val f = File(path)
        webView.loadRequest(NSURLRequest(NSURL(f)))

        contentView.addSubview(webView)

        val runTestsBtn = UIButton()
        runTestsBtn.addOnTouchUpInsideListener({ control, ev ->

        })
        contentView.addSubview(runTestsBtn)
    }
}
