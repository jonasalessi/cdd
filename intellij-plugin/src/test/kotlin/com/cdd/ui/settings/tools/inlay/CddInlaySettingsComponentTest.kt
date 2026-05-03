package com.cdd.ui.settings.tools.inlay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class CddInlaySettingsComponentTest {

    @Test
    fun `should expose a non-null panel`() {
        val component = CddInlaySettingsComponent()

        assertNotNull(component.panel)
    }

    @Test
    fun `should default to inline position and twelve font size`() {
        val component = CddInlaySettingsComponent()

        val state = component.getState()

        assertEquals(12, state.fontSize)
        assertEquals(CddInlayPosition.INLINE, state.position)
    }

    @Test
    fun `should round-trip font size through setState and getState`() {
        val component = CddInlaySettingsComponent()

        component.setState(CddInlaySettingsState(fontSize = 18, position = CddInlayPosition.INLINE))

        assertEquals(18, component.getState().fontSize)
    }

    @Test
    fun `should round-trip above position through setState and getState`() {
        val component = CddInlaySettingsComponent()

        component.setState(CddInlaySettingsState(fontSize = 12, position = CddInlayPosition.ABOVE))

        assertEquals(CddInlayPosition.ABOVE, component.getState().position)
    }

    @Test
    fun `should round-trip inline position through setState and getState`() {
        val component = CddInlaySettingsComponent()
        component.setState(CddInlaySettingsState(fontSize = 12, position = CddInlayPosition.ABOVE))

        component.setState(CddInlaySettingsState(fontSize = 12, position = CddInlayPosition.INLINE))

        assertEquals(CddInlayPosition.INLINE, component.getState().position)
    }
}
