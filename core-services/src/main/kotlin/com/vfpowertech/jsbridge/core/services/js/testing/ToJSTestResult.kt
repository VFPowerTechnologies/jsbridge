package com.vfpowertech.jsbridge.core.services.js.testing

import com.fasterxml.jackson.annotation.JsonRawValue
import com.vfpowertech.jsbridge.core.dispatcher.exceptionToJSONString

/** Utility class to stringify exception for passing to js */
data class ToJSTestResult(
    val suiteName: String,
    val testName: String,
    val passed: Boolean,
    @JsonRawValue
    val exception: String?
) {
    constructor(result: TestResult) : this(
        result.suiteName,
        result.testName,
        result.passed,
        if (result.exception != null) exceptionToJSONString(result.exception) else null)
}