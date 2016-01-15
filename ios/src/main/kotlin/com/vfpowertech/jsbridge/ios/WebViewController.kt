package com.vfpowertech.jsbridge.ios

import org.robovm.apple.foundation.Foundation
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
        userContentController.addScriptMessageHandler(MessageHandler(), "native")
        configuration.userContentController = userContentController

        val webView = WKWebView(contentView.frame, configuration)

        webView.navigationDelegate = NavDelegate()

        val path = NSBundle.getMainBundle().findResourcePath("index", "html", "www")
        val f = File(path)
        webView.loadRequest(NSURLRequest(NSURL(f)))

        //NSURL url = new NSURL("http://www.google.com");
        //webView.loadRequest(new NSURLRequest(url));

        contentView.addSubview(webView)

        Foundation.log("Here")
    }
}
