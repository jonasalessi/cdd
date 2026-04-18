package com.cdd.settings

import com.intellij.ide.actionsOnSave.ActionOnSaveContext
import com.intellij.ide.actionsOnSave.ActionOnSaveInfo

class CddActionOnSaveInfo(
    context: ActionOnSaveContext,
    private val readPersistedState: () -> Boolean = { CDDSettingsService.getInstance().isAutoCalculateOnSave() },
    private val persistState: (Boolean) -> Unit = { CDDSettingsService.getInstance().setAutoCalculateOnSave(it) }
) : ActionOnSaveInfo(context) {
    private var enabled = readPersistedState()

    override fun getActionOnSaveName() = ACTION_NAME

    override fun isActionOnSaveEnabled() = enabled

    override fun setActionOnSaveEnabled(value: Boolean) {
        enabled = value
    }

    public override fun isModified() = enabled != readPersistedState()

    public override fun apply() {
        persistState(enabled)
    }

    companion object {
        const val ACTION_NAME = "Calculate CDD on Save"
    }
}
