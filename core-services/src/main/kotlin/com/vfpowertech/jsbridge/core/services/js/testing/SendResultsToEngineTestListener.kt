package com.vfpowertech.jsbridge.core.services.js.testing

import com.fasterxml.jackson.databind.ObjectMapper
import com.vfpowertech.jsbridge.core.dispatcher.WebEngineInterface

/** Works in tandem with java-test-helpers.js to ship test results into JS */
class SendResultsToEngineTestListener(private val engine: WebEngineInterface) : TestListener {
    val objectMapper = ObjectMapper()

    override fun testStarted(testSuite: TestSuite, test: Test) {
    }

    override fun testFinished(testSuite: TestSuite, test: Test, result: TestResult) {
        val json = objectMapper.writeValueAsString(ToJSTestResult(result))

        engine.runJS("addResult($json);")
    }
}