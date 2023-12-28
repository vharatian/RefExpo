package com.github.vharatian.refexpo.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.psi.*
import java.io.BufferedWriter
import java.io.File

@Service(Service.Level.PROJECT)
class RefExpoService(project: Project) {

    private val psiManager = PsiManager.getInstance(project)
    private val fileIndex = ProjectFileIndex.getInstance(project)
    private val vcsManager = ProjectLevelVcsManager.getInstance(project)

    fun loadProjectFiles(progressIndicator: ProgressIndicator) {
        val projectFiles = getAllFiles()
        val visitor = ErrorResilientVisitor { element ->
            element.references.toList()
        }

        progressIndicator.text2

        projectFiles.forEachIndexed { index, psiFile ->
            if (progressIndicator.isCanceled) return@forEachIndexed

            //Update progress indicator
            progressIndicator.fraction = (index + 1) / projectFiles.size.toDouble()
            progressIndicator.text2 = "(${index + 1} / ${projectFiles.size}) -> ${psiFile.name}"

            psiFile.accept(visitor)
        }
    }

    fun getAllFiles(): List<PsiFile> {
        val files = mutableListOf<PsiFile>()


        fileIndex.iterateContent {

            if (!it.isDirectory && !fileIndex.isInLibrary(it) && vcsManager.isFileInContent(it) && !it.name.contains(".gradle.kts")) {
                val file = psiManager.findFile(it)

                file?.let { files.add(file) }
            }

            true
        }

        return files
    }

    fun exportDependencies(progressIndicator: ProgressIndicator, csvFile: File) {
        val projectFiles = getAllFiles()

        // Delete the existing file
        if (csvFile.isFile) {
            csvFile.delete()
        }


        csvFile.bufferedWriter().use { writer ->
            writer.write("SourceFile,SourceClass,SourceMethod,DestinationFile,DestinationClass,DestinationMethod\n")

            val visitor = ErrorResilientVisitor { element ->
                processElement(element, writer)
            }

            projectFiles.forEachIndexed { index, psiFile ->
                if (progressIndicator.isCanceled) return@exportDependencies

                //Update progress indicator
                progressIndicator.fraction = (index + 1) / projectFiles.size.toDouble()
                progressIndicator.text2 = "(${index + 1} / ${projectFiles.size}) -> ${psiFile.name}"

                psiFile.accept(visitor)
            }

            writer.flush()
            writer.close()
        }
    }

    private fun processElement(psiElement: PsiElement, writer: BufferedWriter) {
        // Check all references of the psiElement
        val references = psiElement.references
        for (ref in references) {
            //Check if the destination of the reference is valid
            val destination = ref.resolve()
            if (destination == null || destination.containingFile == null || !psiManager.isInProject(destination)
                || !vcsManager.isFileInContent(destination.containingFile.virtualFile)) {
                continue
            }

            // Extract information from each reference
            val sourceFile = psiElement.containingFile?.virtualFile?.name ?: ""
            val sourceClass = findEnclosingElement<PsiClass>(psiElement)?.name ?: ""
            val sourceMethod = findEnclosingElement<PsiMethod>(psiElement)?.name ?: ""

            val destinationFile = destination.containingFile.virtualFile.name
            val destinationClass = findEnclosingElement<PsiClass>(destination)?.name ?: ""
            val destinationMethod = findEnclosingElement<PsiMethod>(destination)?.name ?: ""

            val csvLine =
                "$sourceFile,$sourceClass,$sourceMethod,$destinationFile,$destinationClass,$destinationMethod\n"

            writer.append(csvLine)
        }
    }

    private inline fun <reified T : PsiElement> findEnclosingElement(element: PsiElement?): T? {
        var currentElement = element
        while (currentElement != null && currentElement !is T) {
            currentElement = currentElement.parent
        }
        return currentElement as? T
    }
}
