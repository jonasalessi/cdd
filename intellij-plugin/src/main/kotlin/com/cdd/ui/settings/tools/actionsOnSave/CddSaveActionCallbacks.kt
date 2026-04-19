package com.cdd.ui.settings.tools.actionsOnSave

import com.cdd.ui.settings.tools.cdd.CDDSettingsService

internal data class CddSaveActionCallbacks(
    val readPersistedState: () -> Boolean = { CDDSettingsService.getInstance().isAutoCalculateOnSave() },
    val persistState: (Boolean) -> Unit = { CDDSettingsService.getInstance().setAutoCalculateOnSave(it) },
    val clearAllInlays: () -> Unit = {}
)
