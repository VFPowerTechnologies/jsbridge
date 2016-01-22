package com.vfpowertech.jsbridge.core.services

import com.vfpowertech.jsbridge.processor.Exclude
import com.vfpowertech.jsbridge.processor.Generate
import java.util.*

//TODO async method
@Generate
class SampleService {
    var value: Int = 0
    private val listeners = ArrayList<(Int) -> Unit>()

    fun addListener(listener: (Int) -> Unit) {
        listeners.add(listener)
    }

    fun throwException() {
        throw RuntimeException("Java exception occured")
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