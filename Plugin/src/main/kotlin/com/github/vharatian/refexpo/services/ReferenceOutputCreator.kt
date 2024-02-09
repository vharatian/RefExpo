package com.github.vharatian.refexpo.services

import com.github.vharatian.refexpo.models.FlatReferenceOutput
import com.github.vharatian.refexpo.models.Locator
import com.intellij.lang.javascript.psi.ecmal4.JSQualifiedNamedElement
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.PsiQualifiedNamedElement
import com.jetbrains.python.psi.PyQualifiedNameOwner


class ReferenceOutputCreator(val project: Project) {

    private val documentManage = PsiDocumentManager.getInstance(project)

    fun createOutput(source: PsiElement, target: PsiElement): FlatReferenceOutput {

        val sourceLocator = extractLocators(source)
        val targetLocator = extractLocators(target)

        return FlatReferenceOutput(sourceLocator, targetLocator)
    }

    private fun extractLocators(element: PsiElement): Locator {
        val path = getRelativePath(element.containingFile?.virtualFile)
        val classElement = findEnclosingElement(element, LocatorType.CLASS)
        val clazz = getElementName(classElement)
        val classQualifiedName = getQualifiedElementName(classElement)
        val methodElement = findEnclosingElement(element, LocatorType.METHOD)
        val method = getElementName(methodElement)
        val methodQualifiedName = getQualifiedElementName(methodElement)
        val structure = getStructure(methodElement)
        val lineNumber = getLineNumber(element)

        return Locator(path, lineNumber, clazz, classQualifiedName, method, methodQualifiedName, structure)
    }

    fun getLineNumber(element: PsiElement): Int {
        val containingFile = element.containingFile
            ?: return -1 // Element is not in a file
        val document = documentManage.getDocument(containingFile)
            ?: return -1 // No document associated with the file
        val offset = element.textOffset
        return document.getLineNumber(offset) + 1 // Adding 1 to make the line number 1-based
    }

    private fun getRelativePath(file: VirtualFile?) = file?.path?.replace("${project.basePath}/", "") ?: ""

    private inline fun findEnclosingElement(element: PsiElement?, locatorType: LocatorType): PsiElement? {
        var currentElement = element
        while (currentElement != null && !locatorType.matches(currentElement)) {
            currentElement = currentElement.parent
        }

        return currentElement
    }

    private fun getElementName(element: PsiElement?): String {
        if (element == null)
            return ""

        if (element is PsiNamedElement)
            return element.name ?: ""

        return ""
    }

    private fun getQualifiedElementName(element: PsiElement?): String {
        if (element == null)
            return ""

        if (element is PsiQualifiedNamedElement)
            return element.qualifiedName ?: ""

        if (element is PyQualifiedNameOwner)
            return element.qualifiedName ?: ""

        if (element is JSQualifiedNamedElement)
            return element.qualifiedName ?: ""

        return ""
    }

    private fun getStructure(element: PsiElement?): String {
        var parent = element
        var result = ""
        while (parent != null && parent.isValid) {

            if (LocatorType.CLASS.matches(parent) || LocatorType.METHOD.matches(parent)) {
                val elementName = getElementName(parent)
                if (elementName.isNotBlank()) {
                    if (result.isNotEmpty()) {
                        result = ".$result"
                    }

                    result = "$elementName$result"
                }

            }

            parent = parent.parent
        }

        return result
    }
}