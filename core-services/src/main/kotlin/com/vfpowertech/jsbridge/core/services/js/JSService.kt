package com.vfpowertech.jsbridge.core.services.js

import com.vfpowertech.jsbridge.processor.JSGenerate
import com.vfpowertech.jsbridge.processor.JSServiceName
import nl.komponents.kovenant.Promise

//need to define an interface of a js service, then generate an impl from it
@JSServiceName("jsService")
@JSGenerate
interface JSService {
    fun syncFn(v: V, n: Int): Promise<R, Exception>
    fun noArgsFn(): Promise<Unit, Exception>
    fun throwError(): Promise<Unit, Exception>
    fun missingJSMethod(): Promise<Unit, Exception>
    //fun asyncFn(v: Int): Promise<Int, Exception>
}