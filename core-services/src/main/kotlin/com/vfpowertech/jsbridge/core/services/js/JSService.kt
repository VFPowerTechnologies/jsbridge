package com.vfpowertech.jsbridge.core.services.js

import com.vfpowertech.jsbridge.core.services.js.R
import com.vfpowertech.jsbridge.core.services.js.V
import nl.komponents.kovenant.Promise

//need to define an interface of a js service, then generate an impl from it
interface JSService {
    fun syncFn(v: V, n: Int): Promise<R, Exception>
    //fun asyncFn(v: Int): Promise<Int, Exception>
}