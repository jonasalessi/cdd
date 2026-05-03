package com.cdd.ui.settings.tools.inlay

import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.JBIntSpinner
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI.Borders.emptyTop
import javax.swing.JComponent

private const val MIN_FONT_SIZE = 6
private const val MAX_FONT_SIZE = 72
private const val DEFAULT_FONT_SIZE = 12

class CddInlaySettingsComponent {
    val panel: JComponent

    private val fontSizeSpinner = JBIntSpinner(DEFAULT_FONT_SIZE, MIN_FONT_SIZE, MAX_FONT_SIZE)
    private val positionCombo = ComboBox(CddInlayPosition.values())

    init {
        positionCombo.selectedItem = CddInlayPosition.INLINE
        panel = FormBuilder.createFormBuilder()
            .addLabeledComponent("Font size:", fontSizeSpinner)
            .addLabeledComponent("Position:", positionCombo)
            .addComponentFillVertically(javax.swing.JPanel(), 0)
            .panel
            .also { it.border = emptyTop(10) }
    }

    fun getState(): CddInlaySettingsState {
        return CddInlaySettingsState(
            fontSize = fontSizeSpinner.number,
            position = positionCombo.selectedItem as CddInlayPosition
        )
    }

    fun setState(state: CddInlaySettingsState) {
        fontSizeSpinner.number = state.fontSize
        positionCombo.selectedItem = state.position
    }
}
