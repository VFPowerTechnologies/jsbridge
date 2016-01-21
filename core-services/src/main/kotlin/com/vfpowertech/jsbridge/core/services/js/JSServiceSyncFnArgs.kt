package com.vfpowertech.jsbridge.core.services.js

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonPropertyOrder

//TODO don't generate args class if no args required
@JsonFormat(shape = JsonFormat.Shape.ARRAY)
@JsonPropertyOrder("v")
data class JSServiceSyncFnArgs(val v: V, val n: Int)