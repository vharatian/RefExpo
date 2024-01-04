package com.github.vharatian.refexpo.services

import com.github.vharatian.refexpo.models.RefExpoExecutionConfig
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import java.io.BufferedWriter
import java.io.File
import com.github.vharatian.refexpo.utils.isNullOrFalse


class RefExpoEvaluation(private val project: Project, private val config: RefExpoExecutionConfig) {

    private val psiManager = PsiManager.getInstance(project)
    private val fileIndex = ProjectFileIndex.getInstance(project)
    private val vcsManager = ProjectLevelVcsManager.getInstance(project)

    private val ignoredFilesRegex = config.ignoringFilesRegex.createRegex()
    private val ignoredClassesRegex = config.ignoringClassesRegex.createRegex()
    private val ignoredMethodsRegex = config.ignoringMethodsRegex.createRegex()

    private fun String.createRegex() = if (isNotEmpty())
    {
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
            writer.write("SourceFile,SourceClass,SourceMethod,TargetFile,TargetClass,TargetMethod\n")

            val visitor = ErrorResilientVisitor { element ->
                processElement(element, writer)
            }

            runOverAllFiles(projectFiles, visitor, progressIndicator)

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
        val references = psiElement.references
        for (ref in references) {

            //Check if the destination of the reference is valid
            val target = ref.resolve()
            if (target == null || target.containingFile == null || !target.containingFile.virtualFile.isValidForInspection()) {
                continue
            }

            // Extract information from each reference
            val sourceFile = psiElement.containingFile?.virtualFile?.getRelativePath() ?: ""
            val sourceClass = getClassName(psiElement)
            val sourceMethod = findEnclosingElement<PsiMethod>(psiElement)?.name ?: ""

            val destinationFile = target.containingFile.virtualFile.getRelativePath()
            val destinationClass = getClassName(target)
            val destinationMethod = findEnclosingElement<PsiMethod>(target)?.name ?: ""

            // Ignore if the source and destination are the same
            if (ignored(sourceFile, destinationFile, sourceClass, destinationClass, sourceMethod, destinationMethod)) {
                continue
            }

            val csvLine =
                "$sourceFile,$sourceClass,$sourceMethod,$destinationFile,$destinationClass,$destinationMethod\n"

            writer.append(csvLine)
        }
    }

    private fun getClassName(element: PsiElement): String {
        val psiClass = findEnclosingElement<PsiClass>(element)
        return if (config.addPackageName) {
            psiClass?.qualifiedName ?: ""
        } else {
            psiClass?.name ?: ""
        }
    }

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

    private inline fun <reified T : PsiElement> findEnclosingElement(element: PsiElement?): T? {
        var currentElement = element
        while (currentElement != null && currentElement !is T) {
            currentElement = currentElement.parent
        }
        return currentElement as? T
    }
}
