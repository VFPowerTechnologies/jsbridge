package com.vfpowertech.jsbridge.core.services.js.testing

import com.vfpowertech.jsbridge.core.dispatcher.JSException
import com.vfpowertech.jsbridge.core.dispatcher.WebEngineInterface
import com.vfpowertech.jsbridge.core.services.js.JSTestService
import com.vfpowertech.jsbridge.core.services.js.R
import com.vfpowertech.jsbridge.core.services.js.V
import kotlin.test.assertEquals

/** Runs some tests against JSTestService and ships the results into the mocha test runner ui */
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
                val e = kotlin.test.assertFailsWith(JSException::class) {
                    jsTestService.rejectsPromise().get()
                }
                assertEquals("Error", e.type)
            }
        }

        describe("throwsError") {
            it("should throw a JSException") {
                val e = kotlin.test.assertFailsWith(JSException::class) {
                    jsTestService.throwsError().get()
                }
                assertEquals("Error", e.type)
            }
        }

        describe("missingJSMethod") {
            it("should throw a JSException") {
                val e = kotlin.test.assertFailsWith(JSException::class) {
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