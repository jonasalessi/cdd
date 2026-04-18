package com.cdd.ui.settings.tools.actionsOnSave

import com.cdd.ui.editor.inlay.EditorCddIcpInlayService
import com.intellij.ide.actionsOnSave.ActionOnSaveContext
import com.intellij.ide.actionsOnSave.ActionOnSaveInfo
import com.intellij.ide.actionsOnSave.ActionOnSaveInfoProvider

class CddActionOnSaveInfoProvider : ActionOnSaveInfoProvider() {
    override fun getActionOnSaveInfos(context: ActionOnSaveContext): Collection<ActionOnSaveInfo> {
        val project = context.project
        val callbacks = CddSaveActionCallbacks(clearAllInlays = { EditorCddIcpInlayService.clearAll(project) })
        return listOf(CddActionOnSaveInfo(context, callbacks))
    }
}
