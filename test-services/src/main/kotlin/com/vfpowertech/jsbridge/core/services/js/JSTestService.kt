package com.vfpowertech.jsbridge.core.services.js

import com.vfpowertech.jsbridge.processor.annotations.JavaToJSGenerate
import com.vfpowertech.jsbridge.processor.annotations.JSServiceName
import nl.komponents.kovenant.Promise

@JSServiceName("jsTestService")
@JavaToJSGenerate
interface JSTestService {
    fun hasArgs(v: V, n: Int): Promise<R, Exception>
    fun noArgs(): Promise<Unit, Exception>
    fun rejectsPromise(): Promise<Unit, Exception>
    fun throwsError(): Promise<Unit, Exception>
    fun missingJSMethod(): Promise<Unit, Exception>
}