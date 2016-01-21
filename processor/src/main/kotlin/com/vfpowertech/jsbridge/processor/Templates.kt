package com.vfpowertech.jsbridge.processor

import org.apache.velocity.Template
import org.apache.velocity.app.VelocityEngine

//Templates for sharing across generators
class Templates(
    val velocityEngine: VelocityEngine,
    val jsproxyTemplate: Template,
    val argsTemplate: Template,
    val jscallbackTemplate: Template
)