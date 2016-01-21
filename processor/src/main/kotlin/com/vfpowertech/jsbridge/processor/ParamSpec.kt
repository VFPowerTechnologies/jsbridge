package com.vfpowertech.jsbridge.processor

import javax.lang.model.element.VariableElement
import javax.lang.model.type.TypeMirror

data class ParamSpec(
    val name: String,
    val element: VariableElement,
    val type: TypeMirror
) {
    val typeFQN: String
        get() = type.toString()
}