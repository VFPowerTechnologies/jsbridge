package com.vfpowertech.jsbridge.core.services.js.testing

import java.util.*

interface TestListener {
    fun testStarted(testSuite: TestSuite, test: Test)
    fun testFinished(testSuite: TestSuite, test: Test, result: TestResult)
}

/** Result of a single test run */
data class TestResult(val suiteName: String, val testName: String, val passed: Boolean, val exception: Throwable?)

/** Single test */
class Test(val name: String, val body: () -> Unit)

/** Collection of related tests */
class TestSuite(val name: String, val tests: List<Test>, val beforeEachHooks: ArrayList<() -> Unit>)

/** Collection of TestSuites */
class Tests(
    val testSuites: List<TestSuite>) {
    private val listeners = ArrayList<TestListener>()

    fun addListener(testListener: TestListener) {
        listeners.add(testListener)
    }

    fun run() {
        for (testSuite in testSuites) {
            for (test in testSuite.tests) {
                for (listener in listeners)
                    listener.testStarted(testSuite, test)

                for (beforeEach in testSuite.beforeEachHooks)
                    beforeEach()

                val result = try {
                    test.body()
                    TestResult(testSuite.name, test.name, true, null)
                }
                catch (e: Throwable) {
                    TestResult(testSuite.name, test.name, false, e)
                }

                for (listener in listeners)
                    listener.testFinished(testSuite, test, result)
            }
        }
    }
}

class TestsBuilder {
    val tests = ArrayList<Test>()
    val beforeEachHooks = ArrayList<() -> Unit>()

    fun beforeEach(body: () -> Unit) {
        beforeEachHooks.add(body)
    }

    fun it(name: String, testBody: () -> Unit) {
        tests.add(Test(name, testBody))
    }
}

class TestSuiteBuilder {
    val testSuites = ArrayList<TestSuite>()

    fun describe(name: String, init: TestsBuilder.() -> Unit) {
        val builder = TestsBuilder()
        builder.init()
        testSuites.add(TestSuite(name, builder.tests, builder.beforeEachHooks))
    }
}

/** Collects test results into a list */
class ResultCollectorTestListener : TestListener {
    val results = ArrayList<TestResult>()

    override fun testStarted(testSuite: TestSuite, test: Test) {
    }

    override fun testFinished(testSuite: TestSuite, test: Test, result: TestResult) {
        results.add(result)
    }
}

/** Utility function to return all test results in a list */
fun Tests.collectResults(): List<TestResult> {
    val collector = ResultCollectorTestListener()
    addListener(collector)
    run()
    return collector.results
}

/** DSL entry point */
fun declareTests(init: TestSuiteBuilder.() -> Unit): Tests {
    val builder = TestSuiteBuilder()
    builder.init()
    return Tests(builder.testSuites)
}
