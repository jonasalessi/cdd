package com.cdd.ui.settings.tools.cdd.panels

import com.cdd.CddConstants
import com.cdd.ui.settings.tools.cdd.CddRuleItem
import com.cdd.ui.settings.tools.cdd.LimitColumn
import com.cdd.ui.settings.tools.cdd.PatternColumn
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.table.TableView
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI.Borders.emptyTop
import com.intellij.util.ui.ListTableModel
import javax.swing.JPanel

class CddLimitsPanel {
    private val javaLimitsModel = ListTableModel<CddRuleItem>(PatternColumn(), LimitColumn())
    private val kotlinLimitsModel = ListTableModel<CddRuleItem>(PatternColumn(), LimitColumn())

    val panel: JPanel

    init {
        val limitsTabs = JBTabbedPane()
        limitsTabs.addTab(
            "Java",
            createTablePanel(TableView(javaLimitsModel)) { CddRuleItem(CddConstants.WILDCARD_ALL, 12) })
        limitsTabs.addTab(
            "Kotlin",
            createTablePanel(TableView(kotlinLimitsModel)) { CddRuleItem(CddConstants.WILDCARD_ALL, 12) })

        val limitsPanel = FormBuilder.createFormBuilder()
            .addComponent(JBLabel("Configure Intrinsic Complexity Points (ICP) limits per language."))
            .addComponent(JBLabel("Classes exceeding the limit for their matching file pattern will be flagged."))
            .addComponentFillVertically(limitsTabs, 0)
            .panel
        limitsPanel.border = emptyTop(5)

        panel = limitsPanel
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

    fun getJavaRules(): Map<String, Int> = javaLimitsModel.items.associate { it.pattern to it.limit }
    fun getKotlinRules(): Map<String, Int> = kotlinLimitsModel.items.associate { it.pattern to it.limit }

    fun setJavaRules(rules: Map<String, Int>) {
        javaLimitsModel.items = rules.map { CddRuleItem(it.key, it.value) }.toMutableList()
    }

    fun setKotlinRules(rules: Map<String, Int>) {
        kotlinLimitsModel.items = rules.map { CddRuleItem(it.key, it.value) }.toMutableList()
    }
}
