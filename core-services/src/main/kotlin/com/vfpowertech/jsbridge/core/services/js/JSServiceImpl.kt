package com.vfpowertech.jsbridge.core.js

import com.fasterxml.jackson.databind.ObjectMapper
import com.vfpowertech.jsbridge.core.dispatcher.Dispatcher
import com.vfpowertech.jsbridge.core.services.js.JSService
import com.vfpowertech.jsbridge.core.services.js.JSServiceSyncFnArgs
import com.vfpowertech.jsbridge.core.services.js.R
import com.vfpowertech.jsbridge.core.services.js.V
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.deferred

//this should be generated, and registered with the dispatcher through some method without exposing the actual impl class
class JSServiceImpl(private val dispatcher: Dispatcher) : JSService {
    override fun noArgsFn(): Promise<Unit, Exception> {
        throw UnsupportedOperationException()
    }

    private val objectMapper = ObjectMapper()

    override fun syncFn(v: V, n: Int): Promise<R, Exception> {
        val d = deferred<R, Exception>()
        val args = objectMapper.writeValueAsString(JSServiceSyncFnArgs(v, n))
        //should generate these as methods instead? less mem usage if multiple callbacks to same function
        val resolve: (String) -> Unit = { jsonRetVal ->
            //don't need a special struct for this; just unserialize as whatever the ret type is
            val retVal = objectMapper.readValue(jsonRetVal, R::class.java)
            d.resolve(retVal)
        }
        val reject: (String) -> Unit = { jsonExc ->
            d.reject(RuntimeException(jsonExc))
        }
        dispatcher.callJS("window.jsService", "syncFn", args, resolve, reject)
        return d.promise
    }

    //override fun asyncFn(v: Int): Promise<Int, Exception> {
    //}
}