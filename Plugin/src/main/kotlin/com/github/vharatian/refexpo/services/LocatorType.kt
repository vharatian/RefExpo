package com.github.vharatian.refexpo.services

import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType

enum class LocatorType {
    METHOD,
    CLASS,
}

val matchingNames = mapOf(
    LocatorType.METHOD to listOf("method", "function"),
    LocatorType.CLASS to listOf("class"),
)

fun LocatorType.matches(element: PsiElement): Boolean {
    if (element.elementType == null) return false

    val elementTypeName = element.elementType
        .toString()
        .lowercase()

    matchingNames[this]?.forEach {
        if (elementTypeName.contains(it)) return true
    }

    return false
}