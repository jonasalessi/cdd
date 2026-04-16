package com.cdd.ui.inlay

import com.cdd.domain.*
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseListener
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.JBColor
import java.awt.Graphics
import java.awt.Rectangle

internal interface CddIcpInlayService {
    fun render(project: Project, file: VirtualFile, result: AnalysisResult)
}

internal object EditorCddIcpInlayService : CddIcpInlayService {
    private val listenerInstalledKey = Key.create<Boolean>("cdd.icp.inlay.listener.installed")
    private var reportFactory: CddIcpReportFactory = AnalysisCddIcpReportFactory
    private var reportPresenter: CddIcpReportPresenter = DialogCddIcpReportPresenter

    override fun render(project: Project, file: VirtualFile, result: AnalysisResult) {
        FileEditorManager.getInstance(project)
            .getEditors(file)
            .filterIsInstance<TextEditor>()
            .map { it.editor }
            .forEach { editor ->
                ensureClickListener(editor, project)
                clear(editor)
                if (result.errors.isEmpty()) {
                    addHints(editor, result)
                }
            }
    }

    internal fun clear(editor: Editor) {
        editor.inlayModel
            .getAfterLineEndElementsInRange(0, editor.document.textLength)
            .filter { it.renderer is CddIcpTextRenderer }
            .forEach(Inlay<*>::dispose)
    }

    private fun addHints(editor: Editor, result: AnalysisResult) {
        result.classes.forEach { classAnalysis ->
            addHint(
                editor,
                classAnalysis.lineRange.start,
                classHintText(classAnalysis),
                reportFactory.createClassReport(classAnalysis)
            )
            classAnalysis.methods.forEach { methodAnalysis ->
                addHint(
                    editor,
                    methodAnalysis.lineRange.start,
                    methodHintText(methodAnalysis),
                    reportFactory.createMethodReport(methodAnalysis)
                )
            }
        }
    }

    private fun addHint(editor: Editor, lineNumber: Int, text: String, report: CddIcpReport) {
        val zeroBasedLine = lineNumber - 1
        if (zeroBasedLine !in 0 until editor.document.lineCount) {
            return
        }
        val offset = editor.document.getLineEndOffset(zeroBasedLine)
        editor.inlayModel.addAfterLineEndElement(offset, false, CddIcpTextRenderer(text, report))
    }

    private fun classHintText(classAnalysis: ClassAnalysis): String {
        return "  Class ICP: ${formatNumber(classAnalysis.totalIcp)} | SLOC: ${classAnalysis.sloc.codeOnly}/${classAnalysis.sloc.total}"
    }

    private fun methodHintText(methodAnalysis: MethodAnalysis): String {
        return "  ICP: ${formatNumber(methodAnalysis.totalIcp)} | SLOC: ${methodAnalysis.sloc.codeOnly}/${methodAnalysis.sloc.total}"
    }

    private fun formatNumber(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.toInt().toString()
        } else {
            value.toString()
        }
    }

    private fun ensureClickListener(editor: Editor, project: Project) {
        if (editor.getUserData(listenerInstalledKey) == true) {
            return
        }
        editor.addEditorMouseListener(object : EditorMouseListener {
            override fun mouseClicked(event: EditorMouseEvent) {
                val renderer = event.inlay?.renderer as? CddIcpTextRenderer ?: return
                reportPresenter.show(project, renderer.report)
                event.consume()
            }
        })
        editor.putUserData(listenerInstalledKey, true)
    }
}

internal class CddIcpTextRenderer(
    internal val text: String,
    internal val report: CddIcpReport
) : EditorCustomElementRenderer {
    override fun calcWidthInPixels(inlay: Inlay<*>): Int {
        val metrics = inlay.editor.contentComponent.getFontMetrics(font(inlay.editor))
        return metrics.stringWidth(text)
    }

    override fun paint(inlay: Inlay<*>, g: Graphics, targetRegion: Rectangle, textAttributes: TextAttributes) {
        val editor = inlay.editor
        g.font = font(editor)
        g.color = JBColor.GRAY
        g.drawString(text, targetRegion.x, targetRegion.y + editor.ascent)
    }

    private fun font(editor: Editor) = editor.colorsScheme.getFont(EditorFontType.PLAIN)
}
