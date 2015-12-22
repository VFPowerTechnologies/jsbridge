package com.vfpowertech.jsbridge.desktop.console

import com.fasterxml.jackson.annotation.JsonProperty

data class ConsoleMessageAdded(
    @JsonProperty("message") val message: ConsoleMessage
)