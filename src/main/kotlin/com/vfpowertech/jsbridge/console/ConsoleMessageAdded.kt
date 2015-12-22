package com.vfpowertech.jsbridge.console

import com.fasterxml.jackson.annotation.JsonProperty

data class ConsoleMessageAdded(
    @JsonProperty("message") val message: ConsoleMessage
)