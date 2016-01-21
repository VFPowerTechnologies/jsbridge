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
            generateCodeForMethodParams(generatedPkg, classSpec, newSpec, e)

            val argNames = methodSpec.params.map { it.name }
            val argsType = getMethodArgsClassName(classSpec, newSpec)
            val retType = if (!isVoidType(context.processingEnv, newSpec.retMirror)) newSpec.retType else null

            val genInfo = MethodGenerationInfo(methodSpec.name, argsType,  retType,  argNames)
            methodGenerationInfo.add(genInfo)
        }

        //generate js->java proxy
        val vc = VelocityContext()
        vc.put("package", generatedPkg)
        vc.put("className", generatedClassName)
        vc.put("originalFDQN", fqn)
        vc.put("originalClassName", className)
        vc.put("methods", methodGenerationInfo)

        context.writeTemplate(context.templates.jsproxyTemplate, generatedFQN, e, vc)
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

        for ((idx, p) in methodSpec.params.withIndex()) {
            val mirror = methodSpec.paramMirrors[idx]
            if (!checkIfFunctionTypeIsSupported(methodFQN, mirror)) {
                params.add(p)
                continue
            }
            val jscallbackName = jscallbackNameFromParamspec(mirror as DeclaredType)
            val fqn = "${context.options.jsCallbackPackage}.$jscallbackName"
            val newParamSpec = p.copy(typeStr = fqn)
            params.add(newParamSpec)

            if (fqn in generatedCallbacks)
                continue

            generatedCallbacks.add(fqn)

            context.logInfo("Generating $fqn")

            val sig = getTypeWithoutBounds(mirror)
            val (retType, funcArgs) = getFunctionTypes(mirror)
            val vc = VelocityContext()
            vc.put("package", context.options.jsCallbackPackage)
            vc.put("className", jscallbackName)
            vc.put("functionSig", sig)
            vc.put("retType", retType)
            vc.put("argType", funcArgs.first())

            context.writeTemplate(context.templates.jscallbackTemplate, fqn, null, vc)
        }

        return methodSpec.copy(params = params)
    }

    private fun generateCodeForMethodParams(pkg: String, classSpec: ClassSpec, methodSpec: MethodSpec, e: TypeElement) {
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

        context.writeTemplate(context.templates.argsTemplate, fqn, e, vc)
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
            val retType = m.returnType
            val params = ArrayList<ParamSpec>()
            val mirrors = ArrayList<TypeMirror>()

            for (p in m.parameters) {
                val paramName = p.simpleName.toString()
                val mirror = p.asType()
                val paramTypeStr = mirror.toString()
                params.add(ParamSpec(paramName, paramTypeStr))
                mirrors.add(mirror)
            }

            methods.add(MethodSpec(methodName, retType.toString(), retType, params, mirrors))
        }

        return ClassSpec(cls.simpleName.toString(), methods)
    }
}
