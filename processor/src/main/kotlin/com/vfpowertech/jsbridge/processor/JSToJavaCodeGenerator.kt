package com.vfpowertech.jsbridge.processor

import org.apache.velocity.VelocityContext
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.util.*
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeMirror
import javax.lang.model.type.WildcardType

data class MethodGenerationInfo(
    val name: String,
    val argsType: String,
    val returnType: String?,
    val argNames: List<String>,
    val hasReturnValue: Boolean,
    val returnsPromise: Boolean, val promiseReturnType: String?
) {
    val hasArgs: Boolean
        get() = argNames.isNotEmpty()
}

class JSToJavaCodeGenerator(private val context: GenerationContext) {
    //list of generated JSCallback* classes
    private val generatedCallbacks = HashSet<String>()

    fun generate(elements: Set<Element>) {
        for (e in elements)
            generateCodeFor(e as TypeElement)
    }

    private fun generateCodeFor(e: TypeElement) {
        val fqn = e.qualifiedName

        val (pkg, className) = splitPackageClass(fqn)
        val generatedPackage = "$pkg.${context.options.jsToJavaProxySubpackageName}"
        val generatedClassName = "$className${context.options.jsToJavaClassSuffix}"
        val generatedFQN = "$generatedPackage.$generatedClassName"
        context.logInfo("Generating $generatedFQN")

        val classSpec = validateClassSpec(generateClassSpecFor(context.processingEnv, e))

        val methodGenerationInfo = ArrayList<MethodGenerationInfo>()
        //generate method arg classes
        for (methodSpec in classSpec.methods) {
            val newSpec = preprocessMethodSpec(classSpec, methodSpec)
            generateCodeForMethodParams(context, generatedPackage, classSpec, newSpec, e)

            val argNames = methodSpec.params.map { it.name }
            val argsType = getMethodArgsClassName(classSpec, newSpec)
            val returnType = newSpec.returnTypeFQN
            val promiseReturnType = if(methodSpec.returnsPromise)
                getPromiseReturnType(methodSpec.returnType as DeclaredType).toString()
            else
                null

            val genInfo = MethodGenerationInfo(
                methodSpec.name,
                argsType,
                returnType,
                argNames,
                methodSpec.hasReturnValue,
                methodSpec.returnsPromise,
                promiseReturnType)
            methodGenerationInfo.add(genInfo)
        }

        //generate js->java proxy
        val vc = VelocityContext()
        vc.put("package", generatedPackage)
        vc.put("className", generatedClassName)
        vc.put("originalFDQN", fqn)
        vc.put("originalClassName", className)
        vc.put("methods", methodGenerationInfo)

        context.writeTemplate(context.templates.jsToJavaProxy, generatedFQN, e, vc)

        generateJSStub(classSpec)
    }

    private fun generateJSStub(classSpec: ClassSpec) {
        val className = classSpec.name
        val jsModuleName = uncamel(className)
        //TODO support packaging based on truncated java pkg maybe?
        val path = File(context.options.jsOutputDir, jsModuleName + ".js")

        val vc = VelocityContext()
        vc.put("className", className)
        vc.put("methods", classSpec.methods)

        context.logInfo("Generating $path")

        BufferedWriter(FileWriter(path)).use {
            context.templates.jsServiceStub.merge(vc, it)
        }
    }

    private fun validateClassSpec(classSpec: ClassSpec): ClassSpec {
        val newMethodSpecs = ArrayList<MethodSpec>()

        for (methodSpec in classSpec.methods) {
            if (methodSpec.element.getAnnotation(Exclude::class.java) != null)
                continue

            newMethodSpecs.add(methodSpec)
        }

        return classSpec.copy(methods = newMethodSpecs)
    }

    private fun jscallbackNameFromParamspec(mirror: DeclaredType): String {
        fun collapseTypeName(typeName: String): String =
            typeName.replace(".", "")

        fun getStr(mirror: TypeMirror, builder: StringBuilder): Unit =
            when (mirror) {
                is DeclaredType -> {
                    val typeName = mirror.asElement().toString()
                    builder.append(collapseTypeName(typeName))
                    for (arg in mirror.typeArguments)
                        getStr(arg, builder)
                }

                is WildcardType -> {
                    val wildMirror = (mirror.superBound ?: mirror.extendsBound)
                    getStr(wildMirror, builder)
                }

                else ->
                    throw IllegalArgumentException("Unexpected type kind: ${mirror.kind}")
            }

        val arg = mirror.typeArguments.first()
        val builder = StringBuilder("JSCallback")
        getStr(arg, builder)
        return builder.toString()
    }

    private fun preprocessMethodSpec(classSpec: ClassSpec, methodSpec: MethodSpec): MethodSpec {
        val methodFQN = "${classSpec.name}.${methodSpec.name}"

        val params = ArrayList<ParamSpec>()

        for (p in methodSpec.params) {
            if (!checkIfFunctionTypeIsSupported(methodFQN, p.type)) {
                params.add(p)
                continue
            }
            val jscallbackName = jscallbackNameFromParamspec(p.type as DeclaredType)
            val fqn = "${context.options.jsCallbackPackage}.$jscallbackName"
            val newParamSpec = p.copy(type = PlaceholderType(fqn))
            params.add(newParamSpec)

            if (fqn in generatedCallbacks)
                continue

            generatedCallbacks.add(fqn)

            context.logInfo("Generating $fqn")

            val sig = getTypeWithoutBounds(p.type)
            val (retType, funcArgs) = getFunctionTypes(p.type)
            val vc = VelocityContext()
            vc.put("package", context.options.jsCallbackPackage)
            vc.put("className", jscallbackName)
            vc.put("functionSig", sig)
            vc.put("retType", retType)
            vc.put("argType", funcArgs.first())

            context.writeTemplate(context.templates.jsCallback, fqn, null, vc)
        }

        return methodSpec.copy(params = params)
    }
}
