package com.github.vharatian.refexpo.services

import com.github.vharatian.refexpo.models.RefExpoExecutionConfig
import com.github.vharatian.refexpo.utils.isNullOrFalse
import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandlerBase
import com.intellij.lang.javascript.psi.ecmal4.JSQualifiedNamedElement
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.impl.text.PsiAwareTextEditorImpl
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.jetbrains.python.psi.PyPsiFacade
import com.jetbrains.python.psi.PyQualifiedNameOwner
import com.jetbrains.python.psi.PyReferenceExpression
import com.jetbrains.python.psi.PyReferenceOwner
import com.jetbrains.python.psi.resolve.PyResolveContext
import com.jetbrains.python.psi.resolve.PyResolveUtil
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.rd.util.qualifiedName
import java.io.BufferedWriter
import java.io.File


class RefExpoEvaluation(private val project: Project, private val config: RefExpoExecutionConfig) {

    private val psiManager = PsiManager.getInstance(project)
    private val fileIndex = ProjectFileIndex.getInstance(project)
    private val vcsManager = ProjectLevelVcsManager.getInstance(project)
    private val documentManage = PsiDocumentManager.getInstance(project)

    private val ignoredFilesRegex = config.ignoringFilesRegex.createRegex()
    private val ignoredClassesRegex = config.ignoringClassesRegex.createRegex()
    private val ignoredMethodsRegex = config.ignoringMethodsRegex.createRegex()
    private val scope = GlobalSearchScope.projectScope(project)
    private var problemCount = 0

    private val extensionPointName = ExtensionPointName.create<Any>("com.intellij.gotoDeclarationHandler")
    private val declarationhandler = extensionPointName.extensionList.filter { it is GotoDeclarationHandler }.toList()

    private fun String.createRegex() = if (isNotEmpty()) {
//            var regex = this
//
//            if (!regex.startsWith(".")) regex = ".$regex"
//            if (!regex.endsWith("$")) regex = "$regex$"

        Regex(this)
    } else null


    fun runInspections(progressIndicator: ProgressIndicator) {
        progressIndicator.text = "Finding project files"
        val projectFiles = getAllFiles(progressIndicator)

        progressIndicator.text = "Loading all references"
        loadProjectFiles(projectFiles, progressIndicator)

        progressIndicator.text = "Exporting dependencies"
        exportDependencies(projectFiles, progressIndicator)
    }

    fun getAllFiles(progressIndicator: ProgressIndicator): List<PsiFile> {
        val files = mutableListOf<PsiFile>()

        fileIndex.iterateContent {
            if (it.isValidForInspection()) {
                val file = psiManager.findFile(it)

                file?.let {
                    val filepath = file.virtualFile.getRelativePath()
                    progressIndicator.text2 = "${files.size} -> $filepath"

                    if (ignoredFilesRegex?.matches(filepath).isNullOrFalse()) {
                        files.add(file)
                    }
                }
            }
            true
        }

        return files
    }


    fun loadProjectFiles(projectFiles: List<PsiFile>, progressIndicator: ProgressIndicator) {
        val visitor = ErrorResilientVisitor { element ->
            element.references.toList()
        }

        runOverAllFiles(projectFiles, visitor, progressIndicator)
    }


    fun exportDependencies(
        projectFiles: List<PsiFile>, progressIndicator: ProgressIndicator
    ) {
        val csvFile = createOutputFile(config.filePath)

        csvFile.bufferedWriter().use { writer ->
            writer.write("SourceFile,SourceClass,SourceMethod,SourceStructure,SourceLine,TargetFile,TargetClass,TargetMethod,TargetStructure,TargetLine\n")

            val visitor = ErrorResilientVisitor { element ->
                processElement(element, writer)
            }

            runOverAllFiles(projectFiles, visitor, progressIndicator)

            println("************************** Unresolved references: $problemCount ******************************")

            writer.flush()
            writer.close()
        }
    }

    private fun runOverAllFiles(
        projectFiles: List<PsiFile>, visitor: PsiRecursiveElementVisitor, progressIndicator: ProgressIndicator
    ) {

        projectFiles.forEachIndexed { index, psiFile ->
            if (progressIndicator.isCanceled) return@runOverAllFiles

            //Update progress indicator
            progressIndicator.fraction = (index + 1) / projectFiles.size.toDouble()
            progressIndicator.text2 = "(${index + 1} / ${projectFiles.size}) -> ${psiFile.name}"

            psiFile.accept(visitor)
        }
    }

    private fun createOutputFile(filePath: String): File {
        if (filePath.isEmpty()) {
            throw Exception("Please enter a valid file path")
        }

        val csvFile = if (filePath.startsWith("/")) File(filePath)
        else File(project.basePath, filePath)

        // Delete the existing file
        if (csvFile.isFile) {
            csvFile.delete()
        }
        return csvFile
    }

    private fun processElement(psiElement: PsiElement, writer: BufferedWriter) {
        // Check all references of the psiElement


        var references = psiElement.references.toList()
//        references += ReferencesSearch.search(psiElement, scope)

        for (ref in references) {

            if (psiElement.text == "(x+y).component") {
//                (psiElement as Navigatable).navigate(true)
                val a = 5

            }

            //Check if the destination of the reference is valid
            var target = ref.resolve()
            if (target == null) {
                target = tryDeclarationHandlers(psiElement)
                if (target != null){
                    val a = 5
                }
            }

//            if (target == null) {
//                target = tryToFetchDeclaration(psiElement)
//                if (target != null){
//                    val a = 5
//                }
//            }

            if (target == null) {
                problemCount++;
                continue
            }

            if (target.containingFile == null || !target.containingFile.virtualFile.isValidForInspection()) {
                continue
            }

            // Extract information from each reference

            val sourceFile = ref.element.containingFile?.virtualFile?.getRelativePath() ?: ""
            val sourceClass = getElementName(findEnclosingElement(ref.element, LocatorType.CLASS))
            val sourceMethodElement = findEnclosingElement(ref.element, LocatorType.METHOD)
            val sourceMethod = getElementName(sourceMethodElement)
            val sourceStructure = getStructure(sourceMethodElement)
            val sourceLineNumber = documentManage.getDocument(ref.element.containingFile)
                ?.getLineNumber(ref.element.textRangeInParent.startOffset)

//            if (sourceMethodElement != null && sourceStructure.isNullOrEmpty()){
//                (sourceMethodElement as PyQualifiedNameOwner).qualifiedName
//                val a = 5
//            }

            val destinationFile = target.containingFile.virtualFile.getRelativePath()
            val destinationClass = getElementName(findEnclosingElement(target, LocatorType.CLASS))
            val destinationMethod = getElementName(findEnclosingElement(target, LocatorType.METHOD))
            val destinationStructure = getStructure(findEnclosingElement(target, LocatorType.METHOD))
            val destinationLineNumber =
                documentManage.getDocument(target.containingFile)?.getLineNumber(target.textRangeInParent.startOffset)

            // Ignore if the source and destination are the same
            if (ignored(sourceFile, destinationFile, sourceClass, destinationClass, sourceMethod, destinationMethod)) {
                continue
            }

            val csvLine =
                "$sourceFile,$sourceClass,$sourceMethod,$sourceStructure,$sourceLineNumber,$destinationFile,$destinationClass,$destinationMethod,$destinationStructure,$destinationLineNumber\n"

            writer.append(csvLine)
        }
    }


//    private fun tryToFetchDeclaration(psiElement: PsiElement): PsiElement? {
//        val context = PyResolveContext.defaultContext(
//            TypeEvalContext.userInitiated(
//                psiElement.getProject(),
//                psiElement.getContainingFile()
//            )
//        )
//        var referenceOwner: PyReferenceOwner? = null
//        val parent: PsiElement = psiElement.parent
//        if (psiElement is PyReferenceOwner) {
//            referenceOwner = psiElement as PyReferenceOwner?
//        } else if (parent is PyReferenceOwner) {
//            referenceOwner = parent
//        }
//
//        val element: PsiElement?
//        if (referenceOwner != null) {
//            element = PyResolveUtil.resolveDeclaration(referenceOwner.getReference(context), context)
//        } else {
//            element = null
//        }
//
//        return element
//    }

    private fun tryDeclarationHandlers(psiElement: PsiElement): PsiElement? {
        val first = FileEditorManager.getInstance(project).allEditors[0]
        val editor = (first as PsiAwareTextEditorImpl).editor

        val newTargets = extensionPointName.extensionList.filter { it is GotoDeclarationHandler }.flatMap {
            (it as GotoDeclarationHandler).getGotoDeclarationTargets(psiElement, 0, editor)?.toList() ?: emptyList()
        }

        return newTargets.firstOrNull()
    }

//    private fun getClassName(element: PsiElement): String {
//        val psiClass = findEnclosingElement<PsiClass>(element)
//        return if (config.addPackageName) {
//            psiClass?.qualifiedName ?: ""
//        } else {
//            psiClass?.name ?: ""
//        }
//    }

    private fun ignored(
        sourceFile: String,
        destinationFile: String,
        sourceClass: String,
        destinationClass: String,
        sourceMethod: String,
        destinationMethod: String
    ): Boolean {
        if (config.ignoreInterFile && sourceFile == destinationFile) return true
        if (config.ignoreInterClass && sourceFile == destinationFile && sourceClass == destinationClass) return true
        if (config.ignoreInterMethod && sourceFile == destinationFile && sourceClass == destinationClass && sourceMethod == destinationMethod) return true

        if (ignoredFilesRegex?.matches(sourceFile) == true) return true
        if (ignoredFilesRegex?.matches(destinationFile) == true) return true
        if (ignoredClassesRegex?.matches(sourceClass) == true) return true
        if (ignoredClassesRegex?.matches(destinationClass) == true) return true
        if (ignoredMethodsRegex?.matches(sourceMethod) == true) return true
        if (ignoredMethodsRegex?.matches(destinationMethod) == true) return true
        return false
    }

    private fun VirtualFile.isValidForInspection() =
        !isDirectory && !fileIndex.isInLibrary(this) && vcsManager.isFileInContent(this) && !name.contains(".gradle.kts")

    private fun VirtualFile.getRelativePath() = path.replace("${project.basePath}/", "")

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
