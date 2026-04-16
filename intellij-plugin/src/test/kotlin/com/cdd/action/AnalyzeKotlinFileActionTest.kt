package com.cdd.action

import com.cdd.analysis.KotlinFileAnalysisRunner
import com.cdd.domain.AnalysisResult
import com.cdd.domain.ClassAnalysis
import com.cdd.domain.IntRangeSerializable
import com.cdd.domain.MethodAnalysis
import com.cdd.domain.SlocMetrics
import com.cdd.ui.inlay.CddIcpInlayService
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class AnalyzeKotlinFileActionTest : BasePlatformTestCase() {
    fun testShouldRenderInlaysForManualAnalyzeAction() {
        val virtualFile = myFixture.tempDirFixture.createFile("Sample.kt", "class Sample")
        myFixture.openFileInEditor(virtualFile)
        val captured = mutableListOf<AnalysisResult>()
        val action = AnalyzeKotlinFileAction(testRunner(captured))

        action.actionPerformed(createEvent(virtualFile))

        assertEquals(1, captured.size)
        assertEquals("Sample", captured.single().classes.single().name)
    }

    fun testShouldOnlyEnableActionForKotlinFiles() {
        val javaFile = myFixture.tempDirFixture.createFile("Sample.java", "class Sample {}")
        myFixture.openFileInEditor(javaFile)
        val action = AnalyzeKotlinFileAction(testRunner(mutableListOf()))
        val event = createEvent(javaFile)

        action.update(event)

        assertFalse(event.presentation.isEnabled)
        assertFalse(event.presentation.isVisible)
    }

    private fun createEvent(file: VirtualFile): AnActionEvent {
        val dataContext = SimpleDataContext.builder()
            .add(CommonDataKeys.PROJECT, project)
            .add(CommonDataKeys.EDITOR, myFixture.editor)
            .add(CommonDataKeys.VIRTUAL_FILE, file)
            .build()
        return AnActionEvent.createFromDataContext(ActionPlaces.EDITOR_POPUP, Presentation(), dataContext)
    }

    private fun sampleResult(): AnalysisResult {
        return AnalysisResult(
            file = "Sample.kt",
            classes = listOf(
                ClassAnalysis(
                    name = "Sample",
                    packageName = "",
                    lineRange = IntRangeSerializable(1, 1),
                    totalIcp = 2.0,
                    icpBreakdown = emptyMap(),
                    methods = listOf(
                        MethodAnalysis(
                            name = "first",
                            className = "Sample",
                            lineRange = IntRangeSerializable(1, 1),
                            totalIcp = 1.0,
                            icpBreakdown = emptyMap(),
                            sloc = SlocMetrics(1, 1, 1, 0, 0)
                        )
                    ),
                    isOverLimit = false,
                    sloc = SlocMetrics(1, 1, 1, 0, 0)
                )
            ),
            totalIcp = 2.0
        )
    }

    private fun testRunner(captured: MutableList<AnalysisResult>): KotlinFileAnalysisRunner {
        return KotlinFileAnalysisRunner(
            analyzeFile = { _, _ -> sampleResult() },
            inlayService = capturingInlayService(captured)
        )
    }

    private fun capturingInlayService(captured: MutableList<AnalysisResult>): CddIcpInlayService {
        return object : CddIcpInlayService {
            override fun render(project: Project, file: VirtualFile, result: AnalysisResult) {
                captured.add(result)
            }
        }
    }
}
