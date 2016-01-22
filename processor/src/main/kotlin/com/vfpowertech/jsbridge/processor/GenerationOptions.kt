package com.vfpowertech.jsbridge.processor

import java.io.File

data class GenerationOptions(
    val jsOutputDir: File,
    val jsCallbackPackage: String,
    val jsToJavaProxySubpackageName: String,
    val javaToJsProxySubpackageName: String,
    val jsToJavaClassSuffix: String,
    val javaToJSClassSuffix: String
) {
    companion object {
        fun fromAPTOptions(options: Map<String, String>): GenerationOptions {
            val p = options["jsOutputDir"] ?: throw GenerationOptionException("Missing jsOutputDir option")

            val pkg = options["jsCallbackPackage"] ?: throw GenerationOptionException("Missing jsCallbackPackage")

            val jsToJavaProxySubpackageName = options["jsToJavaProxySubpackageName"] ?: "jstojava"
            val javaToJSProxySubpackageName = options["javaToJSProxySubpackageName"] ?: "javatojs"

            val jsToJavaClassSuffix = options["jsToJavaClassSuffix"] ?: "ToJavaProxy"
            val javaToJSClassSuffix = options["javaToJSClassSuffix"] ?: "ToJSProxy"

            return GenerationOptions(
                File(p),
                pkg,
                jsToJavaProxySubpackageName,
                javaToJSProxySubpackageName,
                jsToJavaClassSuffix,
                javaToJSClassSuffix)
        }
    }
}