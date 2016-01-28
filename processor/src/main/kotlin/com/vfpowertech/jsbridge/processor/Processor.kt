package com.vfpowertech.jsbridge.processor

import com.vfpowertech.jsbridge.processor.annotations.Generate
import com.vfpowertech.jsbridge.processor.annotations.JSGenerate
import org.apache.velocity.app.VelocityEngine
import org.apache.velocity.runtime.RuntimeConstants
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader
import java.util.*
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedAnnotationTypes
import javax.annotation.processing.SupportedOptions
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic

@SupportedAnnotationTypes(
    "com.vfpowertech.jsbridge.processor.annotations.Generate",
    "com.vfpowertech.jsbridge.processor.annotations.JSGenerate"
)
@SupportedSourceVersion(SourceVersion.RELEASE_7)
@SupportedOptions(
    "jsOutputDir",
    "jsCallbackPackage",
    "javaToJSProxySubpackageName",
    "javaToJSClassSuffix",
    "jsToJavaProxySubpackageName",
    "jsToJavaClassSuffix"
)
class Processor : AbstractProcessor() {
    private var initialized = false
    private lateinit var context: GenerationContext

    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        if (annotations.isEmpty())
            return false

        if (!initialized) {
            val options = try {
                GenerationOptions.fromAPTOptions(processingEnv.options)
            }
            catch (e: GenerationOptionException) {
                processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, e.message!!)
                return true
            }

            //returns false if dirs already exist
            if (!options.jsOutputDir.mkdirs()) {
                if (!options.jsOutputDir.isDirectory || !options.jsOutputDir.exists()) {
                    context.logError("Unable to create jsBuildDir at ${options.jsOutputDir}")
                    return true
                }
            }

            val props = Properties()
            props.setProperty("runtime.references.strict", "true")
            val velocityEngine = VelocityEngine(props)
            velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath")
            velocityEngine.setProperty("classpath.resource.loader.class", ClasspathResourceLoader::class.java.name)
            velocityEngine.init()

            val templates = Templates(
                velocityEngine,
                velocityEngine.getTemplate("templates/JSToJavaProxy.java.vm"),
                velocityEngine.getTemplate("templates/MethodArgs.java.vm"),
                velocityEngine.getTemplate("templates/JSCallback.java.vm"),
                velocityEngine.getTemplate("templates/JavaToJSProxy.java.vm"),
                velocityEngine.getTemplate("templates/js-service-stub.js.vm"))

            context = GenerationContext(processingEnv, options, templates)
        }

        val jsToJavaCodeGenerator = JSToJavaCodeGenerator(context)
        jsToJavaCodeGenerator.generate(roundEnv.getElementsAnnotatedWith(Generate::class.java))

        val javaToJSCodeGenerator = JavaToJSCodeGenerator(context)
        javaToJSCodeGenerator.generate(roundEnv.getElementsAnnotatedWith(JSGenerate::class.java))

        return true
    }
}
