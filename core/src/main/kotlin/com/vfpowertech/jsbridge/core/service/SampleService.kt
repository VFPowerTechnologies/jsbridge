package com.vfpowertech.jsbridge.core.service

import java.util.*

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