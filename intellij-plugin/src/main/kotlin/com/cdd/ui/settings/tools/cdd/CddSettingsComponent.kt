package com.cdd.ui.settings.tools.cdd

import com.cdd.ui.settings.tools.cdd.panels.CddCouplingPanel
import com.cdd.ui.settings.tools.cdd.panels.CddFilteringPanel
import com.cdd.ui.settings.tools.cdd.panels.CddLimitsPanel
import com.cdd.ui.settings.tools.cdd.panels.CddWeightsPanel
import com.intellij.ui.JBIntSpinner
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTabbedPane
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI.Borders.emptyTop
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

class CddSettingsComponent {
    val panel: JComponent

    private val limitsPanel = CddLimitsPanel()
    private val weightsPanel = CddWeightsPanel()
    private val couplingPanel = CddCouplingPanel()
    private val filteringPanel = CddFilteringPanel()
    private val methodLimitSpinner = JBIntSpinner(24, 1, 1000)

    init {
        val slocPanel = createSlocPanel()

        val mainTabs = JBTabbedPane()
        mainTabs.addTab("ICP Limits", limitsPanel.panel)
        mainTabs.addTab("ICP Weights", weightsPanel.panel)
        mainTabs.addTab("Internal Coupling", couplingPanel.panel)
        mainTabs.addTab("File Filtering", filteringPanel.panel)
        mainTabs.addTab("SLOC Metrics", slocPanel)

        val mainPanel = JPanel(BorderLayout())
        mainPanel.add(mainTabs, BorderLayout.NORTH)

        panel = mainPanel
    }

    private fun createSlocPanel(): JPanel {
        val slocPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent("Method line limit:", methodLimitSpinner)
            .addComponent(JBLabel("Maximum lines of code allowed in a single method."))
            .addComponentFillVertically(JPanel(), 0)
            .panel
        slocPanel.border = emptyTop(10)
        return slocPanel
    }

    fun getJavaRules(): Map<String, Int> = limitsPanel.getJavaRules()
    fun getKotlinRules(): Map<String, Int> = limitsPanel.getKotlinRules()

    fun setJavaRules(rules: Map<String, Int>) {
        limitsPanel.setJavaRules(rules)
    }

    fun setKotlinRules(rules: Map<String, Int>) {
        limitsPanel.setKotlinRules(rules)
    }

    fun getJavaWeights(): Map<String, Map<String, Double>> = weightsPanel.getJavaWeights()
    fun getKotlinWeights(): Map<String, Map<String, Double>> = weightsPanel.getKotlinWeights()

    fun setJavaWeights(weights: Map<String, Map<String, Double>>) {
        weightsPanel.setJavaWeights(weights)
    }

    fun setKotlinWeights(weights: Map<String, Map<String, Double>>) {
        weightsPanel.setKotlinWeights(weights)
    }

    fun isAutoDetect(): Boolean = couplingPanel.isAutoDetect()

    fun setAutoDetect(value: Boolean) {
        couplingPanel.setAutoDetect(value)
    }

    fun getPackages(): List<String> = couplingPanel.getPackages()

    fun setPackages(packages: List<String>) {
        couplingPanel.setPackages(packages)
    }

    fun getIncludePatterns(): List<String> = filteringPanel.getIncludePatterns()

    fun setIncludePatterns(patterns: List<String>) {
        filteringPanel.setIncludePatterns(patterns)
    }

    fun getExcludePatterns(): List<String> = filteringPanel.getExcludePatterns()

    fun setExcludePatterns(patterns: List<String>) {
        filteringPanel.setExcludePatterns(patterns)
    }

    fun getMethodLimit(): Int = methodLimitSpinner.number
    fun setMethodLimit(limit: Int) {
        methodLimitSpinner.number = limit
    }

    fun getSettingsModel(): CddSettingsModel {
        return CddSettingsModel(
            javaIcpLimits = getJavaRules().toMutableMap(),
            kotlinIcpLimits = getKotlinRules().toMutableMap(),
            javaMetrics = getJavaWeights().mapValues { it.value.toMutableMap() }.toMutableMap(),
            kotlinMetrics = getKotlinWeights().mapValues { it.value.toMutableMap() }.toMutableMap(),
            autoDetect = isAutoDetect(),
            packages = getPackages().toMutableList(),
            include = getIncludePatterns().toMutableList(),
            exclude = getExcludePatterns().toMutableList(),
            methodLimit = getMethodLimit()
        )
    }

    fun setSettingsModel(model: CddSettingsModel) {
        setJavaRules(model.javaIcpLimits)
        setKotlinRules(model.kotlinIcpLimits)
        setJavaWeights(model.javaMetrics)
        setKotlinWeights(model.kotlinMetrics)
        setAutoDetect(model.autoDetect)
        setPackages(model.packages)
        setIncludePatterns(model.include)
        setExcludePatterns(model.exclude)
        setMethodLimit(model.methodLimit)
    }
}