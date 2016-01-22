package com.vfpowertech.jsbridge.processor

import org.apache.velocity.Template
import org.apache.velocity.app.VelocityEngine

//Templates for sharing across generators
class Templates(
    val velocityEngine: VelocityEngine,
    val jsToJavaProxy: Template,
    val args: Template,
    val jsCallback: Template,
    val javaToJSProxy: Template
)