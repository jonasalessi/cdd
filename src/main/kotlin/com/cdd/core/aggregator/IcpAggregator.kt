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

        val totalFiles = results.size
        val totalClasses = allClassAnalyses.size
        val totalIcp = allClassAnalyses.sumOf { it.totalIcp }
        val averageIcp = if (totalClasses > 0) totalIcp / totalClasses else 0.0

        val classesOverLimit = allClassAnalyses.filter { it.totalIcp > config.limit }
        val largestClasses = allClassAnalyses.sortedByDescending { it.totalIcp }.take(10)

        val icpDistribution = IcpType.entries.associateWith { type ->
            allClassAnalyses.sumOf { it.icpBreakdown[type]?.size ?: 0 }
        }

        val slocStatistics = computeSlocStatistics(allClassAnalyses, allMethodAnalyses)
        val correlation = computeIcpSlocCorrelation(allClassAnalyses)
        val methodsOverSlocLimit = allMethodAnalyses.filter { it.isOverSlocLimit }

        return AggregatedAnalysis(
            totalFiles = totalFiles,
            totalClasses = totalClasses,
            totalIcp = totalIcp,
            averageIcp = averageIcp,
            classesOverLimit = classesOverLimit,
            icpDistribution = icpDistribution.mapKeys { it.key },
            largestClasses = largestClasses,
            slocMetrics = slocStatistics,
            icpSlocCorrelation = correlation,
            methodsOverSlocLimit = methodsOverSlocLimit
        )
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
        val avgSlocPerMethod = if (methods.isNotEmpty()) methods.sumOf { it.sloc.total }.toDouble() / methods.size else 0.0

        val methodSlocs = methods.map { it.sloc.total }.sorted()
        val medianSlocPerMethod = if (methodSlocs.isNotEmpty()) {
            methodSlocs[methodSlocs.size / 2]
        } else 0

        val slocVariance = classes.sumOf { 
            val diff = it.sloc.total - avgSlocPerClass
            diff * diff 
        } / classes.size
        val slocStdDev = sqrt(slocVariance)

        val distribution = calculateSlocDistribution(classes)

        return SlocStatistics(
            totalSloc = totalSloc,
            averageSlocPerClass = avgSlocPerClass,
            averageSlocPerMethod = avgSlocPerMethod,
            medianSlocPerMethod = medianSlocPerMethod,
            slocStdDev = slocStdDev,
            slocDistribution = distribution
        )
    }

    private fun calculateSlocDistribution(classes: List<ClassAnalysis>): Map<Int, Int> {
        val bucketSize = 50
        return classes.groupBy { (it.sloc.total / bucketSize) * bucketSize }
            .mapValues { it.value.size }
            .toSortedMap()
    }

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
