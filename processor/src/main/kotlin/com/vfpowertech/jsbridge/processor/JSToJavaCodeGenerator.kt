package com.vfpowertech.jsbridge.processor

import org.apache.velocity.VelocityContext
import java.util.*
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeMirror
import javax.lang.model.type.WildcardType

data class MethodGenerationInfo(
    val name: String,
    val argsType: String,
    val returnType: String?,
    val argNames: List<String>
) {
    val hasRetVal: Boolean
        get() = returnType != null
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
        val idx = fqn.lastIndexOf('.')
        if (idx <= 0)
            throw IllegalArgumentException("Annotated objects must be in a package: $fqn")

        //generated files go into <qualified-name>.js.<name>JSProxy
        val pkg = fqn.substring(0, idx)
        val generatedPkg = "$pkg.${context.options.jsProxySubpackageName}"
        val className = fqn.substring(idx+1)
        val generatedClassName = "${className}JSProxy"
        val generatedFQN = "$generatedPkg.$generatedClassName"
        context.logInfo("Generating $generatedFQN")

        //TODO classify methods as:
        //a) async (retval is Promise)
        //b) sync (retval is not Promise)
        //also need to handle FunctionN params since they need to be wrapped
        val classSpec = generateClassSpecFor(e)

        val methodGenerationInfo = ArrayList<MethodGenerationInfo>()
        //generate method arg classes
        for (methodSpec in classSpec.methods) {
            val newSpec = preprocessMethodSpec(classSpec, methodSpec)
            generateCodeForMethodParams(context, generatedPkg, classSpec, newSpec, e)

            val argNames = methodSpec.params.map { it.name }
            val argsType = getMethodArgsClassName(classSpec, newSpec)
            val returnType = if (!isVoidType(context.processingEnv, newSpec.returnType)) newSpec.returnTypeFQN else null

            val genInfo = MethodGenerationInfo(methodSpec.name, argsType,  returnType,  argNames)
            methodGenerationInfo.add(genInfo)
        }

        //generate js->java proxy
        val vc = VelocityContext()
        vc.put("package", generatedPkg)
        vc.put("className", generatedClassName)
        vc.put("originalFDQN", fqn)
        vc.put("originalClassName", className)
        vc.put("methods", methodGenerationInfo)

        context.writeTemplate(context.templates.jsProxyTemplate, generatedFQN, e, vc)
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

            context.writeTemplate(context.templates.jsCallbackTemplate, fqn, null, vc)
        }

        return methodSpec.copy(params = params)
    }

    private fun generateClassSpecFor(cls: TypeElement): ClassSpec {
        val methods = ArrayList<MethodSpec>()

        for (ee in cls.enclosedElements) {
            if (ee.kind != ElementKind.METHOD)
                continue

            if (ee.getAnnotation(Exclude::class.java) != null)
                continue

            val m = ee as ExecutableElement
            val methodName = m.simpleName.toString()
            val returnType = m.returnType
            val params = ArrayList<ParamSpec>()

            for (p in m.parameters) {
                val paramName = p.simpleName.toString()
                val type = p.asType()
                params.add(ParamSpec(paramName, type))
            }

            methods.add(MethodSpec(methodName, params, returnType))
        }

        return ClassSpec(cls.simpleName.toString(), methods)
    }
}
