package com.vfpowertech.jsbridge.core.services

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder

@JsonFormat(shape = JsonFormat.Shape.ARRAY)
@JsonPropertyOrder("value")
data class SampleServiceSetValueArgs(
    @JsonProperty("value") val value: Int
)