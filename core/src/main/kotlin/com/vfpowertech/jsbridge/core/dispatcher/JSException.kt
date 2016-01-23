package com.vfpowertech.jsbridge.core.dispatcher

import com.fasterxml.jackson.annotation.JsonProperty

class JSException(
    @JsonProperty("message") message: String,
    @JsonProperty("type") val type: String) : RuntimeException("$type: $message") {
    override fun toString(): String =
        "JSException($type): $message"
}