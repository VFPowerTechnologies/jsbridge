package com.vfpowertech.jsbridge.ios

import com.vfpowertech.jsbridge.core.dispatcher.Dispatcher
import com.vfpowertech.jsbridge.core.js.JSServiceImpl
import com.vfpowertech.jsbridge.core.service.SampleService
import com.vfpowertech.jsbridge.core.service.SampleServiceJSProxy
import org.robovm.apple.foundation.NSBundle
import org.robovm.apple.foundation.NSURL
import org.robovm.apple.foundation.NSURLRequest
import org.robovm.apple.uikit.UIColor
import org.robovm.apple.uikit.UIScreen
import org.robovm.apple.uikit.UIView
import org.robovm.apple.uikit.UIViewController
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

        val dispatcher = Dispatcher(IOSWebEngineInterface(webView))

        val sampleService = SampleService()
        dispatcher.registerService("SampleService", SampleServiceJSProxy(sampleService, dispatcher))

        val jsService = JSServiceImpl(dispatcher)

        webView.navigationDelegate = NavDelegate(jsService)

        val path = NSBundle.getMainBundle().findResourcePath("index", "html")
        val f = File(path)
        webView.loadRequest(NSURLRequest(NSURL(f)))

        contentView.addSubview(webView)
    }
}
