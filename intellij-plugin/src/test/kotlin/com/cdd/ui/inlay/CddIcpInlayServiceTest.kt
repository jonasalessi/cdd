package com.cdd.ui.inlay

import com.cdd.domain.AnalysisError
import com.cdd.domain.AnalysisResult
import com.cdd.domain.ClassAnalysis
import com.cdd.domain.ErrorSeverity
import com.cdd.domain.IntRangeSerializable
import com.cdd.domain.MethodAnalysis
import com.cdd.domain.SlocMetrics
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class CddIcpInlayServiceTest : BasePlatformTestCase() {
    fun testShouldRenderClassAndMethodInlays() {
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
                "  Class ICP: 3 | SLOC: 3/4",
                "  ICP: 1 | SLOC: 1/1",
                "  ICP: 2 | SLOC: 1/1"
            ),
            cddInlayTexts()
        )
    }

    fun testShouldAttachDialogContentToRenderedInlays() {
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

    fun testShouldClearPreviousInlaysBeforeRendering() {
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

    fun testShouldClearInlaysWhenAnalysisHasErrors() {
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

    private fun cddInlayTexts(): List<String> {
        return myFixture.editor.inlayModel
            .getAfterLineEndElementsInRange(0, myFixture.editor.document.textLength)
            .mapNotNull { (it.renderer as? CddIcpTextRenderer)?.text }
    }

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
                        com.cdd.domain.IcpType.CODE_BRANCH to listOf(
                            com.cdd.domain.IcpInstance(
                                type = com.cdd.domain.IcpType.CODE_BRANCH,
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
                                com.cdd.domain.IcpType.CONDITION to listOf(
                                    com.cdd.domain.IcpInstance(
                                        type = com.cdd.domain.IcpType.CONDITION,
                                        line = 2,
                                        column = 12,
                                        description = "condition expression",
                                        weight = 1.0
                                    )
                                )
                            ),
                            sloc = SlocMetrics(1, 1, 1, 0, 0)
                        ),
                        MethodAnalysis(
                            name = "second",
                            className = "Sample",
                            lineRange = IntRangeSerializable(3, 3),
                            totalIcp = 2.0,
                            icpBreakdown = emptyMap(),
                            sloc = SlocMetrics(1, 1, 1, 0, 0)
                        )
                    ),
                    isOverLimit = false,
                    sloc = SlocMetrics(4, 3, 3, 0, 1)
                )
            ),
            totalIcp = 3.0
        )
    }
}
