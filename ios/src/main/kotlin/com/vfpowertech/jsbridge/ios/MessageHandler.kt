package com.vfpowertech.jsbridge.ios

import org.robovm.apple.foundation.Foundation
import org.robovm.apple.webkit.WKScriptMessage
import org.robovm.apple.webkit.WKScriptMessageHandlerAdapter
import org.robovm.apple.webkit.WKUserContentController

class MessageHandler : WKScriptMessageHandlerAdapter() {
    override fun didReceiveScriptMessage(wkUserContentController: WKUserContentController, wkScriptMessage: WKScriptMessage) {
        Foundation.log("Called")
        val name = wkScriptMessage.name
        val body = wkScriptMessage.body
        if (name != "native") {
            Foundation.log("Unsupported handler")
            return
        }
        Foundation.log("Message from js: %s/%s".format(name, body))
        Foundation.log("Done")
    }
}