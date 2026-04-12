package com.cdd.settings

import com.cdd.CddConstants
import com.cdd.model.CddConfig
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
        return configService.loadConfig() != getUiConfig()
    }

    override fun apply() {
        configService.saveConfig(getUiConfig())
    }

    override fun reset() {
        setUiState(configService.loadConfig())
    }

    override fun disposeUIResources() {
        component = null
    }

    private fun getUiConfig(): CddConfig {
        val config = CddConfig()
        
        config.icpLimits[CddConstants.LANGUAGE_JAVA] = component?.getJavaRules()?.toMutableMap() ?: mutableMapOf()
        config.icpLimits[CddConstants.LANGUAGE_KOTLIN] = component?.getKotlinRules()?.toMutableMap() ?: mutableMapOf()

        config.metrics[CddConstants.LANGUAGE_JAVA] = component?.getJavaWeights()?.mapValues { it.value.toMutableMap() }?.toMutableMap() ?: mutableMapOf()
        config.metrics[CddConstants.LANGUAGE_KOTLIN] = component?.getKotlinWeights()?.mapValues { it.value.toMutableMap() }?.toMutableMap() ?: mutableMapOf()

        config.internalCoupling.autoDetect = component?.isAutoDetect() ?: true
        config.internalCoupling.packages = component?.getPackages()?.toMutableList() ?: mutableListOf()

        config.include = component?.getIncludePatterns()?.toMutableList() ?: mutableListOf()
        config.exclude = component?.getExcludePatterns()?.toMutableList() ?: mutableListOf()

        config.sloc.methodLimit = component?.getMethodLimit() ?: 24

        return config
    }

    private fun setUiState(config: CddConfig) {
        component?.setJavaRules(config.icpLimits[CddConstants.LANGUAGE_JAVA] ?: emptyMap())
        component?.setKotlinRules(config.icpLimits[CddConstants.LANGUAGE_KOTLIN] ?: emptyMap())
        
        component?.setJavaWeights(config.metrics[CddConstants.LANGUAGE_JAVA] ?: emptyMap())
        component?.setKotlinWeights(config.metrics[CddConstants.LANGUAGE_KOTLIN] ?: emptyMap())
        
        component?.setAutoDetect(config.internalCoupling.autoDetect)
        component?.setPackages(config.internalCoupling.packages)
        
        component?.setIncludePatterns(config.include)
        component?.setExcludePatterns(config.exclude)
        component?.setMethodLimit(config.sloc.methodLimit)
    }
}
