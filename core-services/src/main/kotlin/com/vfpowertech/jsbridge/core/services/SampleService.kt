package com.vfpowertech.jsbridge.core.services

import java.util.*

//TODO async method
class SampleService {
    var value: Int = 0
    private val listeners = ArrayList<(Int) -> Unit>()

    fun addListener(listener: (Int) -> Unit) {
        listeners.add(listener)
    }

    fun callListeners(v: Int) {
        listeners.forEach { it(v) }
    }
}