package com.cdd.ui.settings.tools.inlay

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service

@Service(Service.Level.APP)
@State(name = "cddInlaySettings", storages = [Storage("cdd-inlay.xml")])
class CddInlaySettingsService : PersistentStateComponent<CddInlaySettingsState> {
    private var internalState = CddInlaySettingsState()

    override fun getState(): CddInlaySettingsState = internalState

    override fun loadState(state: CddInlaySettingsState) {
        this.internalState = state
    }

    companion object {
        fun getInstance(): CddInlaySettingsService = service()
    }
}
