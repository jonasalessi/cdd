package com.cdd.core.aggregator

import com.cdd.core.config.CddConfig
import com.cdd.domain.*
import kotlin.math.sqrt

/**
 * Aggregates results from multiple language analyzers and computes global statistics.
 */
class IcpAggregator {

    companion object {
        private const val MAX_LARGEST_CLASSES = 10
        private const val EXCEPTION_ICP_THRESHOLD = 0.2
        private const val COUPLING_ICP_THRESHOLD = 0.4
        private const val HIGH_CORRELATION_THRESHOLD = 0.8
        private const val LOW_CORRELATION_THRESHOLD = 0.3
        private const val MIN_CLASSES_FOR_CORRELATION = 5
        private const val SLOC_BUCKET_SIZE = 50
        private const val MAX_METHOD_SUGGESTIONS = 3
    }

    /**
     * Aggregates the given analysis results into a single project-wide report.
     */
    fun aggregate(results: List<AnalysisResult>, config: CddConfig): AggregatedAnalysis {
        val allClassAnalyses = results.flatMap { it.classes }
        val allMethodAnalyses = allClassAnalyses.flatMap { it.methods }

        val totalClasses = allClassAnalyses.size
        val totalIcp = allClassAnalyses.sumOf { it.totalIcp }
        val averageIcp = if (totalClasses > 0) totalIcp / totalClasses else 0.0

        val classesOverLimit = allClassAnalyses.filter { it.isOverLimit }
        val largestClasses = allClassAnalyses.sortedByDescending { it.totalIcp }.take(MAX_LARGEST_CLASSES)
        val icpDistribution = buildIcpDistribution(allClassAnalyses)

        val slocMetrics = computeSlocStatistics(allClassAnalyses, allMethodAnalyses)
        val correlation = computeIcpSlocCorrelation(allClassAnalyses)
        val methodsOverSlocLimit = allMethodAnalyses.filter { it.isOverSlocLimit }

        val suggestions = generateSuggestions(allClassAnalyses, allMethodAnalyses, icpDistribution, correlation, config)

        return AggregatedAnalysis(
            totalFiles = results.size,
            totalClasses = totalClasses,
            totalIcp = totalIcp,
            averageIcp = averageIcp,
            classesOverLimit = classesOverLimit,
            icpDistribution = icpDistribution,
            largestClasses = largestClasses,
            slocMetrics = slocMetrics,
            icpSlocCorrelation = correlation,
            methodsOverSlocLimit = methodsOverSlocLimit,
            suggestions = suggestions
        )
    }

    private fun buildIcpDistribution(classes: List<ClassAnalysis>): Map<IcpType, Int> =
        IcpType.entries.associateWith { type ->
            classes.sumOf { it.icpBreakdown[type]?.size ?: 0 }
        }

    private fun generateSuggestions(
        classes: List<ClassAnalysis>,
        methods: List<MethodAnalysis>,
        distribution: Map<IcpType, Int>,
        correlation: Double,
        config: CddConfig
    ): List<String> {
        if (classes.isEmpty()) return emptyList()

        val suggestions = mutableListOf<String>()
        val totalIcp = classes.sumOf { it.totalIcp }

        addOverLimitSuggestions(classes, suggestions)
        addIcpTypeSuggestions(totalIcp, distribution, suggestions)
        addCorrelationSuggestions(correlation, classes, suggestions)
        addMethodSuggestions(methods, config, suggestions)

        return suggestions
    }

    private fun addOverLimitSuggestions(classes: List<ClassAnalysis>, suggestions: MutableList<String>) {
        val classesOverLimit = classes.filter { it.isOverLimit }
        if (classesOverLimit.isEmpty()) {
            suggestions.add("No classes exceed the complexity limits. Good job!")
            return
        }
        suggestions.add("Refactor the ${classesOverLimit.size} classes that exceed the defined metric limits.")
        val worstClass = classesOverLimit.maxByOrNull { it.totalIcp }
        if (worstClass != null) {
            suggestions.add(
                "Prioritize '${worstClass.name}' as it has the highest complexity (${
                    String.format("%.1f", worstClass.totalIcp)
                } ICP)."
            )
        }
    }

    private fun addIcpTypeSuggestions(
        totalIcp: Double,
        distribution: Map<IcpType, Int>,
        suggestions: MutableList<String>
    ) {
        if (totalIcp <= 0) return

        val exceptionShare = (distribution[IcpType.EXCEPTION_HANDLING] ?: 0).toDouble() / totalIcp
        if (exceptionShare > EXCEPTION_ICP_THRESHOLD) {
            suggestions.add(
                "Exception handling accounts for >20% of total complexity. " +
                "Consider a more centralized error handling strategy or using functional error handling."
            )
        }

        val couplingShare = (distribution[IcpType.INTERNAL_COUPLING] ?: 0).toDouble() / totalIcp
        if (couplingShare > COUPLING_ICP_THRESHOLD) {
            suggestions.add(
                "Coupling accounts for a large portion of complexity. " +
                "Consider extracting high-coupling logic into specialized services or using interfaces to decouple components."
            )
        }
    }

    private fun addCorrelationSuggestions(
        correlation: Double,
        classes: List<ClassAnalysis>,
        suggestions: MutableList<String>
    ) {
        if (classes.size < MIN_CLASSES_FOR_CORRELATION) return

        if (correlation > HIGH_CORRELATION_THRESHOLD) {
            suggestions.add(
                "Strong correlation ($correlation) between SLOC and ICP detected. " +
                "Making methods smaller (extracting methods) will likely reduce cognitive complexity directly."
            )
            return
        }

        val hasLowCorrelationWithOverLimit = correlation < LOW_CORRELATION_THRESHOLD && classes.any { it.isOverLimit }
        if (hasLowCorrelationWithOverLimit) {
            suggestions.add(
                "Low correlation ($correlation) between SLOC and ICP. " +
                "Some classes are complex despite being small. Watch out for 'brain methods' with high density of logic."
            )
        }
    }

    private fun addMethodSuggestions(
        methods: List<MethodAnalysis>,
        config: CddConfig,
        suggestions: MutableList<String>
    ) {
        val overSloc = methods.filter { it.isOverSlocLimit }
        if (overSloc.isEmpty()) return

        suggestions.add("${overSloc.size} methods exceed the ${config.sloc.methodLimit} SLOC threshold. Breaking these down is highly recommended.")
        overSloc.take(MAX_METHOD_SUGGESTIONS).forEach { method ->
            suggestions.add("  - Consider extracting logic from '${method.className}.${method.name}' (${method.sloc.total} SLOC).")
        }
    }

    private fun computeSlocStatistics(
        classes: List<ClassAnalysis>,
        methods: List<MethodAnalysis>
    ): SlocStatistics {
        if (classes.isEmpty()) {
            return SlocStatistics(0, 0.0, 0.0, 0, 0.0, emptyMap())
        }

        val totalSloc = classes.sumOf { it.sloc.total }
        val avgSlocPerClass = totalSloc.toDouble() / classes.size
        val avgSlocPerMethod = computeAverageSlocPerMethod(methods)
        val medianSlocPerMethod = computeMedian(methods.map { it.sloc.total })
        val slocStdDev = computeStdDev(classes.map { it.sloc.total.toDouble() }, avgSlocPerClass)

        return SlocStatistics(
            totalSloc = totalSloc,
            averageSlocPerClass = avgSlocPerClass,
            averageSlocPerMethod = avgSlocPerMethod,
            medianSlocPerMethod = medianSlocPerMethod,
            slocStdDev = slocStdDev,
            slocDistribution = calculateSlocDistribution(classes)
        )
    }

    private fun computeAverageSlocPerMethod(methods: List<MethodAnalysis>): Double =
        if (methods.isNotEmpty()) methods.sumOf { it.sloc.codeOnly }.toDouble() / methods.size else 0.0

    private fun computeMedian(values: List<Int>): Int {
        val sorted = values.sorted()
        return if (sorted.isNotEmpty()) sorted[sorted.size / 2] else 0
    }

    private fun computeStdDev(values: List<Double>, mean: Double): Double {
        val variance = values.sumOf { value ->
            val diff = value - mean
            diff * diff
        } / values.size
        return sqrt(variance)
    }

    private fun calculateSlocDistribution(classes: List<ClassAnalysis>): Map<Int, Int> =
        classes.groupBy { (it.sloc.total / SLOC_BUCKET_SIZE) * SLOC_BUCKET_SIZE }
            .mapValues { it.value.size }
            .toSortedMap()

    private fun computeIcpSlocCorrelation(classes: List<ClassAnalysis>): Double {
        if (classes.size < 2) return 0.0

        val avgIcp = classes.map { it.totalIcp }.average()
        val avgSloc = classes.map { it.sloc.total.toDouble() }.average()

        var numerator = 0.0
        var icpSquareSum = 0.0
        var slocSquareSum = 0.0

        for (cls in classes) {
            val icpDiff = cls.totalIcp - avgIcp
            val slocDiff = cls.sloc.total.toDouble() - avgSloc
            numerator += icpDiff * slocDiff
            icpSquareSum += icpDiff * icpDiff
            slocSquareSum += slocDiff * slocDiff
        }

        val denominator = sqrt(icpSquareSum * slocSquareSum)
        return if (denominator != 0.0) numerator / denominator else 0.0
    }
}
