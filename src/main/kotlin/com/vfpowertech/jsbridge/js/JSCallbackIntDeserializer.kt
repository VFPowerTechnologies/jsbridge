package com.vfpowertech.jsbridge.js

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.vfpowertech.jsbridge.dispatcher.Dispatcher

class JSCallbackIntDeserializer : JsonDeserializer<JSCallbackInt>() {
    override fun deserialize(jp: JsonParser, ctxt: DeserializationContext): JSCallbackInt {
        val dispatcher = ctxt.findInjectableValue(Dispatcher::class.java.name, null, null) as Dispatcher
        return JSCallbackInt(jp.valueAsString, dispatcher)
    }
}