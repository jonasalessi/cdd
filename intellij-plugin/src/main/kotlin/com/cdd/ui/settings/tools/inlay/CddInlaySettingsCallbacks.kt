package com.cdd.ui.settings.tools.inlay

data class CddInlaySettingsCallbacks(
    val readState: () -> CddInlaySettingsState,
    val persistState: (CddInlaySettingsState) -> Unit = {},
    val clearAllInlays: () -> Unit = {}
)
