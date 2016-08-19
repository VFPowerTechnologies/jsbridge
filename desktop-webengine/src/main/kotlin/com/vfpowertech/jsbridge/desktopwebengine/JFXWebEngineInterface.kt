package com.vfpowertech.jsbridge.desktopwebengine

import com.vfpowertech.jsbridge.core.dispatcher.Dispatcher
import com.vfpowertech.jsbridge.core.dispatcher.WebEngineInterface
import javafx.application.Platform
import javafx.scene.web.WebEngine
import netscape.javascript.JSObject
import org.slf4j.LoggerFactory

class JFXWebEngineInterface(private val engine: WebEngine) : WebEngineInterface {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun runJS(js: String) {
        Platform.runLater {
            try {
                engine.executeScript(js)
            }
            catch (e: Throwable) {
                log.error("Script execution failed for: {}", js, e)
            }
        }
    }

    override fun register(dispatcher: Dispatcher) {
        val window = engine.executeScript("window") as JSObject
        window.setMember("nativeDispatcher", dispatcher)
    }
}