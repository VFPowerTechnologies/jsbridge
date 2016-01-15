package com.vfpowertech.jsbridge.ios;

import org.robovm.apple.foundation.Foundation;
import org.robovm.apple.webkit.*;
import org.robovm.objc.annotation.Block;
import org.robovm.objc.block.VoidBlock1;

public class NavDelegate extends WKNavigationDelegateAdapter {
    @Override
    public void didFinishNavigation(WKWebView webView, WKNavigation navigation) {
        Foundation.log("Navigation completed");
        webView.evaluateJavaScript("appendSpan();", (obj, error) -> {
            Foundation.log("Invoked js");
        });
    }

    @Override
    public void decidePolicyForNavigationAction(WKWebView webView, WKNavigationAction navigationAction, @Block VoidBlock1<WKNavigationActionPolicy> decisionHandler) {
        Foundation.log("Navigation to %s".format(navigationAction.getRequest().getURL().toString()));
        decisionHandler.invoke(WKNavigationActionPolicy.Allow);
    }
}
