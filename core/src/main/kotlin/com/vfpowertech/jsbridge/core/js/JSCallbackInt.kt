package com.vfpowertech.jsbridge.core.js

import com.fasterxml.jackson.annotation.JacksonInject
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.vfpowertech.jsbridge.core.dispatcher.Dispatcher

//gonna need to generate all possible combinations of these...
//gonna need to hash all the param names together as well (for namespace issues)
@JsonDeserialize(using = JSCallbackIntDeserializer::class)
class JSCallbackInt(
    private val callbackId: String,
    @JacksonInject private val dispatcher: Dispatcher) : (Int) -> Unit {
    override fun invoke(p1: Int) {
        val json = ObjectMapper().writeValueAsString(p1)
        dispatcher.sendValueBackToJS(callbackId, json)
    }
}