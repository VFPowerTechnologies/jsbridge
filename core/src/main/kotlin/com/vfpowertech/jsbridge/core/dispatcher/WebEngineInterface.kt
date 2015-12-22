package com.vfpowertech.jsbridge.core.dispatcher

interface WebEngineInterface {
    /**
     * Should evaluate the given JS code. This should not raise any exceptions.
     */
    fun runJS(js: String)

    /**
     * This should expose the dispatcher to the JS engine.
     */
    fun register(dispatcher: Dispatcher)
}