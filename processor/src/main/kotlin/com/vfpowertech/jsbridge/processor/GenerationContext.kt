package com.vfpowertech.jsbridge.processor

import javax.annotation.processing.ProcessingEnvironment

class GenerationContext(
    val processingEnv: ProcessingEnvironment,
    val options: GenerationOptions,
    val templates: Templates
)