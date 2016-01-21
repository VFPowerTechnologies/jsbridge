package com.vfpowertech.jsbridge.ios

import com.vfpowertech.jsbridge.core.services.js.JSServiceImpl
import com.vfpowertech.jsbridge.core.services.js.V
import org.robovm.apple.foundation.Foundation
import org.robovm.apple.webkit.*
import org.robovm.objc.annotation.Block
import org.robovm.objc.block.VoidBlock1
import org.slf4j.LoggerFactory

class NavDelegate(private val jsService: JSServiceImpl) : WKNavigationDelegateAdapter() {
    private val log = LoggerFactory.getLogger(javaClass)
    override fun didFinishNavigation(webView: WKWebView, navigation: WKNavigation) {
        log.info("Navigation completed")
        jsService.syncFn(V(5, 6), 5) success {
            log.info("Result of syncFn: {}", it)
        } fail {
            log.info("syncFn failed: {}", it)
        }
    }

    override fun decidePolicyForNavigationAction(webView: WKWebView, navigationAction: WKNavigationAction, @Block decisionHandler: VoidBlock1<WKNavigationActionPolicy>) {
        Foundation.log(navigationAction.request.url.toString().format())
        decisionHandler.invoke(WKNavigationActionPolicy.Allow)
    }
}
