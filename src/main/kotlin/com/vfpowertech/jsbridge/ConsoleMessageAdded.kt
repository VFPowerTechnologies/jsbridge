package com.vfpowertech.jsbridge

import com.fasterxml.jackson.annotation.JsonProperty

data class ConsoleMessageAdded(
    @JsonProperty("message") val message: ConsoleMessage
)