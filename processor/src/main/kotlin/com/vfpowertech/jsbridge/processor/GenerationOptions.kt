package com.vfpowertech.jsbridge.processor

import java.io.File

data class GenerationOptions(
    val jsBuildDir: File,
    val jsCallbackPackage: String,
    val jsProxySubpackageName: String
) {
    companion object {
        fun fromAPTOptions(options: Map<String, String>): GenerationOptions {
            val p = options["jsBuildDir"] ?: throw GenerationOptionException("Missing jsBuildDir option")

            val pkg = options["jsCallbackPackage"] ?: throw GenerationOptionException("Missing jsCallbackPackage")

            val proxySubpackage = options["jsProxySubpackageName"] ?: "jsproxy"

            return GenerationOptions(
                File(p),
                pkg,
                proxySubpackage)
        }
    }
}