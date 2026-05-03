package com.cdd.core.aggregator

import com.cdd.core.config.CddConfig
import com.cdd.domain.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
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
        isOverLimit: Boolean = icp > 10.0,
        icpBreakdown: Map<IcpType, List<IcpInstance>> = emptyMap()
    ): ClassAnalysis {
        return ClassAnalysis(
            name = name,
            packageName = "com.example",
            lineRange = IntRangeSerializable(1, 10),
            totalIcp = icp,
            icpBreakdown = icpBreakdown,
            methods = emptyList(),
            isOverLimit = isOverLimit
        )
    }

    test("should aggregate across multiple files correctly") {
        val results = listOf(
            AnalysisResult(
                file = "File1.java",
                classes = listOf(createClass("Class1", 5.0)),
                totalIcp = 5.0
            ),
            AnalysisResult(
                file = "File2.java",
                classes = listOf(createClass("Class2", 15.0)),
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

    test("should handle empty results") {
        val aggregated = aggregator.aggregate(emptyList(), config)

        aggregated.totalFiles shouldBe 0
        aggregated.totalClasses shouldBe 0
        aggregated.totalIcp shouldBe 0.0
        aggregated.averageIcp shouldBe 0.0
    }

    test("should generate appropriate suggestions") {
        val results = listOf(
            AnalysisResult(
                "F1.java", listOf(
                    createClass("HeavyClass", 20.0)
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
                    createClass("SmallClass", 5.0, isOverLimit = false)
                ), 5.0
            )
        )

        val aggregated = aggregator.aggregate(results, config)
        aggregated.suggestions.any { it.contains("No classes exceed") } shouldBe true
    }

    test("should suggest centralized error handling when exception ICP exceeds 20 percent") {
        val breakdown = mapOf(
            IcpType.EXCEPTION_HANDLING to makeIcpInstances(IcpType.EXCEPTION_HANDLING, 3)
        )
        val results = listOf(
            AnalysisResult(
                "F1.java", listOf(
                    createClass("A", 10.0, isOverLimit = false, icpBreakdown = breakdown)
                ), 10.0
            )
        )

        val aggregated = aggregator.aggregate(results, config)
        aggregated.suggestions.any { it.contains("Exception handling accounts for") } shouldBe true
    }

    test("should suggest decoupling when coupling ICP exceeds 40 percent") {
        val breakdown = mapOf(
            IcpType.INTERNAL_COUPLING to makeIcpInstances(IcpType.INTERNAL_COUPLING, 5)
        )
        val results = listOf(
            AnalysisResult(
                "F1.java", listOf(
                    createClass("B", 10.0, isOverLimit = false, icpBreakdown = breakdown)
                ), 10.0
            )
        )

        val aggregated = aggregator.aggregate(results, config)
        aggregated.suggestions.any { it.contains("Coupling accounts for a large portion") } shouldBe true
    }

    test("should compute ICP distribution including null breakdown entries") {
        val breakdown = mapOf(
            IcpType.CODE_BRANCH to makeIcpInstances(IcpType.CODE_BRANCH, 2)
        )
        val results = listOf(
            AnalysisResult(
                "F1.java", listOf(
                    createClass("C1", 5.0, isOverLimit = false, icpBreakdown = breakdown)
                ), 5.0
            )
        )

        val aggregated = aggregator.aggregate(results, config)
        aggregated.icpDistribution[IcpType.CODE_BRANCH] shouldBe 2
        aggregated.icpDistribution[IcpType.EXCEPTION_HANDLING] shouldBe 0
        aggregated.icpDistribution[IcpType.INTERNAL_COUPLING] shouldBe 0
    }

    test("should not suggest exception or coupling refactoring when ICP shares are below thresholds") {
        val breakdown = mapOf(
            IcpType.EXCEPTION_HANDLING to makeIcpInstances(IcpType.EXCEPTION_HANDLING, 1),
            IcpType.INTERNAL_COUPLING to makeIcpInstances(IcpType.INTERNAL_COUPLING, 3)
        )
        val results = listOf(
            AnalysisResult(
                "F1.java", listOf(
                    createClass("C1", 10.0, isOverLimit = false, icpBreakdown = breakdown)
                ), 10.0
            )
        )

        val aggregated = aggregator.aggregate(results, config)
        aggregated.suggestions.none { it.contains("Exception handling") } shouldBe true
        aggregated.suggestions.none { it.contains("Coupling accounts") } shouldBe true
    }

    test("should not suggest icp type refactoring when total icp is zero") {
        val breakdown = mapOf(
            IcpType.EXCEPTION_HANDLING to makeIcpInstances(IcpType.EXCEPTION_HANDLING, 2),
            IcpType.INTERNAL_COUPLING to makeIcpInstances(IcpType.INTERNAL_COUPLING, 5)
        )
        val results = listOf(
            AnalysisResult(
                "F1.java", listOf(
                    createClass("C1", 0.0, isOverLimit = false, icpBreakdown = breakdown)
                ), 0.0
            )
        )

        val aggregated = aggregator.aggregate(results, config)
        aggregated.suggestions.none { it.contains("Exception handling") } shouldBe true
        aggregated.suggestions.none { it.contains("Coupling accounts") } shouldBe true
    }

    test("should not suggest icp type refactoring at exact configured thresholds") {
        val breakdown = mapOf(
            IcpType.EXCEPTION_HANDLING to makeIcpInstances(IcpType.EXCEPTION_HANDLING, 2),
            IcpType.INTERNAL_COUPLING to makeIcpInstances(IcpType.INTERNAL_COUPLING, 4)
        )
        val results = listOf(
            AnalysisResult(
                "F1.java", listOf(
                    createClass("C1", 10.0, isOverLimit = false, icpBreakdown = breakdown)
                ), 10.0
            )
        )

        val aggregated = aggregator.aggregate(results, config)
        aggregated.suggestions.none { it.contains("Exception handling") } shouldBe true
        aggregated.suggestions.none { it.contains("Coupling accounts") } shouldBe true
    }
})
