package com.cdd.ui.statusbar

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory

class CDDStatusBarWidgetFactory : StatusBarWidgetFactory {
    override fun getId(): String = CDDStatusBarWidget.ID

    override fun getDisplayName(): String = "CDD"

    override fun isAvailable(project: Project): Boolean {
        return !project.isDisposed && !project.isDefault
    }

    override fun createWidget(project: Project): StatusBarWidget = CDDStatusBarWidget(project)

    override fun disposeWidget(widget: StatusBarWidget) {
        widget.dispose()
    }

    override fun canBeEnabledOn(statusBar: com.intellij.openapi.wm.StatusBar): Boolean = true
}
