package com.vfpowertech.jsbridge.processor

import java.util.*
import javax.annotation.processing.ProcessingEnvironment
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
2) Generate: *Args class, serializers for param types (recursively)
    serializers must be reused (similar to JSCallbacks)
    so type fqn -> serializer fqn
    serializers should only be applied to:
        struct-style classes? (do this to start, simplier; just generate a class with a JsonPropertyOrder)
        bean-style data classes (get/set method)
            this lets us add in method calls after
    check type for com.fasterxml.jackson.annotation.JsonSerialize, JsonProperty, JsonPropertyOrder
    ** don't need serializers for POJOs
3) Keep track of serializer classes, then add them to the object mapper at creation time
    probably need a static objectmapper/pkg for this; otherwise we need to recursively add every type
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

class ToJSCodeGenerator(private val processingEnv: ProcessingEnvironment) {
    fun generate(elements: Set<Element>) {
        val classes = ArrayList<ClassInfo>()
        val referencedTypes = HashMap<String, TypeMirror>()
        for (e in elements) {
            val classInfo = generateClassInfoFor(e as TypeElement)
            classes.add(classInfo)
            referencedTypes.putAll(classInfo.getReferencedTypes())
        }

        val serializers = generateSerializers(referencedTypes)
    }

    //class fqn->serializer class
    private fun generateSerializers(referencedTypes: MutableMap<String, TypeMirror>): Map<String, String> {
        println(referencedTypes)
        val serializers = HashMap<String, String>()

        return serializers
    }

    //assuming a Promise<V, E>, return V
    private fun extractReturnTypeValueType(mirror: DeclaredType): TypeMirror {
        return mirror.typeArguments.first()
    }

    private fun isValidReturnType(mirror: TypeMirror): Boolean {
        //TODO make sure this is a Promise<E, Exception>
        val promiseType = processingEnv.elementUtils.getTypeElement("nl.komponents.kovenant.Promise").asType()
        val typeUtils = processingEnv.typeUtils
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