package com.vfpowertech.jsbridge.processor

import javax.lang.model.element.ExecutableElement
import javax.lang.model.type.TypeMirror

data class MethodSpec(
    val name: String,
    val element: ExecutableElement,
    val params: List<ParamSpec>,
    val returnType: TypeMirror,
    /**
     * For normal functions, this is whether or not the return type is void/unit.
     * If method returns a Promise, this indicates whether that promise returns void or not.
     */
    val hasReturnValue: Boolean,
    val returnsPromise: Boolean
) {
    val returnTypeFQN: String
        get() = returnType.toString()
}