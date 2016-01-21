package com.vfpowertech.jsbridge.processor

import javax.lang.model.type.TypeMirror

data class ParamSpec(val name: String, val type: TypeMirror) {
    val typeFQN: String
        get() = type.toString()
}