package com.vfpowertech.jsbridge.core.services

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.vfpowertech.jsbridge.core.services.JSCallbackInt

@JsonFormat(shape = JsonFormat.Shape.ARRAY)
@JsonPropertyOrder("listener")
data class SampleServiceAddListenerArgs(
    @JsonProperty("listener") val listener: JSCallbackInt
)