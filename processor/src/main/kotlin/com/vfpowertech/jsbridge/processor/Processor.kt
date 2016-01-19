package com.vfpowertech.jsbridge.processor

import org.apache.velocity.Template
import org.apache.velocity.VelocityContext
import org.apache.velocity.app.VelocityEngine
import org.apache.velocity.runtime.RuntimeConstants
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader
import java.io.BufferedWriter
import java.io.File
import java.util.*
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedAnnotationTypes
import javax.annotation.processing.SupportedOptions
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic

data class ParamSpec(val name: String, val type: String)
data class MethodSpec(
    val name: String,
    val params: List<ParamSpec>
)
data class ClassSpec(
    val name: String,
    val methods: List<MethodSpec>
)

@SupportedAnnotationTypes(
    "com.vfpowertech.jsbridge.processor.Generate"
)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedOptions("jsBuildDir")
class Processor : AbstractProcessor() {
    private var initialized = false
    private lateinit var jsBuildDir: File
    private lateinit var velocityEngine: VelocityEngine
    private lateinit var jsproxyTemplate: Template
    private lateinit var argsTemplate: Template
    private val generatedClasses = HashSet<String>()

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
            val p = processingEnv.options["jsBuildDir"]
            if (p == null) {
                logError("Missing jsBuildDir option")
                return true
            }
            jsBuildDir = File(p)

            velocityEngine = VelocityEngine()
            velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath")
            velocityEngine.setProperty("classpath.resource.loader.class", ClasspathResourceLoader::class.java.name)
            velocityEngine.init()

            jsproxyTemplate = velocityEngine.getTemplate("templates/jsproxy.java.vm")
            argsTemplate = velocityEngine.getTemplate("templates/args.java.vm")
        }

        for (e in roundEnv.getElementsAnnotatedWith(Generate::class.java)) {
            generateCodeFor(e as TypeElement)
        }

        return true
    }

    private fun generateCodeFor(e: TypeElement) {
        val fqdn = e.qualifiedName
        val idx = fqdn.lastIndexOf('.')
        if (idx <= 0)
            throw IllegalArgumentException("Annotated objects must be in a package: $fqdn")

        //generated files go into <qualified-name>.js.<name>JSProxy
        val pkg = fqdn.substring(0, idx-1)
        val generatedPkg = "$pkg.js"
        val className = fqdn.substring(idx+1)
        val generatedClassName = "${className}JSProxy"
        val generatedFQDN = "$generatedPkg.$generatedClassName"
        logInfo("Generating $generatedFQDN")

        //TODO classify methods as:
        //a) async (retval is Promise)
        //b) sync (retval is not Promise)
        //also need to handle FunctionN params since they need to be wrapped
        val classSpec = generateClassSpecFor(e)

        //generate method arg classes
        for (methodSpec in classSpec.methods)
            generateCodeForMethodParams(generatedPkg, methodSpec, e)

        //generate js->java proxy
        val vc = VelocityContext()
        vc.put("package", generatedPkg)
        vc.put("className", generatedClassName)
        vc.put("originalFDQN", fqdn)
        vc.put("originalClassName", className)

        val jfo = processingEnv.filer.createSourceFile(generatedFQDN, e)

        BufferedWriter(jfo.openWriter()).use {
            jsproxyTemplate.merge(vc, it)
        }
    }

    private fun generateCodeForMethodParams(pkg: String, methodSpec: MethodSpec, e: TypeElement) {
        //don't generate empty params
        if (methodSpec.params.isEmpty())
            return

        val className = "${methodSpec.name}Args"
        val fqdn = "$pkg.$className"

        val vc = VelocityContext()
        vc.put("package", pkg)
        vc.put("className", className)
        vc.put("params", methodSpec.params)

        logInfo("Generating $fqdn")

        //TODO translate FunctionN -> JSCallbackInt
        val jfo = processingEnv.filer.createSourceFile(fqdn, e)
        BufferedWriter(jfo.openWriter()).use {
            argsTemplate.merge(vc, it)
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
            val params = ArrayList<ParamSpec>()

            for (p in m.parameters) {
                val paramName = p.simpleName.toString()
                val paramType = p.asType().toString()
                params.add(ParamSpec(paramName, paramType))
            }

            methods.add(MethodSpec(methodName, params))
        }

        return ClassSpec(cls.simpleName.toString(), methods)
    }
}