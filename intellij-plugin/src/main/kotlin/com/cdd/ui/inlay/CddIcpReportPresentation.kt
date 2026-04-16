package com.cdd.ui.inlay

import com.cdd.domain.ClassAnalysis
import com.cdd.domain.IcpInstance
import com.cdd.domain.IcpType
import com.cdd.domain.MethodAnalysis
import com.cdd.domain.SlocMetrics
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import java.awt.ComponentOrientation
import java.awt.Dimension
import javax.swing.Action
import javax.swing.JComponent
import javax.swing.JScrollPane
import javax.swing.JTextArea

internal data class CddIcpReport(
    val title: String,
    val message: String
)

internal interface CddIcpReportFactory {
    fun createClassReport(classAnalysis: ClassAnalysis): CddIcpReport
    fun createMethodReport(methodAnalysis: MethodAnalysis): CddIcpReport
}

internal object AnalysisCddIcpReportFactory : CddIcpReportFactory {
    override fun createClassReport(classAnalysis: ClassAnalysis): CddIcpReport {
        return CddIcpReport(
            title = "CDD Report: ${classAnalysis.name}",
            message = buildString {
                appendLine("Type: Class")
                appendLine("Name: ${classAnalysis.name}")
                appendLine("Package: ${classAnalysis.packageName.ifBlank { "<default>" }}")
                appendLine("ICP: ${formatNumber(classAnalysis.totalIcp)}")
                appendLine("Over Limit: ${classAnalysis.isOverLimit}")
                appendLine("Line Range: ${classAnalysis.lineRange.start}-${classAnalysis.lineRange.endInclusive}")
                appendLine("SLOC: ${slocText(classAnalysis.sloc)}")
                appendBreakdown(classAnalysis.icpBreakdown)
            }.trimEnd()
        )
    }

    override fun createMethodReport(methodAnalysis: MethodAnalysis): CddIcpReport {
        return CddIcpReport(
            title = "CDD Report: ${methodAnalysis.className}.${methodAnalysis.name}",
            message = buildString {
                appendLine("Type: Method")
                appendLine("Class: ${methodAnalysis.className}")
                appendLine("Name: ${methodAnalysis.name}")
                appendLine("ICP: ${formatNumber(methodAnalysis.totalIcp)}")
                appendLine("Over SLOC Limit: ${methodAnalysis.isOverSlocLimit}")
                appendLine("Line Range: ${methodAnalysis.lineRange.start}-${methodAnalysis.lineRange.endInclusive}")
                appendLine("SLOC: ${slocText(methodAnalysis.sloc)}")
                appendBreakdown(methodAnalysis.icpBreakdown)
            }.trimEnd()
        )
    }

    private fun slocText(sloc: SlocMetrics): String {
        return "${sloc.codeOnly}/${sloc.total} (with comments: ${sloc.withComments}, comments: ${sloc.comments}, blank: ${sloc.blankLines})"
    }

    private fun StringBuilder.appendBreakdown(breakdown: Map<IcpType, List<IcpInstance>>) {
        appendLine("Breakdown:")
        if (breakdown.isEmpty()) {
            appendLine("- none")
            return
        }
        breakdown.entries
            .sortedBy { it.key.name }
            .forEach { (type, instances) ->
                appendLine("- ${type.name}: ${instances.size}")
                instances.forEach { instance ->
                    appendLine(
                        "  • line ${instance.line}, col ${instance.column}: ${instance.description} (${formatNumber(instance.weight)})"
                    )
                }
            }
    }

    private fun formatNumber(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.toInt().toString()
        } else {
            value.toString()
        }
    }
}

internal fun interface CddIcpReportPresenter {
    fun show(project: Project, report: CddIcpReport)
}

internal object DialogCddIcpReportPresenter : CddIcpReportPresenter {
    override fun show(project: Project, report: CddIcpReport) {
        CddIcpReportDialog(project, report).show()
    }
}

private class CddIcpReportDialog(
    project: Project,
    report: CddIcpReport
) : DialogWrapper(project) {
    private val message = report.message

    init {
        title = report.title
        setResizable(true)
        init()
    }

    override fun createCenterPanel(): JComponent {
        val textArea = JTextArea(message)
        textArea.isEditable = false
        textArea.lineWrap = true
        textArea.wrapStyleWord = true
        textArea.caretPosition = 0
        textArea.alignmentX = 0f
        textArea.componentOrientation = ComponentOrientation.LEFT_TO_RIGHT
        return JScrollPane(textArea).apply {
            preferredSize = Dimension(820, 520)
        }
    }

    override fun createActions(): Array<Action> {
        return arrayOf(okAction)
    }
}
