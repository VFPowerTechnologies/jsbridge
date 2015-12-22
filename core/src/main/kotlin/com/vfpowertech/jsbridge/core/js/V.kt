package com.vfpowertech.jsbridge.core.js

import com.fasterxml.jackson.annotation.JsonProperty

data class V(
    @JsonProperty("p") val p: Int,
    @JsonProperty("q") val q: Int
)