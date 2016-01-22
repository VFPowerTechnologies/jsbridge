package com.vfpowertech.jsbridge.processor

import java.io.File

data class GenerationOptions(
    val jsOutputDir: File,
    val jsCallbackPackage: String,
    val jsToJavaProxySubpackageName: String,
    val javaToJsProxySubpackageName: String
) {
    companion object {
        fun fromAPTOptions(options: Map<String, String>): GenerationOptions {
            val p = options["jsOutputDir"] ?: throw GenerationOptionException("Missing jsOutputDir option")

            val pkg = options["jsCallbackPackage"] ?: throw GenerationOptionException("Missing jsCallbackPackage")

            val jsToJavaproxySubpackageName = options["jsToJavaProxySubpackageName"] ?: "jstojava"
            val javaToJSproxySubpackageName = options["javaToJSProxySubpackageName"] ?: "javatojs"

            return GenerationOptions(
                File(p),
                pkg,
                jsToJavaproxySubpackageName,
                javaToJSproxySubpackageName)
        }
    }
}