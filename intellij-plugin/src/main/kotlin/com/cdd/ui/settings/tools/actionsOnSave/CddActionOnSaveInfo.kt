package com.cdd.ui.settings.tools.actionsOnSave

import com.intellij.ide.actionsOnSave.ActionOnSaveContext
import com.intellij.ide.actionsOnSave.ActionOnSaveInfo

class CddActionOnSaveInfo(context: ActionOnSaveContext) : ActionOnSaveInfo(context) {
    private var callbacks = CddSaveActionCallbacks()
    private var enabled = callbacks.readPersistedState()

    internal constructor(context: ActionOnSaveContext, callbacks: CddSaveActionCallbacks) : this(context) {
        this.callbacks = callbacks
        this.enabled = callbacks.readPersistedState()
    }

    override fun getActionOnSaveName() = ACTION_NAME

    override fun isActionOnSaveEnabled() = enabled

    override fun setActionOnSaveEnabled(value: Boolean) {
        enabled = value
    }

    public override fun isModified() = enabled != callbacks.readPersistedState()

    public override fun apply() {
        callbacks.persistState(enabled)
        if (!enabled) callbacks.clearAllInlays()
    }

    companion object {
        const val ACTION_NAME = "Calculate CDD on Save"
    }
}
