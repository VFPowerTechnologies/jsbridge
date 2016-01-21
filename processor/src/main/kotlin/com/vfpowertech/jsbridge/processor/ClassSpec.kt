package com.vfpowertech.jsbridge.processor

//TODO handle generics (List<Set<String>>, etc)
data class ClassSpec(
    val name: String,
    val methods: List<MethodSpec>
)