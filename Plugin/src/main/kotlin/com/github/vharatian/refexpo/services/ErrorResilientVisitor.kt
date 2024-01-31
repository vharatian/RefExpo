package com.github.vharatian.refexpo.services

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementVisitor

class ErrorResilientVisitor(val processElement: (PsiElement) -> Unit) : PsiRecursiveElementVisitor() {

    override fun visitElement(element: PsiElement) {
        try {
            processElement(element)

            super.visitElement(element)
        } catch (e: Exception) {
                println("Error: ${e.message}")
                e.printStackTrace()
        }
    }
}