package com.cdd.ui.settings.tools.inlay

import com.cdd.ui.editor.inlay.EditorCddIcpInlayService
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.ProjectManager
import javax.swing.JComponent
import org.jetbrains.annotations.TestOnly

private const val DISPLAY_NAME = "Inlay Hints"

class CddInlaySettingsConfigurable(
    private val callbacks: CddInlaySettingsCallbacks
) : Configurable {
    private var component: CddInlaySettingsComponent? = null

    constructor() : this(
        CddInlaySettingsCallbacks(
            readState = { copyOf(CddInlaySettingsService.getInstance().state) },
            persistState = { CddInlaySettingsService.getInstance().loadState(it) },
            clearAllInlays = { clearInlaysAcrossOpenProjects() }
        )
    )

    override fun getDisplayName(): String = DISPLAY_NAME

    override fun createComponent(): JComponent {
        val created = CddInlaySettingsComponent()
        component = created
        created.setState(callbacks.readState())
        return created.panel
    }

    override fun isModified(): Boolean = component?.getState() != callbacks.readState()

    override fun apply() {
        val current = component?.getState() ?: return
        val previous = callbacks.readState()
        callbacks.persistState(current)
        if (previous != current) {
            callbacks.clearAllInlays()
        }
    }

    override fun reset() {
        component?.setState(callbacks.readState())
    }

    override fun disposeUIResources() {
        component = null
    }

    @get:TestOnly
    internal val componentForTesting: CddInlaySettingsComponent
        get() = component ?: error("createComponent must be called first")

    companion object {
        private fun copyOf(state: CddInlaySettingsState): CddInlaySettingsState =
            state.copy()

        private fun clearInlaysAcrossOpenProjects() {
            ProjectManager.getInstance().openProjects.forEach { project ->
                EditorCddIcpInlayService.clearAll(project)
            }
        }
    }
}
