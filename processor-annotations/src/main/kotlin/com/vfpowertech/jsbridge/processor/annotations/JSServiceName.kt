package com.vfpowertech.jsbridge.processor.annotations

/**
 * Specifies the window-object relative path to the service.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class JSServiceName(val value: String)