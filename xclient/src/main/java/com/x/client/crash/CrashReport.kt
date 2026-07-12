package com.x.client.crash

data class CrashReport(
    val title: String,
    val reason: String,
    val solution: String,
    val sourceFile: String?,
    val rawExcerpt: String
)
