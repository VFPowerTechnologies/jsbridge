package com.vfpowertech.jsbridge.processor

import java.util.*
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.PrimitiveType
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror
import javax.lang.model.type.WildcardType

fun getMethodArgsClassName(classSpec: ClassSpec, methodSpec: MethodSpec): String =
    "${classSpec.name}${methodSpec.name.capitalize()}Args"

//supports both java's void+Void and kotlin's Unit
fun isVoidType(processingEnv: ProcessingEnvironment, mirror: TypeMirror): Boolean {
    val typeUtils = processingEnv.typeUtils
    val elementUtils = processingEnv.elementUtils

    val voidTypes = arrayListOf(
        typeUtils.getNoType(TypeKind.VOID),
        elementUtils.getTypeElement("java.lang.Void").asType(),
        elementUtils.getTypeElement("kotlin.Unit").asType())

    for (voidMirror in voidTypes) {
        if (typeUtils.isSameType(mirror, voidMirror))
            return true
    }

    return false
}

fun getFunctionTypes(mirror: TypeMirror): Pair<String, List<String>> {
    var argTypes = ArrayList<String>()
    //TODO make sure this is a fun subtype

    mirror as DeclaredType

    //TODO allow for bounds in invoke
    for (arg in mirror.typeArguments) {
        argTypes.add(getTypeWithoutBounds(arg))
    }

    //drop return type
    val retType = argTypes.last()
    argTypes.dropLast(1)

    return retType to argTypes
}

//only support functions with a single arg with a Unit return type right now
fun checkIfFunctionTypeIsSupported(fqn: String, mirror: TypeMirror): Boolean {
    val typeStr = mirror.toString()
    if (typeStr.startsWith("kotlin.jvm.functions.Function")) {
        if (!typeStr.startsWith("kotlin.jvm.functions.Function1"))
            throw IllegalArgumentException("$fqn: Functions with more than one arg aren't supported")

        mirror as DeclaredType
        val retMirror = mirror.typeArguments.last()
        val retStr = retMirror.toString()
        if (retStr != "? extends kotlin.Unit" && retStr != "kotlin.Unit")
            throw IllegalArgumentException("$fqn: Only Unit is supported as a return type, got $retStr")

        return true
    }

    return false
}

fun getTypeWithoutBounds(mirror: TypeMirror): String {
    fun buildTypeStr(mirror: TypeMirror, builder: StringBuilder) {
        when (mirror) {
            is DeclaredType -> {
                builder.append(mirror.asElement().toString())
                if (mirror.typeArguments.isNotEmpty()) {
                    builder.append('<')
                    for (arg in mirror.typeArguments) {
                        buildTypeStr(arg, builder)
                        builder.append(',')
                    }
                    builder.deleteCharAt(builder.lastIndex)
                    builder.append('>')
                }
            }

            is WildcardType -> {
                //get the type without bounds
                val wildMirror = (mirror.superBound ?: mirror.extendsBound)
                if (wildMirror == null)
                    builder.append('?')
                else
                    buildTypeStr(wildMirror, builder)
            }

            is PrimitiveType ->
                builder.append(mirror.toString())

            else -> throw IllegalArgumentException()
        }
    }

    val builder = StringBuilder()
    buildTypeStr(mirror, builder)
    return builder.toString()
}
