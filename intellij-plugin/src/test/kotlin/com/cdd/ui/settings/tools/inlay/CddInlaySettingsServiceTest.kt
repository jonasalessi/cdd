package com.cdd.ui.settings.tools.inlay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test

class CddInlaySettingsServiceTest {

    @Test
    fun `should default font size to twelve`() {
        val service = CddInlaySettingsService()

        assertEquals(12, service.state.fontSize)
    }

    @Test
    fun `should default position to inline`() {
        val service = CddInlaySettingsService()

        assertEquals(CddInlayPosition.INLINE, service.state.position)
    }

    @Test
    fun `should expose its state instance`() {
        val service = CddInlaySettingsService()

        assertSame(service.state, service.getState())
    }

    @Test
    fun `should replace state when loaded`() {
        val service = CddInlaySettingsService()
        val replacement = CddInlaySettingsState(fontSize = 18, position = CddInlayPosition.ABOVE)

        service.loadState(replacement)

        assertSame(replacement, service.state)
    }

    @Test
    fun `should retain replaced font size after loadState`() {
        val service = CddInlaySettingsService()

        service.loadState(CddInlaySettingsState(fontSize = 20, position = CddInlayPosition.INLINE))

        assertEquals(20, service.state.fontSize)
    }

    @Test
    fun `should retain replaced position after loadState`() {
        val service = CddInlaySettingsService()

        service.loadState(CddInlaySettingsState(fontSize = 12, position = CddInlayPosition.ABOVE))

        assertEquals(CddInlayPosition.ABOVE, service.state.position)
    }

    @Test
    fun `should swap underlying state reference on loadState`() {
        val service = CddInlaySettingsService()
        val original = service.state

        service.loadState(CddInlaySettingsState(fontSize = 14, position = CddInlayPosition.INLINE))

        assertNotSame(original, service.state)
    }
}
