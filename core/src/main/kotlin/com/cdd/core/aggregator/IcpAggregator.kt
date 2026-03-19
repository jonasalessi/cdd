package com.cdd.core.aggregator

import com.cdd.core.config.CddConfig
import com.cdd.domain.*
import kotlin.math.sqrt

/**
 * Aggregates results from multiple language analyzers and computes global statistics.
 */
class IcpAggregator {

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

        val suggestionContext = SuggestionContext(
            classes = classes,
            methods = methods,
            distribution = distribution,
            correlation = correlation,
            config = config
        )

        addOverLimitSuggestions(suggestionContext)
        addIcpTypeSuggestions(suggestionContext)
        addCorrelationSuggestions(suggestionContext)
        addMethodSuggestions(suggestionContext)

        return suggestionContext.suggestions
    }

    private fun addOverLimitSuggestions(context: SuggestionContext) {
        val classesOverLimit = context.classes.filter { it.isOverLimit }
        if (classesOverLimit.isEmpty()) {
            context.suggestions.add("No classes exceed the complexity limits. Good job!")
            return
        }
        context.suggestions.add("Refactor the ${classesOverLimit.size} classes that exceed the defined metric limits.")
        val worstClass = classesOverLimit.maxByOrNull { it.totalIcp }
        if (worstClass != null) {
            context.suggestions.add(
                "Prioritize '${worstClass.name}' as it has the highest complexity (${
                    String.format("%.1f", worstClass.totalIcp)
                } ICP)."
            )
        }
    }

    private fun addIcpTypeSuggestions(context: SuggestionContext) {
        val totalIcp = context.classes.sumOf { it.totalIcp }
        if (totalIcp <= 0) return

        val exceptionShare = (context.distribution[IcpType.EXCEPTION_HANDLING] ?: 0).toDouble() / totalIcp
        if (exceptionShare > EXCEPTION_ICP_THRESHOLD) {
            context.suggestions.add(
                "Exception handling accounts for >20% of total complexity. " +
                "Consider a more centralized error handling strategy or using functional error handling."
            )
        }

        val couplingShare = (context.distribution[IcpType.INTERNAL_COUPLING] ?: 0).toDouble() / totalIcp
        if (couplingShare > COUPLING_ICP_THRESHOLD) {
            context.suggestions.add(
                "Coupling accounts for a large portion of complexity. " +
                "Consider extracting high-coupling logic into specialized services or using interfaces to decouple components."
            )
        }
    }

    private fun addCorrelationSuggestions(context: SuggestionContext) {
        if (context.classes.size < MIN_CLASSES_FOR_CORRELATION) return

        if (context.correlation > HIGH_CORRELATION_THRESHOLD) {
            context.suggestions.add(
                "Strong correlation (${context.correlation}) between SLOC and ICP detected. " +
                "Making methods smaller (extracting methods) will likely reduce cognitive complexity directly."
            )
            return
        }

        val hasLowCorrelationWithOverLimit =
            context.correlation < LOW_CORRELATION_THRESHOLD && context.classes.any { it.isOverLimit }
        if (hasLowCorrelationWithOverLimit) {
            context.suggestions.add(
                "Low correlation (${context.correlation}) between SLOC and ICP. " +
                "Some classes are complex despite being small. Watch out for 'brain methods' with high density of logic."
            )
        }
    }

    private fun addMethodSuggestions(context: SuggestionContext) {
        val overSloc = context.methods.filter { it.isOverSlocLimit }
        if (overSloc.isEmpty()) return

        context.suggestions.add(
            "${overSloc.size} methods exceed the ${context.config.sloc.methodLimit} SLOC threshold. Breaking these down is highly recommended."
        )
        overSloc.take(MAX_METHOD_SUGGESTIONS).forEach { method ->
            context.suggestions.add(
                "  - Consider extracting logic from '${method.className}.${method.name}' (${method.sloc.total} SLOC)."
            )
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

    private data class SuggestionContext(
        val classes: List<ClassAnalysis>,
        val methods: List<MethodAnalysis>,
        val distribution: Map<IcpType, Int>,
        val correlation: Double,
        val config: CddConfig,
        val suggestions: MutableList<String> = mutableListOf()
    )

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
}
