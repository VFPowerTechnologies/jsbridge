package com.vfpowertech.jsbridge.processor

import org.apache.velocity.Template
import org.apache.velocity.VelocityContext
import java.io.BufferedWriter
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.tools.Diagnostic

/**
 * Represents a generation context. Contains utility methods over its wrapped @{link javax.annotation.processing.ProcessingEnviroment}
 */
class GenerationContext(
    val processingEnv: ProcessingEnvironment,
    val options: GenerationOptions,
    val templates: Templates
) {
    fun logInfo(s: String) {
        if (!options.jsVerbose)
            return
        processingEnv.messager.printMessage(Diagnostic.Kind.NOTE, s)
    }

    fun logError(s: String) {
        processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, s)
    }

    fun writeTemplate(template: Template, fqn: String, e: Element?, vc: VelocityContext) {
        val jfo = processingEnv.filer.createSourceFile(fqn, e)
        BufferedWriter(jfo.openWriter()).use {
            template.merge(vc, it)
        }
    }
}