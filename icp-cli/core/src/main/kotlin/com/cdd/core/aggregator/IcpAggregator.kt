package com.cdd.core.aggregator

import com.cdd.core.config.CddConfig
import com.cdd.domain.*

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

        val suggestions = generateSuggestions(allClassAnalyses, allMethodAnalyses, icpDistribution, config)

        return AggregatedAnalysis(
            totalFiles = results.size,
            totalClasses = totalClasses,
            totalIcp = totalIcp,
            averageIcp = averageIcp,
            classesOverLimit = classesOverLimit,
            icpDistribution = icpDistribution,
            largestClasses = largestClasses,
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
        config: CddConfig
    ): List<String> {
        if (classes.isEmpty()) return emptyList()

        val suggestionContext = SuggestionContext(
            classes = classes,
            methods = methods,
            distribution = distribution,
            config = config
        )

        addOverLimitSuggestions(suggestionContext)
        addIcpTypeSuggestions(suggestionContext)

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

    private data class SuggestionContext(
        val classes: List<ClassAnalysis>,
        val methods: List<MethodAnalysis>,
        val distribution: Map<IcpType, Int>,
        val config: CddConfig,
        val suggestions: MutableList<String> = mutableListOf()
    )

    companion object {
        private const val MAX_LARGEST_CLASSES = 10
        private const val EXCEPTION_ICP_THRESHOLD = 0.2
        private const val COUPLING_ICP_THRESHOLD = 0.4
    }
}
