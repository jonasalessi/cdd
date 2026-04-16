package com.cdd.listener

import com.cdd.analysis.KotlinFileAnalysisRunner
import com.cdd.domain.AnalysisResult
import com.cdd.domain.ClassAnalysis
import com.cdd.domain.IntRangeSerializable
import com.cdd.domain.MethodAnalysis
import com.cdd.domain.SlocMetrics
import com.cdd.settings.CDDSettingsService
import com.cdd.ui.inlay.CddIcpInlayService
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class IcpAutoCalculateOnSaveListenerTest : BasePlatformTestCase() {
    override fun tearDown() {
        CDDSettingsService.getInstance().setAutoCalculateOnSave(false)
        super.tearDown()
    }

    fun testShouldRenderInlaysAfterSaveCallbackCompletesWhenEnabled() {
        val virtualFile = myFixture.tempDirFixture.createFile(
            "Sample.kt",
            """
            class Sample {
                fun first() {}
            }
            """.trimIndent()
        )
        myFixture.openFileInEditor(virtualFile)
        val captured = mutableListOf<AnalysisResult>()
        var scheduledAnalysis: (() -> Unit)? = null
        val listener = IcpAutoCalculateOnSaveListener(
            analysisRunner = testRunner(captured),
            scheduleAnalysis = { action -> scheduledAnalysis = action }
        )
        CDDSettingsService.getInstance().setAutoCalculateOnSave(true)

        listener.beforeDocumentSaving(myFixture.editor.document)

        assertTrue(captured.isEmpty())
        scheduledAnalysis?.invoke()
        assertEquals(1, captured.size)
        assertEquals(1, captured.single().classes.size)
        assertEquals("Sample", captured.single().classes.single().name)
    }

    fun testShouldSkipRenderingWhenDisabled() {
        val virtualFile = myFixture.tempDirFixture.createFile("Sample.kt", "class Sample")
        myFixture.openFileInEditor(virtualFile)
        val captured = mutableListOf<AnalysisResult>()
        val listener = IcpAutoCalculateOnSaveListener(testRunner(captured))
        CDDSettingsService.getInstance().setAutoCalculateOnSave(false)

        listener.beforeDocumentSaving(myFixture.editor.document)

        assertTrue(captured.isEmpty())
    }

    fun testShouldSkipRenderingWhenDocumentIsNotBackedByAFile() {
        val document = EditorFactory.getInstance().createDocument("class Sample")
        val captured = mutableListOf<AnalysisResult>()
        val listener = IcpAutoCalculateOnSaveListener(testRunner(captured))
        CDDSettingsService.getInstance().setAutoCalculateOnSave(true)

        listener.beforeDocumentSaving(document)

        assertTrue(captured.isEmpty())
    }

    fun testShouldSkipRenderingWhenProjectCannotBeResolved() {
        val virtualFile = myFixture.tempDirFixture.createFile("Sample.kt", "class Sample")
        myFixture.openFileInEditor(virtualFile)
        val captured = mutableListOf<AnalysisResult>()
        val listener = IcpAutoCalculateOnSaveListener(
            analysisRunner = testRunner(captured),
            resolveProject = { null }
        )
        CDDSettingsService.getInstance().setAutoCalculateOnSave(true)

        listener.beforeDocumentSaving(myFixture.editor.document)

        assertTrue(captured.isEmpty())
    }

    fun testShouldSkipJavaFiles() {
        val virtualFile = myFixture.tempDirFixture.createFile("Sample.java", "class Sample {}")
        myFixture.openFileInEditor(virtualFile)
        val captured = mutableListOf<AnalysisResult>()
        val listener = IcpAutoCalculateOnSaveListener(testRunner(captured))
        CDDSettingsService.getInstance().setAutoCalculateOnSave(true)

        listener.beforeDocumentSaving(myFixture.editor.document)

        assertTrue(captured.isEmpty())
    }

    private fun sampleResult(): AnalysisResult {
        return AnalysisResult(
            file = "Sample.kt",
            classes = listOf(
                ClassAnalysis(
                    name = "Sample",
                    packageName = "",
                    lineRange = IntRangeSerializable(1, 3),
                    totalIcp = 2.0,
                    icpBreakdown = emptyMap(),
                    methods = listOf(
                        MethodAnalysis(
                            name = "first",
                            className = "Sample",
                            lineRange = IntRangeSerializable(2, 2),
                            totalIcp = 1.0,
                            icpBreakdown = emptyMap(),
                            sloc = SlocMetrics(1, 1, 1, 0, 0)
                        )
                    ),
                    isOverLimit = false,
                    sloc = SlocMetrics(3, 2, 2, 0, 1)
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
