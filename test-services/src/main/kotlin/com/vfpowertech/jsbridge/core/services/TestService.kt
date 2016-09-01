package com.vfpowertech.jsbridge.core.services

import com.vfpowertech.jsbridge.processor.annotations.Exclude
import com.vfpowertech.jsbridge.processor.annotations.JSToJavaGenerate
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.deferred
import java.util.*
import kotlin.concurrent.timerTask

@JSToJavaGenerate
class TestService {
    var value: Int = 0
    private val listeners = ArrayList<(Int) -> Unit>()
    private val noArgListeners = ArrayList<() -> Unit>()
    private val timer = Timer(true)

    private fun ignoreMe() {}

    /* Listener functions */
    fun addListener(listener: (Int) -> Unit) {
        listeners.add(listener)
    }

    fun addNoArgsListener(listener: () -> Unit) {
        noArgListeners.add(listener)
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

    fun syncReturnList(): List<Int> = arrayListOf(1, 2, 3)

    fun syncListArg(list: List<Int>): Int =
        list.sum()

    fun syncReturnMap(): Map<String, Int> =
        mapOf("a" to 1, "b" to 2)

    /* Async functions */

    fun asyncAdd(i: Int, j: Int): Promise<Int, Exception> {
        val d = deferred<Int, Exception>()

        val task = timerTask {
            d.resolve(i+j)
        }

        timer.schedule(task, 100)

        return d.promise
    }

    fun asyncThrow(): Promise<Unit, Exception> {
        return Promise.ofFail(RuntimeException("Java exception occured"))
    }

    fun asyncVoid(): Promise<Unit, Exception> {
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

    fun resetState() {
        value = 0
        listeners.clear()
    }

    fun callListeners(v: Int) {
        listeners.forEach { it(v) }
    }

    fun callNoArgsListeners() {
        noArgListeners.forEach { it() }
    }
}