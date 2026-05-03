package com.cdd.ui.settings.tools.inlay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CddInlaySettingsConfigurableTest {

    @Test
    fun `should expose a display name`() {
        val configurable = CddInlaySettingsConfigurable(
            CddInlaySettingsCallbacks(readState = { CddInlaySettingsState() })
        )

        assertEquals("Inlay Hints", configurable.displayName)
    }

    @Test
    fun `should create a non-null component`() {
        val configurable = CddInlaySettingsConfigurable(
            CddInlaySettingsCallbacks(readState = { CddInlaySettingsState() })
        )

        assertNotNull(configurable.createComponent())
    }

    @Test
    fun `should report unmodified when UI matches persisted state`() {
        val configurable = CddInlaySettingsConfigurable(
            CddInlaySettingsCallbacks(readState = { CddInlaySettingsState(fontSize = 14, position = CddInlayPosition.ABOVE) })
        )
        configurable.createComponent()
        configurable.reset()

        assertFalse(configurable.isModified)
    }

    @Test
    fun `should report modified after font size changes in UI`() {
        val configurable = CddInlaySettingsConfigurable(
            CddInlaySettingsCallbacks(readState = { CddInlaySettingsState(fontSize = 12, position = CddInlayPosition.INLINE) })
        )
        configurable.createComponent()
        configurable.reset()

        configurable.componentForTesting.setState(CddInlaySettingsState(fontSize = 20, position = CddInlayPosition.INLINE))

        assertTrue(configurable.isModified)
    }

    @Test
    fun `should persist UI state when applied`() {
        val captured = mutableListOf<CddInlaySettingsState>()
        val configurable = CddInlaySettingsConfigurable(
            CddInlaySettingsCallbacks(
                readState = { CddInlaySettingsState() },
                persistState = { captured.add(it) }
            )
        )
        configurable.createComponent()
        configurable.reset()
        configurable.componentForTesting.setState(CddInlaySettingsState(fontSize = 16, position = CddInlayPosition.ABOVE))

        configurable.apply()

        assertEquals(listOf(CddInlaySettingsState(fontSize = 16, position = CddInlayPosition.ABOVE)), captured)
    }

    @Test
    fun `should clear inlays on apply when state changed`() {
        var cleared = 0
        val configurable = CddInlaySettingsConfigurable(
            CddInlaySettingsCallbacks(
                readState = { CddInlaySettingsState(fontSize = 12, position = CddInlayPosition.INLINE) },
                clearAllInlays = { cleared++ }
            )
        )
        configurable.createComponent()
        configurable.reset()
        configurable.componentForTesting.setState(CddInlaySettingsState(fontSize = 18, position = CddInlayPosition.ABOVE))

        configurable.apply()

        assertEquals(1, cleared)
    }

    @Test
    fun `should not clear inlays on apply when state is unchanged`() {
        var cleared = 0
        val configurable = CddInlaySettingsConfigurable(
            CddInlaySettingsCallbacks(
                readState = { CddInlaySettingsState(fontSize = 12, position = CddInlayPosition.INLINE) },
                clearAllInlays = { cleared++ }
            )
        )
        configurable.createComponent()
        configurable.reset()

        configurable.apply()

        assertEquals(0, cleared)
    }

    @Test
    fun `should reload UI from persisted state on reset`() {
        val configurable = CddInlaySettingsConfigurable(
            CddInlaySettingsCallbacks(readState = { CddInlaySettingsState(fontSize = 22, position = CddInlayPosition.ABOVE) })
        )
        configurable.createComponent()

        configurable.reset()

        assertEquals(CddInlaySettingsState(fontSize = 22, position = CddInlayPosition.ABOVE), configurable.componentForTesting.getState())
    }
}
