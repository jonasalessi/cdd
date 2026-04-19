package com.cdd.ui.settings.tools.cdd.panels

import com.cdd.CddConstants
import com.intellij.ui.DoubleClickListener
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import java.awt.event.MouseEvent
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException
import javax.swing.DefaultListModel
import javax.swing.JOptionPane
import javax.swing.JPanel

class CddFilteringPanel {
    private val includeListModel = DefaultListModel<String>()
    private val excludeListModel = DefaultListModel<String>()

    val panel: JPanel

    init {
        val includeList = JBList(includeListModel)
        val includePanel = createListPanel(
            includeList,
            includeListModel,
            "Add include pattern (e.g., glob:src/**/*.java):",
            "Edit include pattern:"
        )

        val excludeList = JBList(excludeListModel)
        val excludePanel = createListPanel(
            excludeList,
            excludeListModel,
            "Add exclude pattern (e.g., regex:.*Test.java):",
            "Edit exclude pattern:"
        )

        val filteringPanel = FormBuilder.createFormBuilder()
            .addComponent(JBLabel("Include specific files (empty = include all):"))
            .addComponent(includePanel)
            .addComponent(JBLabel("Exclude specific files (e.g., generated code, tests):"))
            .addComponent(excludePanel)
            .panel
        filteringPanel.border = JBUI.Borders.emptyTop(10)

        panel = filteringPanel
    }

    private fun createListPanel(
        list: JBList<String>,
        model: DefaultListModel<String>,
        addMsg: String,
        editMsg: String
    ): JPanel {
        val decorator = ToolbarDecorator.createDecorator(list)
            .setAddAction { addPattern(model, addMsg) }
            .setRemoveAction { removePattern(list, model) }
            .setEditAction { editPattern(list, model, editMsg) }
            .disableUpDownActions()

        val panel = decorator.createPanel()

        object : DoubleClickListener() {
            override fun onDoubleClick(e: MouseEvent): Boolean {
                if (list.selectedIndex != -1) {
                    editPattern(list, model, editMsg)
                    return true
                }
                return false
            }
        }.installOn(list)

        return panel
    }

    private fun addPattern(model: DefaultListModel<String>, msg: String) {
        val input = JOptionPane.showInputDialog(msg)
        if (input != null) {
            val trimmed = input.trim()
            if (isValidPattern(trimmed)) {
                model.addElement(trimmed)
            }
        }
    }

    private fun removePattern(list: JBList<String>, model: DefaultListModel<String>) {
        val selectedIndex = list.selectedIndex
        if (selectedIndex >= 0) {
            model.remove(selectedIndex)
        }
    }

    private fun editPattern(list: JBList<String>, model: DefaultListModel<String>, msg: String) {
        val selectedIndex = list.selectedIndex
        if (selectedIndex >= 0) {
            val current = model.get(selectedIndex)
            val input = JOptionPane.showInputDialog(msg, current)
            if (input != null) {
                val trimmed = input.trim()
                if (isValidPattern(trimmed)) {
                    model.set(selectedIndex, trimmed)
                }
            }
        }
    }

    private fun isValidPattern(pattern: String): Boolean {
        val error = validatePattern(pattern)
        if (error != null) {
            JOptionPane.showMessageDialog(null, error, "Invalid Pattern", JOptionPane.ERROR_MESSAGE)
            return false
        }
        return pattern.isNotEmpty()
    }

    private fun validatePattern(input: String): String? {
        if (input.isBlank()) return "Pattern cannot be empty"

        if (input.startsWith(CddConstants.PREFIX_REGEX)) {
            val pattern = input.substring(CddConstants.PREFIX_REGEX.length)
            if (pattern.isBlank()) return "Regex pattern cannot be empty"
            try {
                Pattern.compile(pattern)
            } catch (e: PatternSyntaxException) {
                return "Invalid regular expression: ${e.message}"
            }
        } else if (input.startsWith(CddConstants.PREFIX_GLOB)) {
            val pattern = input.substring(CddConstants.PREFIX_GLOB.length)
            if (pattern.isBlank()) return "Glob pattern cannot be empty"
        }
        return null
    }

    fun getIncludePatterns(): List<String> = includeListModel.elements().toList()
    fun setIncludePatterns(patterns: List<String>) {
        includeListModel.clear()
        patterns.forEach { includeListModel.addElement(it) }
    }

    fun getExcludePatterns(): List<String> = excludeListModel.elements().toList()
    fun setExcludePatterns(patterns: List<String>) {
        excludeListModel.clear()
        patterns.forEach { excludeListModel.addElement(it) }
    }
}
