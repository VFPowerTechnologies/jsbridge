package com.vfpowertech.jsbridge.processor

import com.vfpowertech.jsbridge.processor.annotations.JSServiceName
import org.apache.velocity.VelocityContext
import java.util.*
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType

/*
1) find methods, their params and return types
2) Generate: *Args class
    design decision:
        no serializers are generated for your own types; if they're POJOs jackson can serialize them without any intervention
        otherwise, annotate them or just mixins as appropriate
    rationale:
        makes generation vastly simpler, as well as letting the library user make their own decisions about serialization when required
*/
class JavaToJSCodeGenerator(private val context: GenerationContext) {
    fun generate(elements: Set<Element>) {
        val classes = ArrayList<ClassSpec>()
        for (e in elements) {
            val classInfo = validateClassSpec(generateClassSpecFor(context.processingEnv, e as TypeElement, context.options.jsVerbose))
            classes.add(classInfo)
        }

        for (classSpec in classes) {
            val serviceName = classSpec.element.getAnnotation(JSServiceName::class.java)?.value
            if (serviceName == null) {
                context.logError("${classSpec.fqn} lacks a JSServiceName annotation")
                return
            }

            val fqn = classSpec.fqn
            val (pkg, className) = splitPackageClass(fqn)
            val generatedPackage = "$pkg.${context.options.javaToJsProxySubpackageName}"

            for (methodSpec in classSpec.methods) {
                generateCodeForMethodParams(context, generatedPackage, classSpec, methodSpec, classSpec.asTypeElement())
            }

            val generatedClassName = "$className${context.options.javaToJSClassSuffix}"
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

    private fun validateClassSpec(classSpec: ClassSpec): ClassSpec {
        val newMethodSpecs = ArrayList<MethodSpec>()

        for (methodSpec in classSpec.methods) {
            if (!isValidPromiseType(context.processingEnv, methodSpec.returnType))
                throw IllegalArgumentException(
                    "${classSpec.name}.${methodSpec.name} has an invalid return type; only methods with a return type of nl.komponents.kovenant.Promise are allowed, found ${methodSpec.returnType}")

            val actualReturnType = getPromiseReturnType(methodSpec.returnType as DeclaredType)

            newMethodSpecs.add(methodSpec.copy(returnType = actualReturnType))
        }

        return classSpec.copy(methods = newMethodSpecs)
    }
}