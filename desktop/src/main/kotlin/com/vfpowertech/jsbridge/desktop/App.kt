package com.vfpowertech.jsbridge.desktop

import com.fasterxml.jackson.databind.ObjectMapper
import com.vfpowertech.jsbridge.core.dispatcher.Dispatcher
import com.vfpowertech.jsbridge.core.services.js.JSService
import com.vfpowertech.jsbridge.core.services.js.V
import com.vfpowertech.jsbridge.desktop.console.ConsoleMessageAdded
import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.scene.web.WebEngine
import javafx.scene.web.WebView
import javafx.stage.Stage
import org.slf4j.LoggerFactory

class App : Application() {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun start(primaryStage: Stage) {
        val vb = VBox()

        val webview = WebView()
        VBox.setVgrow(webview, Priority.ALWAYS)
        vb.children.add(webview)

        val engine = webview.engine

        enableDebugger(engine)

        val dispatcher = Dispatcher(JFXWebEngineInterface(engine))

        val testService = com.vfpowertech.jsbridge.core.services.TestService()
        dispatcher.registerService("TestService", com.vfpowertech.jsbridge.core.services.jstojava.TestServiceToJavaProxy(testService, dispatcher))

        val btnBox = HBox()
        vb.children.add(btnBox)

        val notifyBtn = Button("Notify")
        btnBox.children.add(notifyBtn)
        notifyBtn.setOnAction { testService.callListeners(5) }

        val jsService: JSService = com.vfpowertech.jsbridge.core.services.js.javatojs.JSServiceToJSProxy(dispatcher)
        val callBtn = Button("Call JS")
        btnBox.children.add(callBtn)
        callBtn.setOnAction {
            log.info("Attempting to call JS")

            jsService.syncFn(V(5, 6), 5) success {
                log.info("Result of syncFn: {}", it)
            } fail {
                log.info("syncFn failed: {}", it)
            }

            jsService.noArgsFn() success {
                log.info("noArgsFn succeeded: {}", it)
            } fail {
                log.info("noArgsFn failed: {}", it)
            }

            jsService.rejects() success {
                log.info("rejects succeeded: {}", it)
            } fail {
                log.info("rejects failed: {}", it.toString())
            }

            jsService.throwsError() success {
                log.info("throwsError succeeded: {}", it)
            } fail {
                log.info("throwsError failed: {}", it.toString())
            }

            jsService.missingJSMethod() success {
                log.info("missingMethod succeeded: {}", it)
            } fail {
                log.info("missingMethod failed: {}", it.toString())
            }
        }

        engine.load(javaClass.getResource("/index.html").toExternalForm())

        primaryStage.scene = Scene(vb, 852.0, 480.0)
        primaryStage.show()
    }

    private fun enableDebugger(engine: WebEngine) {
        val objectMapper = ObjectMapper()

        val debugger = engine.impl_getDebugger()
        debugger.isEnabled = true
        val jsLog = LoggerFactory.getLogger("Javascript")
        debugger.setMessageCallback { msg ->
            val root = objectMapper.readTree(msg)
            if (root.has("method")) {
                val method = root.get("method").asText()
                if (method == "Console.messageAdded") {
                    val message = objectMapper.convertValue(root.get("params"), ConsoleMessageAdded::class.java).message
                    val level = message.level
                    val text = "[{}:{}] {}"
                    val args = arrayOf(message.url, message.line, message.text)
                    if (level == "log")
                        jsLog.info(text, *args)
                    else if (level == "error")
                        jsLog.error(text, *args)
                    else
                        println("Unknown level: $level")

                }
            }
            null
        }
        debugger.sendMessage("{\"id\": 1, \"method\": \"Console.enable\"}")
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            launch(App::class.java, *args)
        }
    }
}