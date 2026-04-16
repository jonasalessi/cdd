package com.cdd.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.*

@Service(Service.Level.APP)
@State(name = "cddIdeaSettings", storages = [Storage("cdd-idea.xml")])
class CDDSettingsService : PersistentStateComponent<CDDSettingsState> {
    private var state = CDDSettingsState()

    override fun getState(): CDDSettingsState = state

    override fun loadState(state: CDDSettingsState) {
        this.state = state
    }

    fun isAutoCalculateOnSave(): Boolean = state.autoCalculateOnSave

    fun setAutoCalculateOnSave(enabled: Boolean) {
        if (state.autoCalculateOnSave == enabled) {
            return
        }
        state.autoCalculateOnSave = enabled
        ApplicationManager.getApplication()
            .messageBus
            .syncPublisher(IcpSettingsListener.TOPIC)
            .handleAutoCalculateOnSaveChanged(enabled)
    }

    companion object {
        fun getInstance(): CDDSettingsService = service()
    }
}
