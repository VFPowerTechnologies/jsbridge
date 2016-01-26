package com.vfpowertech.jsbridge.desktop

import com.fasterxml.jackson.databind.ObjectMapper
import com.vfpowertech.jsbridge.core.dispatcher.Dispatcher
import com.vfpowertech.jsbridge.core.dispatcher.JSException
import com.vfpowertech.jsbridge.core.dispatcher.WebEngineInterface
import com.vfpowertech.jsbridge.core.dispatcher.exceptionToJSONString
import com.vfpowertech.jsbridge.core.services.js.JSTestService
import com.vfpowertech.jsbridge.core.services.js.R
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
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Utility class to stringify exception for passing to js */
data class ToJSTestResult(val suiteName: String, val testName: String, val passed: Boolean, val exception: String?) {
    constructor(result: TestResult) : this(
        result.suiteName,
        result.testName,
        result.passed,
        if (result.exception != null) exceptionToJSONString(result.exception) else null)
}

class SendResultsToEngineTestListener(private val engine: WebEngineInterface) : TestListener {
    val objectMapper = ObjectMapper()

    override fun testStarted(testSuite: TestSuite, test: Test) {
    }

    override fun testFinished(testSuite: TestSuite, test: Test, result: TestResult) {
        val json = objectMapper.writeValueAsString(ToJSTestResult(result))

        engine.runJS("addResult($json);")
    }
}

class TestRunner(
    private val engineInterface: WebEngineInterface,
    private val jsTestService: JSTestService
) : Runnable {
    //TODO these don't have timeouts...
    private val tests = declareTests {
        describe("hasArgs") {
            it("should return a proper value") {
                val r = jsTestService.hasArgs(V(5, 6), 5).get()
                assertEquals(R(11, 6), r)
            }
        }

        describe("noArgs") {
            it("should return successfully") {
                jsTestService.noArgs().get()
            }
        }

        describe("rejectsPromise") {
            it("should throw a JSException") {
                val e = assertFailsWith(JSException::class) {
                    jsTestService.rejectsPromise().get()
                }
                assertEquals("Error", e.type)
            }
        }

        describe("throwsError") {
            it("should throw a JSException") {
                val e = assertFailsWith(JSException::class) {
                    jsTestService.throwsError().get()
                }
                assertEquals("Error", e.type)
            }
        }

        describe("missingJSMethod") {
            it("should throw a JSException") {
                val e = assertFailsWith(JSException::class) {
                    jsTestService.missingJSMethod().get()
                }
                assertEquals("MissingMethodError", e.type)
            }
        }
    }

    override fun run() {
        tests.addListener(SendResultsToEngineTestListener(engineInterface))

        engineInterface.runJS("clearSuites();")

        tests.run()
    }
}

class App : Application() {
    private val log = LoggerFactory.getLogger(javaClass)
    private lateinit var engineInterface: WebEngineInterface
    private lateinit var jsTestService: JSTestService

    private fun runTests() {
        val runner = TestRunner(engineInterface, jsTestService)
        Thread(runner).start()
    }

    override fun start(primaryStage: Stage) {
        val vb = VBox()

        val webview = WebView()
        VBox.setVgrow(webview, Priority.ALWAYS)
        vb.children.add(webview)

        val engine = webview.engine

        enableDebugger(engine)

        engineInterface = JFXWebEngineInterface(engine)
        val dispatcher = Dispatcher(engineInterface)

        val testService = com.vfpowertech.jsbridge.core.services.TestService()
        dispatcher.registerService("TestService", com.vfpowertech.jsbridge.core.services.jstojava.TestServiceToJavaProxy(testService, dispatcher))

        val btnBox = HBox()
        vb.children.add(btnBox)

        val runTestsBtn = Button("Run Java->JS Tests")
        btnBox.children.add(runTestsBtn)
        runTestsBtn.setOnAction { runTests() }

        jsTestService = com.vfpowertech.jsbridge.core.services.js.javatojs.JSTestServiceToJSProxy(dispatcher)

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
