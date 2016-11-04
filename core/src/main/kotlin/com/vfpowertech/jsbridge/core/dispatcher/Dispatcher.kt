package com.vfpowertech.jsbridge.core.dispatcher

import org.slf4j.LoggerFactory
import java.util.*

class Dispatcher(private val engine: WebEngineInterface) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val services = HashMap<String, JSProxy>()

    private var nextCallbackId = 0
    private val pendingPromises = HashMap<String, PromiseCallbacks>()

    init {
        engine.register(this)
    }

    fun registerService(service: JSProxy) {
        registerService(service.name, service)
    }

    fun registerService(serviceName: String, service: JSProxy) {
        if (serviceName in services)
            throw RuntimeException("The service name $serviceName is already registered")
        services[serviceName] = service
    }

    fun unregisterService(serviceName: String): Boolean {
        return services.remove(serviceName) != null
    }

    /** Clears all pending promises. Callback ids aren't reset to avoid clashes with previously set ids. */
    fun resetState() {
        pendingPromises.clear()
    }

    //string to avoid having to recast constantly
    private fun getNextCallbackId(): String {
        val r = nextCallbackId
        ++nextCallbackId
        return r.toString()
    }

    fun call(serviceName: String, methodName: String, methodArgs: String, callbackId: String) {
        log.trace("js->native: {}.{}({}) -> {}", serviceName, methodName, methodArgs, callbackId)

        val service = services[serviceName]
        if (service == null) {
            log.error("Unknown service: {} for callbackId={}", serviceName, callbackId)
            //send error back to js
            return
        }

        service.call(methodName, methodArgs, callbackId)
    }

    fun sendValueBackToJS(callbackId: String, json: String?, isError: Boolean, serviceName: String?, methodName: String?) {
        val (serviceInfo, jsServiceInfo) = if (serviceName != null && methodName != null) {
            val info = "$serviceName.$methodName"
            " ($info)" to "\"$info\""
        }
        else
            "" to "null"

        log.trace("Dispatching <<<{}>>> for callbackId={}{}", json, callbackId, serviceInfo)
        //this embeds the json as object directly, so we don't need to bother deserializing it on the js side
        engine.runJS("window.dispatcher.sendValueToCallback(\"$callbackId\", $isError, $json, $jsServiceInfo);")
    }

    //target: something like window.service
    //methodArgs is json
    //need to register a callback with th
    fun callJS(serviceName: String, methodName: String, methodArgs: String, resolve: (String) -> Unit, reject: (String) -> Unit) {
        val callbackId = getNextCallbackId()
        log.trace("native->js: {}.{}({}) -> {}", serviceName, methodName, methodArgs, callbackId)
        //we add this first, as executeScript is sync
        pendingPromises[callbackId] = PromiseCallbacks(resolve, reject)
        //TODO catch exceptions and fail the promise?
        engine.runJS("window.dispatcher.handleCallFromNative(\"$serviceName\", \"$methodName\", $methodArgs, \"$callbackId\");")
    }

    fun callbackFromJS(callbackId: String, isError: Boolean, jsonRetVal: String) {
        log.trace("response from js: {} -> {}", callbackId, jsonRetVal)
        val callbacks = pendingPromises[callbackId]
        if (callbacks == null) {
            log.error("Value from js received for callbackId={}, but no pending request was found", callbackId)
            return
        }
        pendingPromises.remove(callbackId)

        try {
            if (!isError)
                callbacks.resolve(jsonRetVal)
            else
                callbacks.reject(jsonRetVal)
        }
        catch (e: Throwable) {
            log.error("Unhandled error while resolving callbackId={}", e)
            return
        }
    }
}