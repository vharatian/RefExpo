package com.github.vharatian.refexpo.services

import com.intellij.psi.JavaRecursiveElementVisitor
import com.intellij.psi.PsiElement

class ErrorResilientVisitor(val processElement: (PsiElement) -> Unit) : JavaRecursiveElementVisitor() {

    override fun visitElement(element: PsiElement) {
        try {
            processElement(element)

            super.visitElement(element)
        } catch (e: Exception) {
//                log("Error: ${e.message}")
//                e.printStackTrace()
        }
    }
}