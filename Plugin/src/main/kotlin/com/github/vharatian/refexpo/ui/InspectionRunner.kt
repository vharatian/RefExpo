package com.github.vharatian.refexpo.ui

import com.github.vharatian.refexpo.models.RefExpoExecutionConfig
import com.github.vharatian.refexpo.services.RefExpoEvaluation
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.util.application


class InspectionRunner(private val project: Project) {

    fun run(config: RefExpoExecutionConfig, onFinished: (Boolean) -> Unit) {
        object: Task.Modal(project, "", true) {
            override fun run(progressIndicator: ProgressIndicator) {
                application.runReadAction{
                    RefExpoEvaluation(project, config).runInspections(progressIndicator)
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
