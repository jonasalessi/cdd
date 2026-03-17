package com.cdd.core.aggregator

import com.cdd.core.config.CddConfig
import com.cdd.domain.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.doubles.shouldBeExactly
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

class IcpAggregatorTest : FunSpec({
    val aggregator = IcpAggregator()
    val config = CddConfig(
        metrics = mapOf("java" to mapOf(".*" to mapOf("code_branch" to 1.0))),
        icpLimits = mapOf("java" to mapOf(".*" to 10.0))
    )

    fun makeIcpInstances(type: IcpType, count: Int): List<IcpInstance> =
        (1..count).map { IcpInstance(type, it, 0, "desc", 1.0) }

    fun createClass(
        name: String,
        icp: Double,
        sloc: Int,
        isOverLimit: Boolean = icp > 10.0,
        icpBreakdown: Map<IcpType, List<IcpInstance>> = emptyMap()
    ): ClassAnalysis {
        return ClassAnalysis(
            name = name,
            packageName = "com.example",
            lineRange = IntRangeSerializable(1, sloc),
            totalIcp = icp,
            icpBreakdown = icpBreakdown,
            methods = emptyList(),
            isOverLimit = isOverLimit,
            sloc = SlocMetrics(sloc, sloc, sloc, 0, 0)
        )
    }

    test("should aggregate across multiple files correctly") {
        val results = listOf(
            AnalysisResult(
                file = "File1.java",
                classes = listOf(createClass("Class1", 5.0, 50)),
                totalIcp = 5.0
            ),
            AnalysisResult(
                file = "File2.java",
                classes = listOf(createClass("Class2", 15.0, 150)),
                totalIcp = 15.0
            )
        )

        val aggregated = aggregator.aggregate(results, config)

        aggregated.totalFiles shouldBe 2
        aggregated.totalClasses shouldBe 2
        aggregated.totalIcp shouldBe 20.0
        aggregated.averageIcp shouldBe 10.0
        aggregated.classesOverLimit shouldHaveSize 1
        aggregated.classesOverLimit[0].name shouldBe "Class2"
    }

    test("should compute correct SLOC stats") {
        val classes = listOf(
            createClass("C1", 5.0, 40),
            createClass("C2", 10.0, 60),
            createClass("C3", 15.0, 80)
        )
        val results = listOf(AnalysisResult("F1.java", classes, 30.0))

        val aggregated = aggregator.aggregate(results, config)
        val stats = aggregated.slocMetrics

        stats.totalSloc shouldBe 180
        stats.averageSlocPerClass shouldBe 60.0
        stats.slocStdDev shouldBeGreaterThan 0.0
        // Variance: ((40-60)^2 + (60-60)^2 + (80-60)^2) / 3 = (400 + 0 + 400) / 3 = 800/3 = 266.66
        // StdDev = sqrt(266.66) approx 16.33
        stats.slocStdDev shouldBe (kotlin.math.sqrt(800.0 / 3.0))
    }

    test("should calculate correct ICP-SLOC correlation") {
        // Positive correlation: ICP and SLOC grow together
        val positiveResults = listOf(
            AnalysisResult(
                "F1.java", listOf(
                    createClass("C1", 2.0, 20),
                    createClass("C2", 4.0, 40),
                    createClass("C3", 6.0, 60),
                    createClass("C4", 8.0, 80),
                    createClass("C5", 10.0, 100)
                ), 30.0
            )
        )

        val posAggregated = aggregator.aggregate(positiveResults, config)
        posAggregated.icpSlocCorrelation shouldBeExactly 1.0

        // No correlation
        val noCorrResults = listOf(
            AnalysisResult(
                "F1.java", listOf(
                    createClass("C1", 2.0, 100),
                    createClass("C2", 10.0, 20)
                ), 12.0
            )
        )
        val noAggregated = aggregator.aggregate(noCorrResults, config)
        noAggregated.icpSlocCorrelation shouldBeExactly -1.0 // Inversely perfectly correlated here
    }

    test("should handle empty results") {
        val aggregated = aggregator.aggregate(emptyList(), config)

        aggregated.totalFiles shouldBe 0
        aggregated.totalClasses shouldBe 0
        aggregated.totalIcp shouldBe 0.0
        aggregated.averageIcp shouldBe 0.0
        aggregated.slocMetrics.totalSloc shouldBe 0
    }

    test("should generate appropriate suggestions") {
        val results = listOf(
            AnalysisResult(
                "F1.java", listOf(
                    createClass("HeavyClass", 20.0, 500)
                ), 20.0
            )
        )

        val aggregated = aggregator.aggregate(results, config)
        aggregated.suggestions shouldHaveSize 2
        aggregated.suggestions[0] shouldBe "Refactor the 1 classes that exceed the defined metric limits."
        aggregated.suggestions[1] shouldBe "Prioritize 'HeavyClass' as it has the highest complexity (20.0 ICP)."
    }

    test("should suggest no refactoring when all classes are within limits") {
        val results = listOf(
            AnalysisResult(
                "F1.java", listOf(
                    createClass("SmallClass", 5.0, 100, isOverLimit = false)
                ), 5.0
            )
        )

        val aggregated = aggregator.aggregate(results, config)
        aggregated.suggestions.any { it.contains("No classes exceed") } shouldBe true
    }

    test("should suggest smaller methods when correlation is high") {
        val results = listOf(
            AnalysisResult(
                "F1.java", listOf(
                    createClass("C1", 2.0, 20),
                    createClass("C2", 4.0, 40),
                    createClass("C3", 6.0, 60),
                    createClass("C4", 8.0, 80),
                    createClass("C5", 12.0, 120) // One over limit
                ), 32.0
            )
        )

        val aggregated = aggregator.aggregate(results, config)
        aggregated.suggestions.any { it.contains("Strong correlation") } shouldBe true
    }

    test("should include class name in method suggestions") {
        val method = MethodAnalysis(
            name = "complexMethod",
            className = "MyClass",
            lineRange = IntRangeSerializable(10, 50),
            totalIcp = 15.0, // Over limit (10.0)
            icpBreakdown = emptyMap(),
            sloc = SlocMetrics(100, 80, 90, 10, 10),
            isOverSlocLimit = true
        )
        val clazz = ClassAnalysis(
            name = "MyClass",
            packageName = "com.example",
            lineRange = IntRangeSerializable(1, 100),
            totalIcp = 15.0,
            icpBreakdown = emptyMap(),
            methods = listOf(method),
            isOverLimit = true,
            sloc = SlocMetrics(100, 80, 90, 10, 10)
        )
        val results = listOf(AnalysisResult("MyClass.kt", listOf(clazz), 15.0))

        val aggregated = aggregator.aggregate(results, config)

        // Should contain suggestion for method SLOC limit and ICP limit
        aggregated.suggestions.any { it.contains("MyClass.complexMethod") } shouldBe true
        aggregated.suggestions.any { it.contains("Consider extracting logic from 'MyClass.complexMethod'") } shouldBe true
    }

    test("should return zero correlation when all classes have identical ICP and SLOC") {
        // When all values are equal, variance is 0, denominator is 0 → correlation must be 0.0
        val results = listOf(
            AnalysisResult(
                "F1.java", listOf(
                    createClass("C1", 5.0, 50),
                    createClass("C2", 5.0, 50)
                ), 10.0
            )
        )

        val aggregated = aggregator.aggregate(results, config)
        aggregated.icpSlocCorrelation shouldBeExactly 0.0
    }

    test("should suggest centralized error handling when exception ICP exceeds 20 percent") {
        // Total ICP = 10.0; 3 exception instances → exceptionIcp / totalIcp = 3/10 = 30% > 20%
        val breakdown = mapOf(
            IcpType.EXCEPTION_HANDLING to makeIcpInstances(IcpType.EXCEPTION_HANDLING, 3)
        )
        val results = listOf(
            AnalysisResult(
                "F1.java", listOf(
                    createClass("A", 10.0, 100, isOverLimit = false, icpBreakdown = breakdown)
                ), 10.0
            )
        )

        val aggregated = aggregator.aggregate(results, config)
        aggregated.suggestions.any { it.contains("Exception handling accounts for") } shouldBe true
    }

    test("should suggest decoupling when coupling ICP exceeds 40 percent") {
        // Total ICP = 10.0; 5 coupling instances → couplingIcp / totalIcp = 5/10 = 50% > 40%
        val breakdown = mapOf(
            IcpType.INTERNAL_COUPLING to makeIcpInstances(IcpType.INTERNAL_COUPLING, 5)
        )
        val results = listOf(
            AnalysisResult(
                "F1.java", listOf(
                    createClass("B", 10.0, 100, isOverLimit = false, icpBreakdown = breakdown)
                ), 10.0
            )
        )

        val aggregated = aggregator.aggregate(results, config)
        aggregated.suggestions.any { it.contains("Coupling accounts for a large portion") } shouldBe true
    }

    test("should warn about brain methods when correlation is low and a class is over limit") {
        // Low correlation: one small class has very high ICP (brain method), others are large+low ICP
        val results = listOf(
            AnalysisResult(
                "F1.java", listOf(
                    createClass("C1", 20.0, 10, isOverLimit = true),
                    createClass("C2", 1.0, 200, isOverLimit = false),
                    createClass("C3", 1.0, 200, isOverLimit = false),
                    createClass("C4", 1.0, 200, isOverLimit = false),
                    createClass("C5", 1.0, 200, isOverLimit = false)
                ), 24.0
            )
        )

        val aggregated = aggregator.aggregate(results, config)
        aggregated.suggestions.any { it.contains("brain methods") } shouldBe true
    }

    test("should compute ICP distribution including null breakdown entries") {
        // icpBreakdown only has CODE_BRANCH — missing IcpType keys hit the Elvis ?: 0 branch
        val breakdown = mapOf(
            IcpType.CODE_BRANCH to makeIcpInstances(IcpType.CODE_BRANCH, 2)
        )
        val results = listOf(
            AnalysisResult(
                "F1.java", listOf(
                    createClass("C1", 5.0, 50, isOverLimit = false, icpBreakdown = breakdown)
                ), 5.0
            )
        )

        val aggregated = aggregator.aggregate(results, config)
        aggregated.icpDistribution[IcpType.CODE_BRANCH] shouldBe 2
        aggregated.icpDistribution[IcpType.EXCEPTION_HANDLING] shouldBe 0
        aggregated.icpDistribution[IcpType.INTERNAL_COUPLING] shouldBe 0
    }

    test("should not suggest exception or coupling refactoring when ICP shares are below thresholds") {
        // exception = 1/10 = 10% (<20%) and coupling = 3/10 = 30% (<40%) — neither threshold is exceeded
        val breakdown = mapOf(
            IcpType.EXCEPTION_HANDLING to makeIcpInstances(IcpType.EXCEPTION_HANDLING, 1),
            IcpType.INTERNAL_COUPLING to makeIcpInstances(IcpType.INTERNAL_COUPLING, 3)
        )
        val results = listOf(
            AnalysisResult(
                "F1.java", listOf(
                    createClass("C1", 10.0, 100, isOverLimit = false, icpBreakdown = breakdown)
                ), 10.0
            )
        )

        val aggregated = aggregator.aggregate(results, config)
        aggregated.suggestions.none { it.contains("Exception handling") } shouldBe true
        aggregated.suggestions.none { it.contains("Coupling accounts") } shouldBe true
    }

    test("should not warn about brain methods when low correlation classes are all within limits") {
        // correlation < 0.3, ≥5 classes, but none over limit → brain-method suggestion should NOT appear
        val results = listOf(
            AnalysisResult(
                "F1.java", listOf(
                    createClass("C1", 9.0, 10, isOverLimit = false),
                    createClass("C2", 1.0, 200, isOverLimit = false),
                    createClass("C3", 1.0, 200, isOverLimit = false),
                    createClass("C4", 1.0, 200, isOverLimit = false),
                    createClass("C5", 1.0, 200, isOverLimit = false)
                ), 13.0
            )
        )

        val aggregated = aggregator.aggregate(results, config)
        aggregated.suggestions.none { it.contains("brain methods") } shouldBe true
    }
})
