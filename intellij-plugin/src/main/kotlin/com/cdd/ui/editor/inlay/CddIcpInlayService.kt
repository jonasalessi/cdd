package com.cdd.ui.editor.inlay

import com.cdd.domain.AnalysisResult
import com.cdd.domain.ClassAnalysis
import com.cdd.domain.MethodAnalysis
import com.cdd.ui.settings.tools.inlay.CddInlayPosition
import com.cdd.ui.settings.tools.inlay.CddInlaySettingsService
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseListener
import com.intellij.openapi.editor.event.EditorMouseMotionListener
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.JBColor
import java.awt.Cursor
import java.awt.Graphics
import java.awt.Rectangle

private val HAND_CURSOR: Cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
private val HOVER_COLOR: JBColor = JBColor.namedColor("Link.activeForeground", JBColor(0x2470B3, 0x589DF6))

internal interface CddIcpInlayService {
    fun render(project: Project, file: VirtualFile, result: AnalysisResult)
}

internal object EditorCddIcpInlayService : CddIcpInlayService {
    private val listenerInstalledKey = Key.create<Boolean>("cdd.icp.inlay.listener.installed")
    internal val hoveredInlayKey: Key<Inlay<*>> = Key.create("cdd.icp.inlay.hovered")
    private var reportFactory: CddIcpReportFactory = AnalysisCddIcpReportFactory
    private var reportPresenter: CddIcpReportPresenter = DialogCddIcpReportPresenter

    fun clearAll(project: Project) {
        FileEditorManager.getInstance(project).openFiles.forEach { file ->
            FileEditorManager.getInstance(project).getEditors(file)
                .filterIsInstance<TextEditor>()
                .map { it.editor }
                .forEach { clear(it) }
        }
    }

    override fun render(project: Project, file: VirtualFile, result: AnalysisResult) {
        FileEditorManager.getInstance(project)
            .getEditors(file)
            .filterIsInstance<TextEditor>()
            .map { it.editor }
            .forEach { editor ->
                ensureMouseListeners(editor, project)
                clear(editor)
                if (result.errors.isEmpty()) {
                    addHints(editor, result)
                }
            }
    }

    internal fun clear(editor: Editor) {
        val textLength = editor.document.textLength
        editor.inlayModel
            .getAfterLineEndElementsInRange(0, textLength)
            .filter { it.renderer is CddIcpTextRenderer }
            .forEach(Inlay<*>::dispose)
        editor.inlayModel
            .getBlockElementsInRange(0, textLength)
            .filter { it.renderer is CddIcpTextRenderer }
            .forEach(Inlay<*>::dispose)
    }

    private fun addHints(editor: Editor, result: AnalysisResult) {
        val position = CddInlaySettingsService.getInstance().state.position
        result.classes.forEach { classAnalysis ->
            addHint(
                editor,
                classAnalysis.lineRange.start,
                classHintText(classAnalysis),
                reportFactory.createClassReport(classAnalysis),
                position
            )
            classAnalysis.methods.forEach { methodAnalysis ->
                addHint(
                    editor,
                    methodAnalysis.lineRange.start,
                    methodHintText(methodAnalysis),
                    reportFactory.createMethodReport(methodAnalysis),
                    position
                )
            }
        }
    }

    private fun addHint(editor: Editor, lineNumber: Int, text: String, report: CddIcpReport, position: CddInlayPosition) {
        val zeroBasedLine = lineNumber - 1
        if (zeroBasedLine !in 0 until editor.document.lineCount) {
            return
        }
        when (position) {
            CddInlayPosition.INLINE -> {
                val offset = editor.document.getLineEndOffset(zeroBasedLine)
                editor.inlayModel.addAfterLineEndElement(offset, false, CddIcpTextRenderer(text, report))
            }
            CddInlayPosition.ABOVE -> {
                val offset = editor.document.getLineStartOffset(zeroBasedLine)
                val leftMargin = computeLineIndentPixels(editor, zeroBasedLine)
                editor.inlayModel.addBlockElement(offset, false, true, 0, CddIcpTextRenderer(text, report, leftMargin))
            }
        }
    }

    private fun computeLineIndentPixels(editor: Editor, zeroBasedLine: Int): Int {
        val document = editor.document
        val lineStart = document.getLineStartOffset(zeroBasedLine)
        val lineEnd = document.getLineEndOffset(zeroBasedLine)
        val chars = document.charsSequence
        var firstNonWs = lineStart
        while (firstNonWs < lineEnd && chars[firstNonWs].isWhitespace()) {
            firstNonWs++
        }
        if (firstNonWs == lineStart) return 0
        val visual = editor.offsetToVisualPosition(firstNonWs)
        return editor.visualPositionToXY(visual).x
    }

    private fun classHintText(classAnalysis: ClassAnalysis): String {
        return "Class ICP: ${formatNumber(classAnalysis.totalIcp)} | SLOC: ${classAnalysis.sloc.codeOnly}/${classAnalysis.sloc.total}"
    }

    private fun methodHintText(methodAnalysis: MethodAnalysis): String {
        return "ICP: ${formatNumber(methodAnalysis.totalIcp)} | SLOC: ${methodAnalysis.sloc.codeOnly}/${methodAnalysis.sloc.total}"
    }

    private fun formatNumber(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.toInt().toString()
        } else {
            value.toString()
        }
    }

    private fun ensureMouseListeners(editor: Editor, project: Project) {
        if (editor.getUserData(listenerInstalledKey) == true) {
            return
        }
        editor.addEditorMouseListener(object : EditorMouseListener {
            override fun mouseClicked(event: EditorMouseEvent) {
                val renderer = event.inlay?.renderer as? CddIcpTextRenderer ?: return
                reportPresenter.show(project, renderer.report)
                event.consume()
            }

            override fun mouseExited(event: EditorMouseEvent) {
                applyInlayCursor(event.editor, null)
            }
        })
        editor.addEditorMouseMotionListener(object : EditorMouseMotionListener {
            override fun mouseMoved(event: EditorMouseEvent) {
                applyInlayCursor(event.editor, event.inlay)
            }
        })
        editor.putUserData(listenerInstalledKey, true)
    }

    internal fun applyInlayCursor(editor: Editor, inlay: Inlay<*>?) {
        val editorEx = editor as? EditorEx ?: return
        val previous = editor.getUserData(hoveredInlayKey)
        val current = if (inlay?.renderer is CddIcpTextRenderer) inlay else null
        if (previous !== current) {
            editor.putUserData(hoveredInlayKey, current)
            previous?.let { repaintInlay(editor, it) }
            current?.let { repaintInlay(editor, it) }
        }
        editorEx.setCustomCursor(this, if (current != null) HAND_CURSOR else null)
    }

    private fun repaintInlay(editor: Editor, inlay: Inlay<*>) {
        val bounds = inlay.bounds ?: return
        editor.contentComponent.repaint(bounds)
    }
}

internal class CddIcpTextRenderer(
    internal val text: String,
    internal val report: CddIcpReport,
    internal val leftMargin: Int = 0
) : EditorCustomElementRenderer {
    override fun calcWidthInPixels(inlay: Inlay<*>): Int {
        val metrics = inlay.editor.contentComponent.getFontMetrics(font(inlay.editor))
        return leftMargin + metrics.stringWidth(text)
    }

    override fun paint(inlay: Inlay<*>, g: Graphics, targetRegion: Rectangle, textAttributes: TextAttributes) {
        val editor = inlay.editor
        g.font = font(editor)
        val isHovered = editor.getUserData(EditorCddIcpInlayService.hoveredInlayKey) === inlay
        g.color = if (isHovered) HOVER_COLOR else JBColor.GRAY
        val baselineY = targetRegion.y + editor.ascent
        val textX = targetRegion.x + leftMargin
        g.drawString(text, textX, baselineY)
        if (isHovered) {
            val width = g.fontMetrics.stringWidth(text)
            g.drawLine(textX, baselineY + 1, textX + width - 1, baselineY + 1)
        }
    }

    internal fun font(editor: Editor): java.awt.Font {
        val base = editor.colorsScheme.getFont(EditorFontType.PLAIN)
        val size = CddInlaySettingsService.getInstance().state.fontSize
        return base.deriveFont(size.toFloat())
    }
}
