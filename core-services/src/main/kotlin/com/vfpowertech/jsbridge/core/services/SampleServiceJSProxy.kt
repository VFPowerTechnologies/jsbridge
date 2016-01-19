package com.vfpowertech.jsbridge.core.services

import com.fasterxml.jackson.databind.InjectableValues
import com.fasterxml.jackson.databind.ObjectMapper
import com.vfpowertech.jsbridge.core.dispatcher.Dispatcher
import com.vfpowertech.jsbridge.core.dispatcher.JSProxy
import com.vfpowertech.jsbridge.core.services.SampleService
import org.slf4j.LoggerFactory

//two types of functions:
//sync
//async
//also, listeners
class SampleServiceJSProxy(private val base: SampleService, private val dispatcher: Dispatcher) : JSProxy {
    private val log = LoggerFactory.getLogger(javaClass)
    private val objectMapper = ObjectMapper()

    init {
        val injectionableValues = InjectableValues.Std()
        injectionableValues.addValue(Dispatcher::class.java, dispatcher)
        objectMapper.setInjectableValues(injectionableValues)
    }

    //TODO catch exceptions
    override fun call(methodName: String, methodArgs: String, callbackId: String) {
        //sync method, no args example
        if (methodName == "getValue") {
            val json = objectMapper.writeValueAsString(base.value)
            dispatcher.sendValueBackToJS(callbackId, json)
        }
        //sync method, args example
        else if (methodName == "setValue") {
            val args = objectMapper.readValue(methodArgs, SampleServiceSetValueArgs::class.java)
            base.value = args.value
            dispatcher.sendValueBackToJS(callbackId, null)
        }
        //listener registration method example
        else if (methodName == "addListener") {
            val args = objectMapper.readValue(methodArgs, SampleServiceAddListenerArgs::class.java)
            base.addListener(args.listener)
            dispatcher.sendValueBackToJS(callbackId, null)
        }
        else {
            log.error("Unknown method: {} for callbackId={}", methodName, callbackId)
        }
    }
}