package com.vfpowertech.jsbridge.processor

import javax.lang.model.type.TypeMirror

data class MethodSpec(val name: String, val params: List<ParamSpec>, val returnType: TypeMirror) {
    val returnTypeFQN: String
        get() = returnType.toString()
}