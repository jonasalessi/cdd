package com.cdd.analyzer.kotlin

import com.cdd.core.config.CddConfig
import com.cdd.core.config.InternalCouplingConfig
import com.cdd.domain.IcpType
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.kotlin.psi.KtFile
import java.io.File

class IntellijKotlinAnalyzerTest : BasePlatformTestCase() {
    private val config = CddConfig(
        metrics = mapOf("kotlin" to mapOf(".*" to mapOf("code_branch" to 1.0))),
        icpLimits = mapOf("kotlin" to mapOf(".*" to 10.0)),
        internalCoupling = InternalCouplingConfig(autoDetect = true, packages = listOf("com.example", "com.challenge"))
    )

    fun testShouldReportKotlinLanguageName() {
        val analyzer = IntellijKotlinAnalyzer(project)

        assertEquals("Kotlin", analyzer.languageName)
    }

    fun testShouldReportKtAndKtsSupportedExtensions() {
        val analyzer = IntellijKotlinAnalyzer(project)

        assertEquals(listOf("kt", "kts"), analyzer.supportedExtensions)
    }

    fun testShouldReturnErrorWhenFileCannotResolveToProjectPsi() {
        val analyzer = IntellijKotlinAnalyzer(project)
        val file = File(project.basePath, "missing-file.kt")

        val result = analyzer.analyze(file, config)
        val error = result.errors.single()

        assertTrue(result.classes.isEmpty())
        assertEquals(0.0, result.totalIcp)
        assertEquals(1, result.errors.size)
        assertEquals(file.absolutePath, error.file)
    }

    fun testShouldReturnErrorWhenFileIsNotKotlin() {
        val virtualFile = myFixture.tempDirFixture.createFile("Sample.txt", "plain text")
        val analyzer = IntellijKotlinAnalyzer(project)

        val result = analyzer.analyze(File(virtualFile.path), config)
        val error = result.errors.single()

        assertTrue(result.classes.isEmpty())
        assertEquals(0.0, result.totalIcp)
        assertEquals(1, result.errors.size)
        assertEquals(File(virtualFile.path).absolutePath, error.file)
    }

    fun testShouldResolveProjectKotlinPsiWithoutErrors() {
        val kotlinFile = myFixture.configureByText("Sample.kt", "package demo\nclass Sample") as KtFile
        val analyzer = IntellijKotlinAnalyzer(project) { _, _ -> kotlinFile }
        val sourceFile = File("Sample.kt")

        val result = analyzer.analyze(sourceFile, config)

        assertEquals(sourceFile.absolutePath, result.file)
        assertTrue(result.errors.isEmpty())
        assertEquals(1, result.classes.size)
        assertEquals("Sample", result.classes.single().name)
        assertEquals(0.0, result.totalIcp)
    }

    fun testShouldAnalyzeClassAndMethodMetadataFromSampleConstructs() {
        val result = analyzeFixture("kotlin-samples/SampleConstructs.kt")

        assertTrue(result.errors.isEmpty())
        val classAnalysis = result.classes.single { it.name == "SampleConstructs" }
        assertEquals("SampleConstructs", classAnalysis.name)
        assertEquals("com.example", classAnalysis.packageName)
        assertEquals(listOf("testWhen", "testElvis", "testConditions"), classAnalysis.methods.map { it.name })
    }

    fun testShouldDetectControlFlowCounts() {
        val result = analyzeFixture("kotlin-samples/SampleControlFlow.kt")
        val classAnalysis = result.classes.single { it.name == "SampleControlFlow" }

        assertEquals(5, classAnalysis.icpBreakdown[IcpType.CODE_BRANCH]?.size)
        assertEquals(4, classAnalysis.icpBreakdown[IcpType.CONDITION]?.size)
    }

    fun testShouldDetectExceptionHandlingCounts() {
        val result = analyzeFixture("kotlin-samples/SampleExceptions.kt")
        val classAnalysis = result.classes.single { it.name == "SampleExceptions" }

        assertEquals(3, classAnalysis.icpBreakdown[IcpType.EXCEPTION_HANDLING]?.size)
    }

    fun testShouldDetectInternalCouplingCounts() {
        val result = analyzeFixture("kotlin-samples/com/examples/SampleCoupling.kt")
        val classAnalysis = result.classes.single { it.name == "SampleCoupling" }

        assertEquals(3, classAnalysis.icpBreakdown[IcpType.INTERNAL_COUPLING]?.size)
    }

    fun testShouldComputeNonZeroSlocMetrics() {
        val result = analyzeFixture("kotlin-samples/SampleConstructs.kt")
        val classAnalysis = result.classes.single { it.name == "SampleConstructs" }

        assertTrue(classAnalysis.sloc.total > 0)
        assertTrue(classAnalysis.sloc.codeOnly > 0)
        assertTrue(classAnalysis.methods.all { it.sloc.total > 0 })
    }

    fun testShouldStripInlineAndBlockCommentsLikeCliAnalyzer() {
        val analyzer = IntellijKotlinAnalyzer(project)

        assertEquals("val x = 1", analyzer.stripComments("val x = 1 // inline"))
        assertEquals("val y =  2", analyzer.stripComments("val y = /* block */ 2"))
        assertEquals("// comment only", analyzer.stripComments("// comment only"))
    }

    fun testShouldCountSafeCallAsBranchButNotCondition() {
        val result = analyzeFixture("kotlin-samples/SafeCallTest.kt")
        val classAnalysis = result.classes.single { it.name == "SafeCallTest" }

        assertEquals(2, classAnalysis.icpBreakdown[IcpType.CODE_BRANCH]?.size)
        assertEquals(1, classAnalysis.icpBreakdown[IcpType.CONDITION]?.size)
    }

    fun testShouldPreserveWildcardAndExplicitImportCouplingHeuristics() {
        val result = analyzeFixture("kotlin-samples/com/examples/SampleAnnotationCoupling.kt")
        val classAnalysis = result.classes.single { it.name == "Category" }

        assertEquals(1.0, classAnalysis.icpBreakdown[IcpType.INTERNAL_COUPLING]?.sumOf { it.weight })
    }

    fun testShouldKeepNestedMethodsOnTheirOwningClassOnly() {
        val result = analyzeFixture("kotlin-samples/NestedOwnershipSample.kt")
        val outerClass = result.classes.single { it.name == "OuterClass" }
        val innerClass = result.classes.single { it.name == "InnerClass" }

        assertEquals(listOf("outerMethod"), outerClass.methods.map { it.name })
        assertEquals(listOf("innerMethod"), innerClass.methods.map { it.name })
    }

    private fun analyzeFixture(relativePath: String): com.cdd.domain.AnalysisResult {
        val analyzer = IntellijKotlinAnalyzer(project)
        val sourceFile = copyFixtureToProject(relativePath)

        return analyzer.analyze(sourceFile, config)
    }

    private fun copyFixtureToProject(relativePath: String): File {
        val fixtureStream = javaClass.classLoader.getResourceAsStream(relativePath)
            ?: error("Missing fixture: $relativePath")
        val targetFile = File(project.basePath, relativePath)
        targetFile.parentFile?.mkdirs()
        fixtureStream.use { input ->
            targetFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return targetFile
    }
}
