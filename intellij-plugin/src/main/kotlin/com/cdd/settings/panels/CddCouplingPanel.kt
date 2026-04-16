package com.cdd.settings.panels

import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.util.ui.FormBuilder
import java.awt.BorderLayout
import javax.swing.DefaultListModel
import javax.swing.JPanel

class CddCouplingPanel {
    private val autoDetectCheckBox = JBCheckBox("Auto-detect internal packages from source files")
    private val packagesListModel = DefaultListModel<String>()
    private val packagesList = JBList(packagesListModel)

    val panel: JPanel

    init {
        val packagesDecorator = ToolbarDecorator.createDecorator(packagesList)
            .setAddAction { addPackage() }
            .setRemoveAction { removePackage() }
            .setEditAction { editSelectedPackage() }
            .disableUpDownActions()

        val packagesPanel = packagesDecorator.createPanel()

        object : com.intellij.ui.DoubleClickListener() {
            override fun onDoubleClick(e: java.awt.event.MouseEvent): Boolean {
                if (packagesList.selectedIndex != -1) {
                    editSelectedPackage()
                    return true
                }
                return false
            }
        }.installOn(packagesList)

        packagesPanel.isVisible = !autoDetectCheckBox.isSelected
        autoDetectCheckBox.addActionListener {
            packagesPanel.isVisible = !autoDetectCheckBox.isSelected
        }

        val couplingTopPanel = FormBuilder.createFormBuilder()
            .addComponent(JBLabel("Settings to define what is considered 'internal' to your project."))
            .addComponent(autoDetectCheckBox)
            .addComponent(JBLabel("Explicit list of package prefixes to treat as internal:"))
            .panel

        val couplingPanel = JPanel(BorderLayout())
        couplingPanel.add(couplingTopPanel, BorderLayout.NORTH)
        couplingPanel.add(packagesPanel, BorderLayout.CENTER)
        couplingPanel.border = com.intellij.util.ui.JBUI.Borders.emptyTop(10)
        
        panel = couplingPanel
    }

    private fun addPackage() {
        val input = javax.swing.JOptionPane.showInputDialog("Enter package prefix (e.g., com.mycompany):")
        if (!input.isNullOrBlank()) {
            packagesListModel.addElement(input.trim())
        }
    }

    private fun removePackage() {
        val selectedIndex = packagesList.selectedIndex
        if (selectedIndex >= 0) {
            packagesListModel.remove(selectedIndex)
        }
    }

    private fun editSelectedPackage() {
        val selectedIndex = packagesList.selectedIndex
        if (selectedIndex >= 0) {
            val current = packagesListModel.get(selectedIndex)
            val input = javax.swing.JOptionPane.showInputDialog("Edit package prefix:", current)
            if (!input.isNullOrBlank()) {
                packagesListModel.set(selectedIndex, input.trim())
            }
        }
    }

    fun isAutoDetect(): Boolean = autoDetectCheckBox.isSelected
    
    fun setAutoDetect(value: Boolean) {
        autoDetectCheckBox.isSelected = value
        // Use loop over action listeners to trigger visibility update, maintaining original logic parity
        autoDetectCheckBox.actionListeners.forEach { it.actionPerformed(null) }
    }

    fun getPackages(): List<String> = packagesListModel.elements().toList()
    
    fun setPackages(packages: List<String>) {
        packagesListModel.clear()
        packages.forEach { packagesListModel.addElement(it) }
    }
}
