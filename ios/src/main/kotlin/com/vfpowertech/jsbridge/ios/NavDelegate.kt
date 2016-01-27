package com.vfpowertech.jsbridge.ios

import com.vfpowertech.jsbridge.core.dispatcher.WebEngineInterface
import com.vfpowertech.jsbridge.core.services.js.JSTestService
import com.vfpowertech.jsbridge.core.services.js.testing.TestRunner
import org.robovm.apple.foundation.Foundation
import org.robovm.apple.webkit.*
import org.robovm.objc.annotation.Block
import org.robovm.objc.block.VoidBlock1
import org.slf4j.LoggerFactory

class NavDelegate(
        private val engineInterface: WebEngineInterface,
        private val jsTestService: JSTestService
) : WKNavigationDelegateAdapter() {
    private val log = LoggerFactory.getLogger(javaClass)
    override fun didFinishNavigation(webView: WKWebView, navigation: WKNavigation) {
        log.info("Navigation completed")
        val testRunner = TestRunner(engineInterface, jsTestService)
        Thread(testRunner).start()
    }

    override fun decidePolicyForNavigationAction(webView: WKWebView, navigationAction: WKNavigationAction, @Block decisionHandler: VoidBlock1<WKNavigationActionPolicy>) {
        Foundation.log(navigationAction.request.url.toString().format())
        decisionHandler.invoke(WKNavigationActionPolicy.Allow)
    }
}
