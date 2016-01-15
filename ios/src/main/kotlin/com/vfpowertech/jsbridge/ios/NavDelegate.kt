package com.vfpowertech.jsbridge.ios

import org.robovm.apple.foundation.Foundation
import org.robovm.apple.webkit.*
import org.robovm.objc.annotation.Block
import org.robovm.objc.block.VoidBlock1

class NavDelegate : WKNavigationDelegateAdapter() {
    override fun didFinishNavigation(webView: WKWebView, navigation: WKNavigation) {
        Foundation.log("Navigation completed")
        webView.evaluateJavaScript("appendSpan();") { obj, error -> Foundation.log("Invoked js") }
    }

    override fun decidePolicyForNavigationAction(webView: WKWebView, navigationAction: WKNavigationAction, @Block decisionHandler: VoidBlock1<WKNavigationActionPolicy>) {
        Foundation.log(navigationAction.request.url.toString().format())
        decisionHandler.invoke(WKNavigationActionPolicy.Allow)
    }
}
