package com.cdd.ui.settings.tools.actionsOnSave

import com.intellij.ide.actionsOnSave.ActionOnSaveContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import sun.misc.Unsafe

class CddActionOnSaveInfoTest {

    @Test
    fun `should expose correct action name`() {
        val info = CddActionOnSaveInfo(stubContext(), CddSaveActionCallbacks(readPersistedState = { false }))

        assertEquals(CddActionOnSaveInfo.ACTION_NAME, info.getActionOnSaveName())
    }

    @Test
    fun `should report disabled when setting is false`() {
        val info = CddActionOnSaveInfo(stubContext(), CddSaveActionCallbacks(readPersistedState = { false }))

        assertFalse(info.isActionOnSaveEnabled())
    }

    @Test
    fun `should report enabled when setting is true`() {
        val info = CddActionOnSaveInfo(stubContext(), CddSaveActionCallbacks(readPersistedState = { true }))

        assertTrue(info.isActionOnSaveEnabled())
    }

    @Test
    fun `should enable setting locally when activated`() {
        val info = CddActionOnSaveInfo(stubContext(), CddSaveActionCallbacks(readPersistedState = { false }))

        info.setActionOnSaveEnabled(true)

        assertTrue(info.isActionOnSaveEnabled())
    }

    @Test
    fun `should disable setting locally when deactivated`() {
        val info = CddActionOnSaveInfo(stubContext(), CddSaveActionCallbacks(readPersistedState = { true }))

        info.setActionOnSaveEnabled(false)

        assertFalse(info.isActionOnSaveEnabled())
    }

    @Test
    fun `should report unmodified when local state matches persisted state`() {
        val info = CddActionOnSaveInfo(stubContext(), CddSaveActionCallbacks(readPersistedState = { false }))

        assertFalse(info.isModified())
    }

    @Test
    fun `should report modified after changing enabled state`() {
        var persisted = false
        val info = CddActionOnSaveInfo(stubContext(), CddSaveActionCallbacks(readPersistedState = { persisted }, persistState = { persisted = it }))

        info.setActionOnSaveEnabled(true)

        assertTrue(info.isModified())
    }

    @Test
    fun `should persist enabled state when applied`() {
        val captured = mutableListOf<Boolean>()
        val info = CddActionOnSaveInfo(stubContext(), CddSaveActionCallbacks(readPersistedState = { false }, persistState = { captured.add(it) }))
        info.setActionOnSaveEnabled(true)

        info.apply()

        assertEquals(listOf(true), captured)
    }

    @Test
    fun `should report unmodified after applying changes`() {
        var persisted = false
        val info = CddActionOnSaveInfo(stubContext(), CddSaveActionCallbacks(readPersistedState = { persisted }, persistState = { persisted = it }))
        info.setActionOnSaveEnabled(true)

        info.apply()

        assertFalse(info.isModified())
    }

    @Test
    fun `should clear inlays when disabled on apply`() {
        var cleared = false
        val info = CddActionOnSaveInfo(stubContext(), CddSaveActionCallbacks(readPersistedState = { true }, clearAllInlays = { cleared = true }))
        info.setActionOnSaveEnabled(false)

        info.apply()

        assertTrue(cleared)
    }

    @Test
    fun `should not clear inlays when enabled on apply`() {
        var cleared = false
        val info = CddActionOnSaveInfo(stubContext(), CddSaveActionCallbacks(readPersistedState = { false }, clearAllInlays = { cleared = true }))
        info.setActionOnSaveEnabled(true)

        info.apply()

        assertFalse(cleared)
    }

    private fun stubContext(): ActionOnSaveContext {
        val unsafe = Unsafe::class.java.getDeclaredField("theUnsafe")
            .also { it.isAccessible = true }
            .get(null) as Unsafe
        return unsafe.allocateInstance(ActionOnSaveContext::class.java) as ActionOnSaveContext
    }
}
