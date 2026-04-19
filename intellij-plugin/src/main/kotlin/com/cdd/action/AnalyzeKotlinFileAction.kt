package com.cdd.action

import com.cdd.analyzer.kotlin.KotlinFileAnalysisRunner
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys

class AnalyzeKotlinFileAction() : AnAction() {
    private var analysisRunner = KotlinFileAnalysisRunner()

    internal constructor(analysisRunner: KotlinFileAnalysisRunner) : this() {
        this.analysisRunner = analysisRunner
    }

    override fun update(event: AnActionEvent) {
        val file = event.getData(CommonDataKeys.VIRTUAL_FILE)
        val shouldShow = event.project != null && file != null && analysisRunner.isKotlinFile(file)
        event.presentation.isEnabledAndVisible = shouldShow
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val file = event.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        analysisRunner.analyze(project, file)
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
}
