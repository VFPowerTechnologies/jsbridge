package com.vfpowertech.jsbridge.processor.annotations

/** Takes an optional JavaScript class name to use. Uses the java class name as the default. */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class JSToJavaGenerate(val value: String = "")
