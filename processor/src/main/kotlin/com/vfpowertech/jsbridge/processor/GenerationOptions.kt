package com.vfpowertech.jsbridge.processor

import java.io.File

data class GenerationOptions(
    val jsOutputDir: File,
    val jsCallbackPackage: String,
    val jsToJavaProxySubpackageName: String,
    val javaToJsProxySubpackageName: String,
    val jsToJavaClassSuffix: String,
    val javaToJSClassSuffix: String,
    val jsAddModuleExports: Boolean,
    val jsVerbose: Boolean
) {
    companion object {
        //String.toBoolean returns false for anything that isn't true, which isn't nice since it won't throw on typos
        private fun stringToBoolean(options: Map<String, String>, optionName: String, default: Boolean): Boolean {
            val s = options[optionName] ?: return default

            val v = s.toLowerCase()
            if (v != "false" && v != "true")
                throw GenerationOptionException("Expected true or false for $optionName but got $v")

            return v.toBoolean()
        }

        fun fromAPTOptions(options: Map<String, String>): GenerationOptions {
            val p = options["jsOutputDir"] ?: throw GenerationOptionException("Missing jsOutputDir option")

            val pkg = options["jsCallbackPackage"] ?: throw GenerationOptionException("Missing jsCallbackPackage")

            val jsToJavaProxySubpackageName = options["jsToJavaProxySubpackageName"] ?: "jstojava"
            val javaToJSProxySubpackageName = options["javaToJSProxySubpackageName"] ?: "javatojs"

            val jsToJavaClassSuffix = options["jsToJavaClassSuffix"] ?: "ToJavaProxy"
            val javaToJSClassSuffix = options["javaToJSClassSuffix"] ?: "ToJSProxy"

            val jsAddModuleExports = stringToBoolean(options, "jsAddModuleExports", true)

            val jsVerbose = stringToBoolean(options, "jsVerbose", false)

            return GenerationOptions(
                File(p),
                pkg,
                jsToJavaProxySubpackageName,
                javaToJSProxySubpackageName,
                jsToJavaClassSuffix,
                javaToJSClassSuffix,
                jsAddModuleExports,
                jsVerbose)
        }
    }
}