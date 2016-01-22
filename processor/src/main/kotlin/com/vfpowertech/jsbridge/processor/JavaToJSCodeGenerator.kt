package com.vfpowertech.jsbridge.processor

import org.apache.velocity.VelocityContext
import java.util.*
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeMirror

fun ClassSpec.getReferencedTypes(): Map<String,  TypeMirror> {
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

/*
1) find methods, their params and return types
2) Generate: *Args class
    design decision:
        no serializers are generated for your own types; if they're POJOs jackson can serializer them without any intervention
        otherwise, annotate them or just mixins as appropriate
    rationale:
        makes generation vastly simpler, as well as letting the library user make their own decisions about serialization when required
*/
class JavaToJSCodeGenerator(private val context: GenerationContext) {
    fun generate(elements: Set<Element>) {
        val classes = ArrayList<ClassSpec>()
        val referencedTypes = HashMap<String, TypeMirror>()
        for (e in elements) {
            val classInfo = validateClassSpec(generateClassSpecFor(e as TypeElement))
            classes.add(classInfo)
            referencedTypes.putAll(classInfo.getReferencedTypes())
        }

        for (classSpec in classes) {
            val serviceName = classSpec.element.getAnnotation(JSServiceName::class.java)?.value
            if (serviceName == null) {
                context.logError("${classSpec.fqn} lacks a JSServiceName annotation")
                return
            }

            val fqn = classSpec.fqn
            val (pkg, className) = splitPackageClass(fqn)

            for (methodSpec in classSpec.methods) {
                val generatedPkg = "$pkg.javaproxy"
                generateCodeForMethodParams(context, generatedPkg, classSpec, methodSpec, classSpec.asTypeElement())
            }

            val generatedClassName = "${className}Proxy"
            //TODO make option
            val generatedPackage = "$pkg.javaproxy"
            val generatedClassFQN = "$generatedPackage.$generatedClassName"

            val vc = VelocityContext()
            vc.put("package", generatedPackage)
            vc.put("className", generatedClassName)
            vc.put("utils", object {
                fun getMethodArgsClassName(methodSpec: MethodSpec): String =
                    getMethodArgsClassName(classSpec, methodSpec)
            })
            vc.put("methods", classSpec.methods)
            vc.put("serviceInterface", classSpec.type)
            vc.put("serviceName", serviceName)

            context.logInfo("Generating $generatedClassFQN")
            context.writeTemplate(context.templates.javaToJSProxy, generatedClassFQN, classSpec.element, vc)
        }
    }

    //assuming a Promise<V, E>, return V
    private fun extractReturnTypeValueType(mirror: DeclaredType): TypeMirror {
        return mirror.typeArguments.first()
    }

    private fun isValidReturnType(mirror: TypeMirror): Boolean {
        //TODO make sure this is a Promise<?, Exception>
        val promiseType = context.processingEnv.elementUtils.getTypeElement("nl.komponents.kovenant.Promise").asType()
        val typeUtils = context.processingEnv.typeUtils
        return typeUtils.isSubtype(typeUtils.erasure(mirror), typeUtils.erasure(promiseType))
    }

    private fun validateClassSpec(classSpec: ClassSpec): ClassSpec {
        val newMethodSpecs = ArrayList<MethodSpec>()

        for (methodSpec in classSpec.methods) {
            if (!isValidReturnType(methodSpec.returnType))
                throw IllegalArgumentException(
                    "${classSpec.name}.${methodSpec.name} has an invalid return type; only methods with a return type of nl.komponents.kovenant.Promise are allowed, found ${methodSpec.returnType}")

            val actualReturnType = extractReturnTypeValueType(methodSpec.returnType as DeclaredType)

            //Elements no longer match...
            newMethodSpecs.add(methodSpec.copy(returnType = actualReturnType))
        }

        return classSpec.copy(methods = newMethodSpecs)
    }
}