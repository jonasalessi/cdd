package com.cdd.domain

import kotlinx.serialization.Serializable

/**
 * Project-wide aggregated analysis results.
 */
@Serializable
data class AggregatedAnalysis(
    val totalFiles: Int,
    val totalClasses: Int,
    val totalIcp: Double,
    val averageIcp: Double,
    val classesOverLimit: List<ClassAnalysis>,
    val icpDistribution: Map<IcpType, Int>,
    val largestClasses: List<ClassAnalysis>,
    val suggestions: List<String> = emptyList()
)
