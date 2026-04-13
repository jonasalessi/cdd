package com.cdd.listener

import com.cdd.CddConstants
import com.cdd.settings.CDDSettingsService
import com.cdd.settings.CddConfigService
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.intellij.openapi.project.ProjectLocator

class IcpAutoCalculateOnSaveListener : FileDocumentManagerListener {
    override fun beforeAnyDocumentSaving(document: Document, explicit: Boolean) {
        if (!CDDSettingsService.getInstance().isAutoCalculateOnSave()) {
            return
        }
        val file = FileDocumentManager.getInstance().getFile(document) ?: return
        val extension = file.extension?.lowercase() ?: ""
        if (extension != CddConstants.FILE_EXTENSION_KOTLIN && extension != CddConstants.FILE_EXTENSION_JAVA) {
            return
        }
        val project = ProjectLocator.getInstance().guessProjectForFile(file)
        if (project != null) {
            val ktFile = file.toNioPath().toFile()
            val config = CddConfigService.getInstance(project).loadConfig()
            //TODO here to call the analizer and show inlay the results for each line number
        }

    }

    companion object {
        const val NOTIFICATION_GROUP_ID = "cddAutoCalculateOnSave"
    }
}
