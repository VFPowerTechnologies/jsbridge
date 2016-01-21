package com.vfpowertech.jsbridge.processor

import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType

//TODO handle generics (List<Set<String>>, etc)
data class ClassSpec(
    val name: String,
    val element: TypeElement,
    val type: DeclaredType,
    val methods: List<MethodSpec>
) {
    val fqn: CharSequence
        get() = type.toString()

    fun asTypeElement(): TypeElement = type.asElement() as TypeElement
}