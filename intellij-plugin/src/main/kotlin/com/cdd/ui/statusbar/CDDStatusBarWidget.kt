package com.cdd.ui.statusbar

import com.cdd.CddConstants
import com.cdd.settings.CDDSettingsService
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.util.Consumer
import java.awt.event.MouseEvent

class CDDStatusBarWidget(private val project: Project) : StatusBarWidget, StatusBarWidget.TextPresentation {
    private var statusBar: StatusBar? = null

    override fun ID(): String = ID

    override fun install(statusBar: StatusBar) {
        this.statusBar = statusBar
    }

    override fun dispose() {
        statusBar = null
    }

    override fun getPresentation(): StatusBarWidget.WidgetPresentation = this

    override fun getText(): String = CddConstants.PLUGIN_NAME

    override fun getAlignment(): Float = 0f

    override fun getTooltipText(): String = if (hasSupportedFileOpen()) {
        "CDD options"
    } else {
        "CDD options (open a .java or .kt file to calculate)"
    }

    override fun getClickConsumer(): Consumer<MouseEvent> = Consumer { event ->
        val actionGroup = DefaultActionGroup(
            AutoCalculateOnSaveToggleAction { statusBar?.updateWidget(ID) }
        )
        val dataContext = DataManager.getInstance().getDataContext(event.component)
        val popup = JBPopupFactory.getInstance()
            .createActionGroupPopup(
                "Intrinsic Cognitive Point (ICP)",
                actionGroup,
                dataContext,
                JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                true
            )
        popup.showUnderneathOf(event.component)
    }

    private fun hasSupportedFileOpen(): Boolean {
        if (project.isDisposed || !project.isInitialized) {
            return false
        }
        val selectedFiles = FileEditorManager.getInstance(project).selectedFiles
        return selectedFiles.any { file ->
            val extension = file.extension?.lowercase()
            extension == CddConstants.FILE_EXTENSION_JAVA || extension == CddConstants.FILE_EXTENSION_KOTLIN
        }
    }

    private class AutoCalculateOnSaveToggleAction(
        private val onUpdated: () -> Unit
    ) : ToggleAction("Auto Calculate ICP When Saving File") {

        override fun getActionUpdateThread(): ActionUpdateThread {
            return ActionUpdateThread.BGT
        }

        override fun isSelected(e: AnActionEvent): Boolean {
            return CDDSettingsService.getInstance().isAutoCalculateOnSave()
        }

        override fun update(e: AnActionEvent) {
            super.update(e)
            e.presentation.isVisible = true
            e.presentation.isEnabled = true
        }

        override fun setSelected(e: AnActionEvent, state: Boolean) {
            CDDSettingsService.getInstance().setAutoCalculateOnSave(state)
            onUpdated()
        }
    }

    companion object {
        const val ID = "cddStatusBarWidget"
    }
}
