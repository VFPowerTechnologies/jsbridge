package com.vfpowertech.jsbridge.js

import com.fasterxml.jackson.annotation.JsonProperty

data class R(
    @JsonProperty("p") val p: Int,
    @JsonProperty("q") val q: Int
)