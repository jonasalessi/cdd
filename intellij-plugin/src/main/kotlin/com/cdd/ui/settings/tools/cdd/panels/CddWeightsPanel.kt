package com.cdd.ui.settings.tools.cdd.panels

import com.cdd.CddConstants
import com.cdd.ui.settings.tools.cdd.CddWeightItem
import com.cdd.ui.settings.tools.cdd.WeightColumn
import com.cdd.ui.settings.tools.cdd.WeightPatternColumn
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.table.TableView
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI.Borders.emptyTop
import com.intellij.util.ui.ListTableModel
import javax.swing.JPanel

class CddWeightsPanel {
    private val javaWeightsModel = ListTableModel<CddWeightItem>(
        WeightPatternColumn(),
        WeightColumn("Code Branch", CddConstants.METRIC_CODE_BRANCH),
        WeightColumn("Condition", CddConstants.METRIC_CONDITION),
        WeightColumn("Coupling", CddConstants.METRIC_INTERNAL_COUPLING),
        WeightColumn("Exception", CddConstants.METRIC_EXCEPTION_HANDLING)
    )
    private val kotlinWeightsModel = ListTableModel<CddWeightItem>(
        WeightPatternColumn(),
        WeightColumn("Code Branch", CddConstants.METRIC_CODE_BRANCH),
        WeightColumn("Condition", CddConstants.METRIC_CONDITION),
        WeightColumn("Coupling", CddConstants.METRIC_INTERNAL_COUPLING),
        WeightColumn("Exception", CddConstants.METRIC_EXCEPTION_HANDLING)
    )

    val panel: JPanel

    init {
        val weightsTabs = JBTabbedPane()
        weightsTabs.addTab("Java", createTablePanel(TableView(javaWeightsModel)) { createDefaultWeightItem() })
        weightsTabs.addTab("Kotlin", createTablePanel(TableView(kotlinWeightsModel)) { createDefaultWeightItem() })

        val weightsPanel = FormBuilder.createFormBuilder()
            .addComponent(JBLabel("Configure weights for each complexity metric."))
            .addComponent(JBLabel("Higher weights make specific patterns count more towards the limit."))
            .addComponentFillVertically(weightsTabs, 0)
            .panel
        weightsPanel.border = emptyTop(5)

        panel = weightsPanel
    }

    private fun <T> createTablePanel(table: TableView<T>, createDefaultItem: () -> T): JPanel {
        val decorator = ToolbarDecorator.createDecorator(table)
            .setAddAction {
                val model = table.listTableModel
                model.addRow(createDefaultItem())
            }
            .setRemoveAction {
                val model = table.listTableModel
                val selectedRow = table.selectedRow
                if (selectedRow >= 0) {
                    model.removeRow(selectedRow)
                }
            }

        return decorator.createPanel()
    }

    private fun createDefaultWeightItem() = CddWeightItem(
        CddConstants.WILDCARD_ALL, mutableMapOf(
            CddConstants.METRIC_CODE_BRANCH to 1.0,
            CddConstants.METRIC_CONDITION to 1.0,
            CddConstants.METRIC_INTERNAL_COUPLING to 1.0,
            CddConstants.METRIC_EXCEPTION_HANDLING to 1.0
        )
    )

    fun getJavaWeights(): Map<String, Map<String, Double>> = javaWeightsModel.items.associate { it.pattern to it.weights }
    fun getKotlinWeights(): Map<String, Map<String, Double>> =
        kotlinWeightsModel.items.associate { it.pattern to it.weights }

    fun setJavaWeights(weights: Map<String, Map<String, Double>>) {
        javaWeightsModel.items = weights.map { CddWeightItem(it.key, it.value.toMutableMap()) }.toMutableList()
    }

    fun setKotlinWeights(weights: Map<String, Map<String, Double>>) {
        kotlinWeightsModel.items = weights.map { CddWeightItem(it.key, it.value.toMutableMap()) }.toMutableList()
    }
}
