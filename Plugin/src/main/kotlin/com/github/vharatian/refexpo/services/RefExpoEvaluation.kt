package com.github.vharatian.refexpo.services

import com.github.vharatian.refexpo.models.CsvStreamWriter
import com.github.vharatian.refexpo.models.FlatReferenceOutput
import com.github.vharatian.refexpo.models.RefExpoExecutionConfig
import com.github.vharatian.refexpo.utils.isNullOrFalse
import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
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
import java.io.File


class RefExpoEvaluation(private val project: Project, private val config: RefExpoExecutionConfig) {

    private val outputCreateor = ReferenceOutputCreator(project)
    private val psiManager = PsiManager.getInstance(project)
    private val fileIndex = ProjectFileIndex.getInstance(project)
    private val vcsManager = ProjectLevelVcsManager.getInstance(project)

    private val ignoredFilesRegex = config.ignoringFilesRegex.createRegex()
    private val ignoredClassesRegex = config.ignoringClassesRegex.createRegex()
    private val ignoredMethodsRegex = config.ignoringMethodsRegex.createRegex()
    private val scope = GlobalSearchScope.projectScope(project)
    private var problemCount = 0

    private val extensionPointName = ExtensionPointName.create<Any>("com.intellij.gotoDeclarationHandler")
    private val declarationHandlers = extensionPointName.extensionList.filter { it is GotoDeclarationHandler }.toList()

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
                    val filepath = getRelativePath(file.virtualFile)
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

    private fun getRelativePath(file: VirtualFile?) = file?.path?.replace("${project.basePath}/", "") ?: ""


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
        CsvStreamWriter(csvFile, FlatReferenceOutput::class).use { writer ->


            val visitor = ErrorResilientVisitor { element ->
                processElement(element, writer)
            }

            runOverAllFiles(projectFiles, visitor, progressIndicator)

            println("************************** Unresolved references: $problemCount ******************************")
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

    private fun processElement(psiElement: PsiElement, writer: CsvStreamWriter<FlatReferenceOutput>) {
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
                if (target != null) {
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

            outputCreateor.createOutput(psiElement, target).let { refOut ->
                if (ignored(refOut)) {
                    return
                }

                writer.write(refOut)
            }


        }
    }


    private fun tryDeclarationHandlers(psiElement: PsiElement): PsiElement? {
        val first = FileEditorManager.getInstance(project).allEditors[0]
        val editor = (first as PsiAwareTextEditorImpl).editor

        val newTargets = declarationHandlers.flatMap {
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
        output: FlatReferenceOutput
    ): Boolean {
        val sameFile = output.sourcePath == output.targetPath
        val sameClass = sameFile && output.sourceClass == output.targetClass
        val sameMethod = sameClass && output.sourceMethod == output.targetMethod

        if (config.ignoreInterFile && sameFile) return true
        if (config.ignoreInterClass && sameClass) return true
        if (config.ignoreInterMethod && sameMethod) return true

        if (ignoredFilesRegex?.matches(output.sourcePath) == true) return true
        if (ignoredFilesRegex?.matches(output.targetPath) == true) return true
        if (ignoredClassesRegex?.matches(output.sourceClass) == true) return true
        if (ignoredClassesRegex?.matches(output.targetClass) == true) return true
        if (ignoredMethodsRegex?.matches(output.sourceMethod) == true) return true
        if (ignoredMethodsRegex?.matches(output.targetMethod) == true) return true
        return false
    }

    private fun VirtualFile.isValidForInspection() =
        !isDirectory && !fileIndex.isInLibrary(this) && vcsManager.isFileInContent(this) && !name.contains(".gradle.kts")



}
