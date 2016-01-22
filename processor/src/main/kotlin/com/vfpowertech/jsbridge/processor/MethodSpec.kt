package com.vfpowertech.jsbridge.processor

import javax.lang.model.element.ExecutableElement
import javax.lang.model.type.TypeMirror

data class MethodSpec(
    val name: String,
    val element: ExecutableElement,
    val params: List<ParamSpec>,
    val returnType: TypeMirror,
    val hasReturnValue: Boolean
) {
    val returnTypeFQN: String
        get() = returnType.toString()
}