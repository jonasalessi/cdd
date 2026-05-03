package com.cdd.ui.settings.tools.inlay

enum class CddInlayPosition { ABOVE, INLINE }

data class CddInlaySettingsState(
    var fontSize: Int = 12,
    var position: CddInlayPosition = CddInlayPosition.INLINE
)
