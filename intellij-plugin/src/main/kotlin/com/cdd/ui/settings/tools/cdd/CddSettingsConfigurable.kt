package com.cdd.ui.settings.tools.cdd

import com.cdd.CddConstants
import com.cdd.ui.settings.tools.cdd.CddSettingsComponent
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import javax.swing.JComponent

class CddSettingsConfigurable(private val project: Project) : Configurable {
    private var component: CddSettingsComponent? = null
    private val configService = CddConfigService.getInstance(project)

    override fun getDisplayName(): String = CddConstants.PLUGIN_NAME

    override fun createComponent(): JComponent? {
        component = CddSettingsComponent()
        return component?.panel
    }

    override fun isModified(): Boolean {
        return CddConfigMapper.toSettingsModel(configService.loadConfig()) != getUiModel()
    }

    override fun apply() {
        configService.saveConfig(CddConfigMapper.toCoreConfig(getUiModel()))
    }

    override fun reset() {
        component?.setSettingsModel(CddConfigMapper.toSettingsModel(configService.loadConfig()))
    }

    override fun disposeUIResources() {
        component = null
    }

    private fun getUiModel(): CddSettingsModel {
        return component?.getSettingsModel() ?: CddSettingsModel(
            javaIcpLimits = mutableMapOf(CddConstants.WILDCARD_ALL to 12),
            kotlinIcpLimits = mutableMapOf(CddConstants.WILDCARD_ALL to 12)
        )
    }
}
