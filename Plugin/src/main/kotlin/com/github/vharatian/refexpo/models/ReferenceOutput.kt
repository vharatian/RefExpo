package com.github.vharatian.refexpo.models

import com.fasterxml.jackson.annotation.JsonPropertyOrder

@JsonPropertyOrder(
    "sourcePath",
    "sourceLine",
    "sourceClass",
    "sourceClassFull",
    "sourceMethod",
    "sourceMethodFull",
    "sourceStructure",
    "targetPath",
    "targetLine",
    "targetClass",
    "targetClassFull",
    "targetMethod",
    "targetMethodFull",
    "targetStructure"
)
class FlatReferenceOutput {
    val sourcePath: String
    val sourceLine: Int
    val sourceClass: String
    val sourceClassFull: String
    val sourceMethod: String
    val sourceMethodFull: String
    val sourceStructure: String

    val targetPath: String
    val targetLine: Int
    val targetClass: String
    val targetClassFull: String
    val targetMethod: String
    val targetMethodFull: String
    val targetStructure: String

    constructor (source: Locator, target: Locator) {
        this.sourcePath = source.path
        this.sourceLine = source.lineNumber
        this.sourceClass = source.clazz
        this.sourceClassFull = source.classQualifiedName
        this.sourceMethod = source.method
        this.sourceMethodFull = source.methodQualifiedName
        this.sourceStructure = source.structure

        this.targetPath = target.path
        this.targetLine = target.lineNumber
        this.targetClass = target.clazz
        this.targetClassFull = target.classQualifiedName
        this.targetMethod = target.method
        this.targetMethodFull = target.methodQualifiedName
        this.targetStructure = target.structure
    }
}

class Locator(
    val path: String,
    val lineNumber: Int,
    val clazz: String,
    val classQualifiedName: String,
    val method: String,
    val methodQualifiedName: String,
    val structure: String
) {

}

