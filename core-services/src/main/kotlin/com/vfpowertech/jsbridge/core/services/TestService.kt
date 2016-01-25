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

    fun addListener(listener: (Int) -> Unit) {
        listeners.add(listener)
    }

    fun throwException() {
        throw RuntimeException("Java exception occured")
    }

    fun asyncMethod(i: Int, j: Int): Promise<Int, Exception> {
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

    fun asyncVoidMethod(i: Int): Promise<Unit, Exception> {
        println("void async")
        return Promise.ofSuccess(Unit)
    }

    @Exclude
    fun badArgCount(listener: (Int, String) -> Unit) {

    }

    @Exclude
    fun badRetType(listener: (Int) -> Int) {

    }

    @Exclude
    fun callListeners(v: Int) {
        listeners.forEach { it(v) }
    }
}