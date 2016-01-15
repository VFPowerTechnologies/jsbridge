package com.vfpowertech.jsbridge.ios;

import org.robovm.apple.foundation.Foundation;
import org.robovm.apple.foundation.NSObject;
import org.robovm.apple.webkit.WKScriptMessage;
import org.robovm.apple.webkit.WKScriptMessageHandlerAdapter;
import org.robovm.apple.webkit.WKUserContentController;

public class MessageHandler extends WKScriptMessageHandlerAdapter {
    @Override
    public void didReceiveScriptMessage(WKUserContentController wkUserContentController, WKScriptMessage wkScriptMessage) {
        Foundation.log("Called");
        String name = wkScriptMessage.getName();
        NSObject body = wkScriptMessage.getBody();
        if (!name.equals("native")) {
            Foundation.log("Unsupported handler");
            return;
        }
        Foundation.log(String.format("Message from js: %s/%s", name, body));
        Foundation.log("Done");
    }
}