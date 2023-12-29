package com.github.vharatian.refexpo.services

import com.github.vharatian.refexpo.models.RefExpoExecutionConfig
import com.intellij.openapi.components.Service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import java.io.BufferedWriter
import java.io.File

@Service(Service.Level.PROJECT)
class RefExpoService(val project: Project) {

    private val psiManager = PsiManager.getInstance(project)
    private val fileIndex = ProjectFileIndex.getInstance(project)
    private val vcsManager = ProjectLevelVcsManager.getInstance(project)


    fun runInspections(config: RefExpoExecutionConfig, progressIndicator: ProgressIndicator) {
        progressIndicator.text = "Finding project files"
        val projectFiles = getAllFiles(progressIndicator)

        progressIndicator.text = "Loading all references"
        loadProjectFiles(projectFiles, progressIndicator)

        progressIndicator.text = "Exporting dependencies"
        exportDependencies(projectFiles, progressIndicator, config)
    }

    fun getAllFiles(progressIndicator: ProgressIndicator): List<PsiFile> {
        val files = mutableListOf<PsiFile>()

        fileIndex.iterateContent {
            if (it.isValidForInspection()) {
                val file = psiManager.findFile(it)

                file?.let {
                    files.add(file)
                    progressIndicator.text2 = "${files.size} -> ${file.name}"
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
        projectFiles: List<PsiFile>, progressIndicator: ProgressIndicator, config: RefExpoExecutionConfig
    ) {
        val csvFile = createOutputFile(config.filePath)

        csvFile.bufferedWriter().use { writer ->
            writer.write("SourceFile,SourceClass,SourceMethod,DestinationFile,DestinationClass,DestinationMethod\n")

            val visitor = ErrorResilientVisitor { element ->
                processElement(element, writer, config)
            }

            runOverAllFiles(projectFiles, visitor, progressIndicator)

            writer.flush()
            writer.close()
        }
    }

    private fun runOverAllFiles(
        projectFiles: List<PsiFile>, visitor: ErrorResilientVisitor, progressIndicator: ProgressIndicator
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

    private fun processElement(psiElement: PsiElement, writer: BufferedWriter, config: RefExpoExecutionConfig) {
        // Check all references of the psiElement
        val references = psiElement.references
        for (ref in references) {

            //Check if the destination of the reference is valid
            val destination = ref.resolve()
            if (destination == null || destination.containingFile == null || !destination.containingFile.virtualFile.isValidForInspection()) {
                continue
            }

            // Extract information from each reference
            val sourceFile = psiElement.containingFile?.virtualFile?.getRelativePath() ?: ""
            val sourceClass = findEnclosingElement<PsiClass>(psiElement)?.name ?: ""
            val sourceMethod = findEnclosingElement<PsiMethod>(psiElement)?.name ?: ""

            val destinationFile = destination.containingFile.virtualFile.getRelativePath()
            val destinationClass = findEnclosingElement<PsiClass>(destination)?.name ?: ""
            val destinationMethod = findEnclosingElement<PsiMethod>(destination)?.name ?: ""

            // Ignore if the source and destination are the same
            if (config.ignoreInterFile && sourceFile == destinationFile) continue
            if (config.ignoreInterClass && sourceFile == destinationFile && sourceClass == destinationClass) continue
            if (config.ignoreInterMethod && sourceFile == destinationFile && sourceClass == destinationClass && sourceMethod == destinationMethod) continue

            val csvLine =
                "$sourceFile,$sourceClass,$sourceMethod,$destinationFile,$destinationClass,$destinationMethod\n"

            writer.append(csvLine)
        }
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
