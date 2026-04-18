package com.cdd.settings

import com.intellij.ide.actionsOnSave.ActionOnSaveContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import sun.misc.Unsafe

class CddActionOnSaveInfoTest {

    @Test
    fun `should expose correct action name`() {
        val info = CddActionOnSaveInfo(stubContext(), readPersistedState = { false }, persistState = {})

        assertEquals(CddActionOnSaveInfo.ACTION_NAME, info.getActionOnSaveName())
    }

    @Test
    fun `should report disabled when setting is false`() {
        val info = CddActionOnSaveInfo(stubContext(), readPersistedState = { false }, persistState = {})

        assertFalse(info.isActionOnSaveEnabled())
    }

    @Test
    fun `should report enabled when setting is true`() {
        val info = CddActionOnSaveInfo(stubContext(), readPersistedState = { true }, persistState = {})

        assertTrue(info.isActionOnSaveEnabled())
    }

    @Test
    fun `should enable setting locally when activated`() {
        val info = CddActionOnSaveInfo(stubContext(), readPersistedState = { false }, persistState = {})

        info.setActionOnSaveEnabled(true)

        assertTrue(info.isActionOnSaveEnabled())
    }

    @Test
    fun `should disable setting locally when deactivated`() {
        val info = CddActionOnSaveInfo(stubContext(), readPersistedState = { true }, persistState = {})

        info.setActionOnSaveEnabled(false)

        assertFalse(info.isActionOnSaveEnabled())
    }

    @Test
    fun `should report unmodified when local state matches persisted state`() {
        val info = CddActionOnSaveInfo(stubContext(), readPersistedState = { false }, persistState = {})

        assertFalse(info.isModified())
    }

    @Test
    fun `should report modified after changing enabled state`() {
        var persisted = false
        val info = CddActionOnSaveInfo(stubContext(), readPersistedState = { persisted }, persistState = { persisted = it })

        info.setActionOnSaveEnabled(true)

        assertTrue(info.isModified())
    }

    @Test
    fun `should persist enabled state when applied`() {
        val captured = mutableListOf<Boolean>()
        val info = CddActionOnSaveInfo(stubContext(), readPersistedState = { false }, persistState = { captured.add(it) })
        info.setActionOnSaveEnabled(true)

        info.apply()

        assertEquals(listOf(true), captured)
    }

    @Test
    fun `should report unmodified after applying changes`() {
        var persisted = false
        val info = CddActionOnSaveInfo(stubContext(), readPersistedState = { persisted }, persistState = { persisted = it })
        info.setActionOnSaveEnabled(true)

        info.apply()

        assertFalse(info.isModified())
    }

    private fun stubContext(): ActionOnSaveContext {
        val unsafe = Unsafe::class.java.getDeclaredField("theUnsafe")
            .also { it.isAccessible = true }
            .get(null) as Unsafe
        return unsafe.allocateInstance(ActionOnSaveContext::class.java) as ActionOnSaveContext
    }
}
