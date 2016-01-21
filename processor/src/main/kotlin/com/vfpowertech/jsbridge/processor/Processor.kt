package com.vfpowertech.jsbridge.processor

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

@SupportedAnnotationTypes(
    "com.vfpowertech.jsbridge.processor.Generate",
    "com.vfpowertech.jsbridge.processor.JSGenerate"
)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedOptions("jsBuildDir", "jsCallbackPackage", "jsProxySubpackageName")
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
                context.logError(e.message!!)
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

            context = GenerationContext(processingEnv, options, templates)
        }

        val jsToJavaCodeGenerator = JSToJavaCodeGenerator(context)
        jsToJavaCodeGenerator.generate(roundEnv.getElementsAnnotatedWith(Generate::class.java))

        val javaToJSCodeGenerator = JavaToJSCodeGenerator(context)
        javaToJSCodeGenerator.generate(roundEnv.getElementsAnnotatedWith(JSGenerate::class.java))

        return true
    }
}
