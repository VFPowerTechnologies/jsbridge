package com.vfpowertech.jsbridge.desktop

import com.vfpowertech.jsbridge.core.dispatcher.Dispatcher
import com.vfpowertech.jsbridge.core.dispatcher.WebEngineInterface
import javafx.scene.web.WebEngine
import netscape.javascript.JSObject

class JFXWebEngineInterface(private val engine: WebEngine) : WebEngineInterface {
    override fun runJS(js: String) {
        engine.executeScript(js)
    }

    override fun register(dispatcher: Dispatcher) {
        val window = engine.executeScript("window") as JSObject
        window.setMember("nativeDispatcher", dispatcher)
    }
}