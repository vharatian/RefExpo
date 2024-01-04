package com.github.vharatian.refexpo.models

class RefExpoExecutionConfig(
    val filePath: String,
    val ignoringFilesRegex: String,
    val ignoringClassesRegex: String,
    val ignoringMethodsRegex: String,
    val ignoreInterFile: Boolean,
    val ignoreInterClass: Boolean,
    val ignoreInterMethod: Boolean,
    val addPackageName: Boolean
)