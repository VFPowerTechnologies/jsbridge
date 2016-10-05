package com.vfpowertech.jsbridge.core.dispatcher

/**
 * All interaction with the Dispatcher should be done on the application's main thread.
 */
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