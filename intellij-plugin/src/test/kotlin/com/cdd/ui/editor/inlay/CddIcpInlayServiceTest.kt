package com.cdd.ui.editor.inlay

import com.cdd.domain.*
import com.cdd.ui.settings.tools.inlay.CddInlayPosition
import com.cdd.ui.settings.tools.inlay.CddInlaySettingsService
import com.cdd.ui.settings.tools.inlay.CddInlaySettingsState
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixture4TestCase
import java.awt.Cursor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CddIcpInlayServiceTest : LightPlatformCodeInsightFixture4TestCase() {

    @Before
    fun resetInlaySettings() {
        CddInlaySettingsService.getInstance().loadState(CddInlaySettingsState())
    }

    @Test
    fun `should render class and method inlays`() {
        val psiFile = myFixture.configureByText(
            "Sample.kt",
            """
            class Sample {
                fun first() {}
                fun second() {}
            }
            """.trimIndent()
        )

        EditorCddIcpInlayService.render(project, psiFile.virtualFile, sampleResult())

        assertEquals(
            listOf(
                "Class ICP: 3",
                "ICP: 1",
                "ICP: 2"
            ),
            cddInlayTexts()
        )
    }

    @Test
    fun `should attach dialog content to rendered inlays`() {
        val psiFile = myFixture.configureByText(
            "Sample.kt",
            """
            class Sample {
                fun first() {}
            }
            """.trimIndent()
        )

        EditorCddIcpInlayService.render(project, psiFile.virtualFile, sampleResult())

        val renderers = myFixture.editor.inlayModel
            .getAfterLineEndElementsInRange(0, myFixture.editor.document.textLength)
            .mapNotNull { it.renderer as? CddIcpTextRenderer }

        assertEquals("CDD Report: Sample", renderers.first().report.title)
        assertTrue(renderers.first().report.message.contains("Type: Class"))
        assertTrue(renderers.first().report.message.contains("Breakdown:"))
        assertEquals("CDD Report: Sample.first", renderers[1].report.title)
        assertTrue(renderers[1].report.message.contains("Type: Method"))
    }

    @Test
    fun `should clear previous inlays before rendering`() {
        val psiFile = myFixture.configureByText(
            "Sample.kt",
            """
            class Sample {
                fun first() {}
            }
            """.trimIndent()
        )
        EditorCddIcpInlayService.render(project, psiFile.virtualFile, sampleResult())

        EditorCddIcpInlayService.render(project, psiFile.virtualFile, sampleResult())

        assertEquals(3, cddInlayTexts().size)
    }

    @Test
    fun `should clear inlays when analysis has errors`() {
        val psiFile = myFixture.configureByText(
            "Sample.kt",
            """
            class Sample {
                fun first() {}
            }
            """.trimIndent()
        )
        EditorCddIcpInlayService.render(project, psiFile.virtualFile, sampleResult())

        EditorCddIcpInlayService.render(
            project,
            psiFile.virtualFile,
            AnalysisResult(
                file = "Sample.kt",
                classes = emptyList(),
                totalIcp = 0.0,
                errors = listOf(
                    AnalysisError("Sample.kt", message = "Failed", severity = ErrorSeverity.ERROR)
                )
            )
        )

        assertTrue(cddInlayTexts().isEmpty())
    }

    @Test
    fun `should show hand cursor when hovering CDD inlay`() {
        val psiFile = myFixture.configureByText(
            "Sample.kt",
            """
            class Sample {
                fun first() {}
            }
            """.trimIndent()
        )
        EditorCddIcpInlayService.render(project, psiFile.virtualFile, sampleResult())
        val editor = myFixture.editor as EditorEx
        val cddInlay = editor.inlayModel
            .getAfterLineEndElementsInRange(0, editor.document.textLength)
            .first { it.renderer is CddIcpTextRenderer }

        EditorCddIcpInlayService.applyInlayCursor(editor, cddInlay)

        assertEquals(Cursor.HAND_CURSOR, editor.contentComponent.cursor.type)
    }

    @Test
    fun `should clear hand cursor when hovering outside CDD inlay`() {
        val psiFile = myFixture.configureByText(
            "Sample.kt",
            """
            class Sample {
                fun first() {}
            }
            """.trimIndent()
        )
        EditorCddIcpInlayService.render(project, psiFile.virtualFile, sampleResult())
        val editor = myFixture.editor as EditorEx
        val cddInlay = editor.inlayModel
            .getAfterLineEndElementsInRange(0, editor.document.textLength)
            .first { it.renderer is CddIcpTextRenderer }
        EditorCddIcpInlayService.applyInlayCursor(editor, cddInlay)

        EditorCddIcpInlayService.applyInlayCursor(editor, null)

        assertFalse(editor.contentComponent.cursor.type == Cursor.HAND_CURSOR)
    }

    @Test
    fun `should mark inlay as hovered when pointer enters inlay`() {
        val psiFile = myFixture.configureByText(
            "Sample.kt",
            """
            class Sample {
                fun first() {}
            }
            """.trimIndent()
        )
        EditorCddIcpInlayService.render(project, psiFile.virtualFile, sampleResult())
        val editor = myFixture.editor as EditorEx
        val cddInlay = editor.inlayModel
            .getAfterLineEndElementsInRange(0, editor.document.textLength)
            .first { it.renderer is CddIcpTextRenderer }

        EditorCddIcpInlayService.applyInlayCursor(editor, cddInlay)

        assertSame(cddInlay, editor.getUserData(EditorCddIcpInlayService.hoveredInlayKey))
    }

    @Test
    fun `should unmark inlay when pointer leaves inlay`() {
        val psiFile = myFixture.configureByText(
            "Sample.kt",
            """
            class Sample {
                fun first() {}
            }
            """.trimIndent()
        )
        EditorCddIcpInlayService.render(project, psiFile.virtualFile, sampleResult())
        val editor = myFixture.editor as EditorEx
        val cddInlay = editor.inlayModel
            .getAfterLineEndElementsInRange(0, editor.document.textLength)
            .first { it.renderer is CddIcpTextRenderer }
        EditorCddIcpInlayService.applyInlayCursor(editor, cddInlay)

        EditorCddIcpInlayService.applyInlayCursor(editor, null)

        assertNull(editor.getUserData(EditorCddIcpInlayService.hoveredInlayKey))
    }

    @Test
    fun `should place inlays after line end when position is inline`() {
        CddInlaySettingsService.getInstance().loadState(CddInlaySettingsState(position = CddInlayPosition.INLINE))
        val psiFile = myFixture.configureByText("Sample.kt", "class Sample {\n    fun first() {}\n}\n")

        EditorCddIcpInlayService.render(project, psiFile.virtualFile, sampleResult())

        assertTrue(afterLineEndCddInlays().isNotEmpty())
        assertTrue(blockCddInlays().isEmpty())
    }

    @Test
    fun `should place inlays above line when position is above`() {
        CddInlaySettingsService.getInstance().loadState(CddInlaySettingsState(position = CddInlayPosition.ABOVE))
        val psiFile = myFixture.configureByText("Sample.kt", "class Sample {\n    fun first() {}\n}\n")

        EditorCddIcpInlayService.render(project, psiFile.virtualFile, sampleResult())

        assertTrue(blockCddInlays().isNotEmpty())
        assertTrue(afterLineEndCddInlays().isEmpty())
    }

    @Test
    fun `should align above block inlays with line indentation`() {
        CddInlaySettingsService.getInstance().loadState(CddInlaySettingsState(position = CddInlayPosition.ABOVE))
        val psiFile = myFixture.configureByText(
            "Sample.kt",
            """
            class Sample {
                fun first() {}
                fun second() {}
            }
            """.trimIndent()
        )

        EditorCddIcpInlayService.render(project, psiFile.virtualFile, sampleResult())

        val classRenderer = blockCddInlays()
            .map { it.renderer as CddIcpTextRenderer }
            .first { it.text.startsWith("Class ICP") }
        val methodRenderer = blockCddInlays()
            .map { it.renderer as CddIcpTextRenderer }
            .first { it.text.startsWith("ICP: 1") }
        assertEquals(0, classRenderer.leftMargin)
        assertTrue(methodRenderer.leftMargin > 0)
    }

    @Test
    fun `should use zero left margin for inline inlays`() {
        CddInlaySettingsService.getInstance().loadState(CddInlaySettingsState(position = CddInlayPosition.INLINE))
        val psiFile = myFixture.configureByText(
            "Sample.kt",
            """
            class Sample {
                fun first() {}
                fun second() {}
            }
            """.trimIndent()
        )

        EditorCddIcpInlayService.render(project, psiFile.virtualFile, sampleResult())

        val methodRenderer = afterLineEndCddInlays()
            .map { it.renderer as CddIcpTextRenderer }
            .first { it.text.startsWith("ICP: 1") }
        assertEquals(0, methodRenderer.leftMargin)
    }

    @Test
    fun `should dispose block inlays on clear`() {
        CddInlaySettingsService.getInstance().loadState(CddInlaySettingsState(position = CddInlayPosition.ABOVE))
        val psiFile = myFixture.configureByText("Sample.kt", "class Sample {\n    fun first() {}\n}\n")
        EditorCddIcpInlayService.render(project, psiFile.virtualFile, sampleResult())

        EditorCddIcpInlayService.clearAll(project)

        assertTrue(blockCddInlays().isEmpty())
    }

    @Test
    fun `should render with font size from settings`() {
        CddInlaySettingsService.getInstance().loadState(CddInlaySettingsState(fontSize = 24, position = CddInlayPosition.INLINE))
        val psiFile = myFixture.configureByText("Sample.kt", "class Sample {\n    fun first() {}\n}\n")

        EditorCddIcpInlayService.render(project, psiFile.virtualFile, sampleResult())

        val renderer = afterLineEndCddInlays().first().renderer as CddIcpTextRenderer
        assertEquals(24, renderer.font(myFixture.editor).size)
    }

    @Test
    fun `should clear all inlays from open editors`() {
        val psiFile = myFixture.configureByText(
            "Sample.kt",
            """
            class Sample {
                fun first() {}
            }
            """.trimIndent()
        )
        EditorCddIcpInlayService.render(project, psiFile.virtualFile, sampleResult())

        EditorCddIcpInlayService.clearAll(project)

        assertTrue(cddInlayTexts().isEmpty())
    }

    private fun cddInlayTexts(): List<String> {
        return myFixture.editor.inlayModel
            .getAfterLineEndElementsInRange(0, myFixture.editor.document.textLength)
            .mapNotNull { (it.renderer as? CddIcpTextRenderer)?.text }
    }

    private fun afterLineEndCddInlays() = myFixture.editor.inlayModel
        .getAfterLineEndElementsInRange(0, myFixture.editor.document.textLength)
        .filter { it.renderer is CddIcpTextRenderer }

    private fun blockCddInlays() = myFixture.editor.inlayModel
        .getBlockElementsInRange(0, myFixture.editor.document.textLength)
        .filter { it.renderer is CddIcpTextRenderer }

    private fun sampleResult(): AnalysisResult {
        return AnalysisResult(
            file = "Sample.kt",
            classes = listOf(
                ClassAnalysis(
                    name = "Sample",
                    packageName = "",
                    lineRange = IntRangeSerializable(1, 4),
                    totalIcp = 3.0,
                    icpBreakdown = mapOf(
                        IcpType.CODE_BRANCH to listOf(
                            IcpInstance(
                                type = IcpType.CODE_BRANCH,
                                line = 2,
                                column = 5,
                                description = "if branch",
                                weight = 1.0
                            )
                        )
                    ),
                    methods = listOf(
                        MethodAnalysis(
                            name = "first",
                            className = "Sample",
                            lineRange = IntRangeSerializable(2, 2),
                            totalIcp = 1.0,
                            icpBreakdown = mapOf(
                                IcpType.CONDITION to listOf(
                                    IcpInstance(
                                        type = IcpType.CONDITION,
                                        line = 2,
                                        column = 12,
                                        description = "condition expression",
                                        weight = 1.0
                                    )
                                )
                            )
                        ),
                        MethodAnalysis(
                            name = "second",
                            className = "Sample",
                            lineRange = IntRangeSerializable(3, 3),
                            totalIcp = 2.0,
                            icpBreakdown = emptyMap()
                        )
                    ),
                    isOverLimit = false
                )
            ),
            totalIcp = 3.0
        )
    }
}
