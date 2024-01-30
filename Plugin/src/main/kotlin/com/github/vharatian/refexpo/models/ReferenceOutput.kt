package com.github.vharatian.refexpo.models

import com.fasterxml.jackson.annotation.JsonPropertyOrder

@JsonPropertyOrder(
    "sourcePath",
    "sourceLine",
    "sourceClass",
    "sourceMethod",
    "sourceStructure",
    "targetPath",
    "targetLine",
    "targetClass",
    "targetMethod",
    "targetStructure")
class FlatReferenceOutput {
    val sourcePath: String
    val sourceLine: Int
    val sourceClass: String
    val sourceMethod: String
    val sourceStructure: String

    val targetPath: String
    val targetLine: Int
    val targetClass: String
    val targetMethod: String
    val targetStructure: String

    constructor (source: Locator, target: Locator) {
        this.sourcePath = source.path
        this.sourceLine = source.lineNumber
        this.sourceClass = source.clazz
        this.sourceMethod = source.method
        this.sourceStructure = source.structure

        this.targetPath = target.path
        this.targetLine = target.lineNumber
        this.targetClass = target.clazz
        this.targetMethod = target.method
        this.targetStructure = target.structure
    }
}

class Locator(val path: String, val lineNumber: Int, val clazz: String, val method: String, val structure: String) {

}

