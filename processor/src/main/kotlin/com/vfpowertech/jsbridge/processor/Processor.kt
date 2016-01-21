package com.vfpowertech.jsbridge.processor

import org.apache.velocity.VelocityContext
import org.apache.velocity.app.VelocityEngine
import org.apache.velocity.runtime.RuntimeConstants
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader
import java.io.BufferedWriter
import java.util.*
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedAnnotationTypes
import javax.annotation.processing.SupportedOptions
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.PrimitiveType
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror
import javax.lang.model.type.WildcardType
import javax.tools.Diagnostic

data class MethodGenerationInfo(
    val name: String,
    val argsType: String,
    val retType: String?,
    val argNames: List<String>
) {
    val hasRetVal: Boolean
        get() = retType != null
    val hasArgs: Boolean
        get() = argNames.isNotEmpty()
}

data class ParamSpec(val name: String, val typeStr: String)
data class MethodSpec(
    val name: String,
    val retType: String,
    val retMirror: TypeMirror,
    val params: List<ParamSpec>,
    val paramMirrors: List<TypeMirror>
)
data class ClassSpec(
    val name: String,
    val methods: List<MethodSpec>
)

@SupportedAnnotationTypes(
    "com.vfpowertech.jsbridge.processor.Generate",
    "com.vfpowertech.jsbridge.processor.JSGenerate"
)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedOptions("jsBuildDir", "jsCallbackPackage", "jsProxySubpackageName")
class Processor : AbstractProcessor() {
    companion object {
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
    }

    private var initialized = false
    private lateinit var context: GenerationContext
    //list of generated JSCallback* classes
    private val generatedCallbacks = HashSet<String>()

    private fun logInfo(s: String) {
        processingEnv.messager.printMessage(Diagnostic.Kind.NOTE, s)
    }

    private fun logError(s: String) {
        processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, s)
    }

    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        if (annotations.isEmpty())
            return false

        if (!initialized) {
            val options = try {
                GenerationOptions.fromAPTOptions(processingEnv.options)
            }
            catch (e: GenerationOptionException) {
                logError(e.message!!)
                return true
            }

            val props = Properties()
            props.setProperty("runtime.references.strict", "true")
            val velocityEngine = VelocityEngine(props)
            velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath")
            velocityEngine.setProperty("classpath.resource.loader.class", ClasspathResourceLoader::class.java.name)
            velocityEngine.init()

            val templates = Templates(
                velocityEngine,
                velocityEngine.getTemplate("templates/jsproxy.java.vm"),
                velocityEngine.getTemplate("templates/args.java.vm"),
                velocityEngine.getTemplate("templates/jscallback.java.vm"))

            context = GenerationContext(processingEnv, options,  templates)
        }

        for (e in roundEnv.getElementsAnnotatedWith(Generate::class.java)) {
            generateCodeFor(e as TypeElement)
        }

        val toJSCodeGenerator = ToJSCodeGenerator(processingEnv)
        toJSCodeGenerator.generate(roundEnv.getElementsAnnotatedWith(JSGenerate::class.java))

        return true
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
        logInfo("Generating $generatedFQN")

        //TODO classify methods as:
        //a) async (retval is Promise)
        //b) sync (retval is not Promise)
        //also need to handle FunctionN params since they need to be wrapped
        val classSpec = generateClassSpecFor(e)

        val methodGenerationInfo = ArrayList<MethodGenerationInfo>()
        //generate method arg classes
        for (methodSpec in classSpec.methods) {
            val newSpec = preprocessMethodSpec(generatedPkg, classSpec, methodSpec)
            generateCodeForMethodParams(generatedPkg, classSpec, newSpec, e)

            val argNames = methodSpec.params.map { it.name }
            val argsType = getMethodArgsClassName(classSpec, newSpec)
            val retType = if (!isVoidType(processingEnv, newSpec.retMirror)) newSpec.retType else null

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

        val jfo = processingEnv.filer.createSourceFile(generatedFQN, e)

        BufferedWriter(jfo.openWriter()).use {
            context.templates.jsproxyTemplate.merge(vc, it)
        }
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

    private fun preprocessMethodSpec(pkg: String, classSpec: ClassSpec, methodSpec: MethodSpec): MethodSpec {
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

            logInfo("Generating $fqn")

            val sig = getTypeWithoutBounds(mirror)
            val (retType, funcArgs) = getFunctionTypes(mirror)
            val vc = VelocityContext()
            vc.put("package", context.options.jsCallbackPackage)
            vc.put("className", jscallbackName)
            vc.put("functionSig", sig)
            vc.put("retType", retType)
            vc.put("argType", funcArgs.first())

            val jfo = processingEnv.filer.createSourceFile(fqn)
            BufferedWriter(jfo.openWriter()).use {
                context.templates.jscallbackTemplate.merge(vc, it)
            }
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

        logInfo("Generating $fqn")

        val jfo = processingEnv.filer.createSourceFile(fqn, e)
        BufferedWriter(jfo.openWriter()).use {
            context.templates.argsTemplate.merge(vc, it)
        }
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

