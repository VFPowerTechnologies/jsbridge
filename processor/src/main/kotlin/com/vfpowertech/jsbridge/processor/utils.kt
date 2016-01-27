package com.vfpowertech.jsbridge.processor

import org.apache.velocity.VelocityContext
import java.util.*
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.PrimitiveType
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror
import javax.lang.model.type.WildcardType
import javax.tools.Diagnostic

fun uncamel(s: CharSequence): String =
    kotlin.text.Regex("(?<!^)([A-Z])").replace(s, { m ->
        "-" + m.groups[1]!!.value.toLowerCase()
    }).toLowerCase()

/**
 * Returns true if the given type matches Promise<?, Exception>
 */
fun isValidPromiseType(processingEnv: ProcessingEnvironment, type: TypeMirror): Boolean {
    val typeUtils = processingEnv.typeUtils
    val elementUtils = processingEnv.elementUtils

    val promiseType = typeUtils.getDeclaredType(
        elementUtils.getTypeElement("nl.komponents.kovenant.Promise"),
        typeUtils.getWildcardType(null, null),
        elementUtils.getTypeElement("java.lang.Exception").asType()
    )

    return typeUtils.isAssignable(type, promiseType)
}

fun generateClassSpecFor(processingEnv: ProcessingEnvironment, cls: TypeElement): ClassSpec {
    val methods = ArrayList<MethodSpec>()

    fun logDebug(msg: String) {
        processingEnv.messager.printMessage(Diagnostic.Kind.NOTE, msg)
    }

    val className = cls.simpleName.toString()
    logDebug("Class $className")

    for (ee in cls.enclosedElements) {
        if (ee.kind != ElementKind.METHOD)
            continue

        ee as ExecutableElement

        val modifiers = ee.modifiers
        if (Modifier.PUBLIC !in modifiers || Modifier.STATIC in modifiers)
            continue

        val methodName = ee.simpleName.toString()
        val returnType = ee.returnType
        val params = ArrayList<ParamSpec>()

        for (p in ee.parameters) {
            val paramName = p.simpleName.toString()
            val type = p.asType()
            val isFuncType = isFunctionType(type)
            params.add(ParamSpec(paramName, p, type, isFuncType))
        }

        val returnsPromise = isValidPromiseType(processingEnv, returnType)
        val hasReturnValue = !isVoidType(processingEnv, if (!returnsPromise) {
            returnType
        }
        else {
            getPromiseReturnType(returnType as DeclaredType)
        })

        val methodSpec = MethodSpec(methodName, ee, params, returnType, hasReturnValue, returnsPromise)
        logDebug("$methodSpec")
        methods.add(methodSpec)
    }

    return ClassSpec(className, cls, cls.asType() as DeclaredType, methods)
}

/** Returns V from Promise<V, E> */
fun getPromiseReturnType(type: DeclaredType): TypeMirror =
    type.typeArguments.first()

fun getMethodArgsClassName(classSpec: ClassSpec, methodSpec: MethodSpec): String =
    "${classSpec.name}${methodSpec.name.capitalize()}Args"

/**
 * Returns true if the given type corresponds to a void type.
 * Supports java's void, java.lang.Void and kotlin.Unit.
 */
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

fun isFunctionType(mirror: TypeMirror): Boolean =
    mirror.toString().startsWith("kotlin.jvm.functions.Function")

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

fun generateCodeForMethodParams(context: GenerationContext, pkg: String, classSpec: ClassSpec, methodSpec: MethodSpec, e: TypeElement) {
    //don't generate empty params
    if (methodSpec.params.isEmpty())
        return

    val className = getMethodArgsClassName(classSpec, methodSpec)
    val fqn = "$pkg.$className"

    val vc = VelocityContext()
    vc.put("package", pkg)
    vc.put("className", className)
    vc.put("params", methodSpec.params)

    context.logInfo("Generating $fqn")

    context.writeTemplate(context.templates.args, fqn, e, vc)
}

/**
 * Returns (packageName, className) from a FQN.
 */
fun splitPackageClass(fqn: CharSequence): Pair<String, String> {
    val idx = fqn.lastIndexOf('.')
    if (idx <= 0)
        throw IllegalArgumentException("Annotated objects must be in a package: $fqn")
    val pkg = fqn.substring(0, idx)
    val className = fqn.substring(idx+1)
    return pkg to className
}
