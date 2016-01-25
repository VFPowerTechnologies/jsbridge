package com.vfpowertech.jsbridge.core.services

import com.vfpowertech.jsbridge.processor.Exclude
import com.vfpowertech.jsbridge.processor.Generate
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.deferred
import java.util.*
import kotlin.concurrent.timerTask

@Generate
class TestService {
    var value: Int = 0
    private val listeners = ArrayList<(Int) -> Unit>()
    private val timer = Timer(true)

    /* Listener functions */
    fun addListener(listener: (Int) -> Unit) {
        listeners.add(listener)
    }

    /* Sync functions */
    fun syncAdd(i: Int, j: Int): Int {
        return i + j
    }

    fun syncThrow(): Int {
        throw RuntimeException("Java exception occured")
    }

    fun syncVoid() {
    }

    /* Async functions */

    fun asyncAdd(i: Int, j: Int): Promise<Int, Exception> {
        println("Scheduling timer")

        val d = deferred<Int, Exception>()

        val task = timerTask {
            println("Timer fired")
            d.resolve(i+j)
        }

        timer.schedule(task, 500)

        return d.promise
    }

    fun asyncThrow(): Promise<Unit, Exception> {
        return Promise.ofFail(RuntimeException("asyncThrow exception"))
    }

    fun asyncVoid(i: Int): Promise<Unit, Exception> {
        println("void async")
        return Promise.ofSuccess(Unit)
    }

    /* Invalid signatures */

    @Exclude
    fun badArgCount(listener: (Int, String) -> Unit) {

    }

    @Exclude
    fun badRetType(listener: (Int) -> Int) {

    }

    @Exclude
    fun badPromiseErrorType(): Promise<Int, Int> {
        return Promise.ofSuccess(1)
    }

    /* Utils */

    @Exclude
    fun callListeners(v: Int) {
        listeners.forEach { it(v) }
    }
}