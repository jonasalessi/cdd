package com.cdd.listener

import com.cdd.analyzer.kotlin.KotlinFileAnalysisRunner
import com.cdd.ui.settings.tools.cdd.CDDSettingsService
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectLocator
import com.intellij.openapi.vfs.VirtualFile

class IcpAutoCalculateOnSaveListener(
) : FileDocumentManagerListener {
    private var analysisRunner = KotlinFileAnalysisRunner()
    private var resolveFile: (Document) -> VirtualFile? =
        { document -> FileDocumentManager.getInstance().getFile(document) }
    private var resolveProject: (VirtualFile) -> Project? =
        { file -> ProjectLocator.getInstance().guessProjectForFile(file) }
    private var scheduleAnalysis: ((() -> Unit) -> Unit) =
        { action -> ApplicationManager.getApplication().invokeLater(action) }

    internal constructor(
        analysisRunner: KotlinFileAnalysisRunner,
        resolveFile: (Document) -> VirtualFile? =
            { document -> FileDocumentManager.getInstance().getFile(document) },
        resolveProject: (VirtualFile) -> Project? =
            { file -> ProjectLocator.getInstance().guessProjectForFile(file) },
        scheduleAnalysis: ((() -> Unit) -> Unit) =
            { action -> ApplicationManager.getApplication().invokeLater(action) }
    ) : this() {
        this.analysisRunner = analysisRunner
        this.resolveFile = resolveFile
        this.resolveProject = resolveProject
        this.scheduleAnalysis = scheduleAnalysis
    }

    override fun beforeDocumentSaving(document: Document) {
        if (!CDDSettingsService.getInstance().isAutoCalculateOnSave()) {
            return
        }
        val file = resolveFile(document) ?: return
        val project = resolveProject(file) ?: return
        scheduleAnalysis {
            analysisRunner.analyze(project, file)
        }
    }
}
