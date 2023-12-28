package com.github.vharatian.refexpo.toolWindow

import com.github.vharatian.refexpo.services.RefExpoService
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.util.application
import java.io.File

class InspectionRunner(private val project: Project) {

    private val service = project.service<RefExpoService>()

    fun run(csvFile: File, onFinished: (Boolean) -> Unit) {
        object: Task.Modal(project, "", true) {
            override fun run(progressIndicator: ProgressIndicator) {
                application.runReadAction{
                    progressIndicator.text = "Loading all project files"
                    service.loadProjectFiles(progressIndicator)

                    progressIndicator.text = "Exporting dependencies"
                    service.exportDependencies(progressIndicator, csvFile)
                }

            }

            override fun onCancel() {
                onFinished(false)
            }

            override fun onSuccess() {
                super.onSuccess()
                onFinished(true)
            }

        }.queue()
    }
}
