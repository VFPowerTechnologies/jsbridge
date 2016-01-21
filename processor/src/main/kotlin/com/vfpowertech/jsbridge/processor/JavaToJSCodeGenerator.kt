package com.vfpowertech.jsbridge.processor

import java.util.*
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeMirror

//TODO handle generics (List<Set<String>>, etc)
data class ClassInfo(val name: String, val methods: List<MethodInfo>)
data class ParamInfo(val name: String, val type: TypeMirror) {
    val typeFQN: String
        get() = type.toString()
}
data class MethodInfo(val name: String, val params: List<ParamInfo>, val returnType: TypeMirror) {
    val returnTypeFQN: String
        get() = returnType.toString()
}

/*
1) find methods, their params and return types
2) Generate: *Args class
    design decision:
        no serializers are generated for your own types; if they're POJOs jackson can serializer them without any intervention
        otherwise, annotate them or just mixins as appropriate
    rationale:
        makes generation vastly simpler, as well as letting the library user make their own decisions about serialization when required
*/
fun ClassInfo.getReferencedTypes(): Map<String,  TypeMirror> {
    //can't compare TypeMirrors directly, as the docs state that there's no guarantee that the same type will always
    //be ref'ed by the same object
    //for our purposes so long as the TypeMirror conveys the same info it doesn't matter which one is kept
    val referencedTypes = HashMap<String, TypeMirror>()
    for (m in methods) {
        val returnType = m.returnType
        referencedTypes[returnType.toString()] = returnType

        for (p in m.params) {
            //TODO handle generics
            referencedTypes[p.type.toString()] = p.type
        }
    }

    return referencedTypes
}

class JavaToJSCodeGenerator(private val context: GenerationContext) {
    fun generate(elements: Set<Element>) {
        val classes = ArrayList<ClassInfo>()
        val referencedTypes = HashMap<String, TypeMirror>()
        for (e in elements) {
            val classInfo = generateClassInfoFor(e as TypeElement)
            classes.add(classInfo)
            referencedTypes.putAll(classInfo.getReferencedTypes())
        }
    }

    //assuming a Promise<V, E>, return V
    private fun extractReturnTypeValueType(mirror: DeclaredType): TypeMirror {
        return mirror.typeArguments.first()
    }

    private fun isValidReturnType(mirror: TypeMirror): Boolean {
        //TODO make sure this is a Promise<E, Exception>
        val promiseType = context.processingEnv.elementUtils.getTypeElement("nl.komponents.kovenant.Promise").asType()
        val typeUtils = context.processingEnv.typeUtils
        return typeUtils.isSubtype(typeUtils.erasure(mirror), typeUtils.erasure(promiseType))
    }

    private fun generateClassInfoFor(cls: TypeElement): ClassInfo {
        val className = cls.qualifiedName.toString()
        val methods = ArrayList<MethodInfo>()

        for (e in cls.enclosedElements) {
            if (e.kind != ElementKind.METHOD)
                continue
            e as ExecutableElement

            val methodName = e.simpleName.toString()
            val returnType = e.returnType
            val params = ArrayList<ParamInfo>()

            if (!isValidReturnType(returnType))
                throw IllegalArgumentException("$className.$methodName has an invalid return type; only methods with a return type of nl.komponents.kovenant.Promise are allowed")

            val actualReturnType = extractReturnTypeValueType(returnType as DeclaredType)

            for (pe in e.parameters) {
                val paramInfo = ParamInfo(pe.simpleName.toString(), pe.asType())
                params.add(paramInfo)
            }

            val methodInfo = MethodInfo(methodName, params, actualReturnType)
            methods.add(methodInfo)
        }

        return ClassInfo(className, methods)
    }
}