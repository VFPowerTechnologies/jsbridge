@file:JvmName("Utils")
package com.vfpowertech.jsbridge.core.dispatcher

import com.fasterxml.jackson.databind.ObjectMapper
import java.io.PrintWriter
import java.io.StringWriter

private data class ExceptionJSONRepr(
    val message: String,
    val type: String,
    val stacktrace: String
)

fun stacktraceAsString(e: Exception): String {
    val sw = StringWriter()
    val pw = PrintWriter(sw)
    e.printStackTrace(pw)
    return sw.toString()
}

fun exceptionToJSONString(e: Exception): String {
    val repr = ExceptionJSONRepr(e.message ?: "<no message>", e.javaClass.canonicalName, stacktraceAsString(e))
    return ObjectMapper().writeValueAsString(repr)
}