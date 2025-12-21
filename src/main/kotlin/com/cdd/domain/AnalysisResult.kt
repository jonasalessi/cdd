package com.cdd.domain

import java.io.File
import kotlinx.serialization.Serializable

/**
 * Result of analyzing a single file.
 */
@Serializable
data class AnalysisResult(
    val file: String, // Use String path for easier serialization
    val classes: List<ClassAnalysis>,
    val totalIcp: Double,
    val errors: List<AnalysisError> = emptyList()
)

/**
 * Analysis data for a single class.
 */
@Serializable
data class ClassAnalysis(
    val name: String,
    val packageName: String,
    val lineRange: IntRangeSerializable,
    val totalIcp: Double,
    val icpBreakdown: Map<IcpType, List<IcpInstance>>,
    val methods: List<MethodAnalysis>,
    val isOverLimit: Boolean,
    val sloc: SlocMetrics
)

/**
 * Analysis data for a single method.
 */
@Serializable
data class MethodAnalysis(
    val name: String,
    val lineRange: IntRangeSerializable,
    val totalIcp: Double,
    val icpBreakdown: Map<IcpType, List<IcpInstance>>,
    val sloc: SlocMetrics,
    val isOverSlocLimit: Boolean = false
)

/**
 * SLOC (Source Lines of Code) metrics.
 */
@Serializable
data class SlocMetrics(
    val total: Int,
    val codeOnly: Int,
    val withComments: Int,
    val comments: Int,
    val blankLines: Int
)

/**
 * A single occurrence of an ICP.
 */
@Serializable
data class IcpInstance(
    val type: IcpType,
    val line: Int,
    val column: Int,
    val description: String,
    val weight: Double
)

/**
 * Error encountered during analysis.
 */
@Serializable
data class AnalysisError(
    val file: String,
    val line: Int? = null,
    val message: String,
    val severity: ErrorSeverity
)

@Serializable
enum class ErrorSeverity {
    WARNING, ERROR, CRITICAL
}

/**
 * Serializable version of IntRange because Kotlin's IntRange is not @Serializable by default.
 */
@Serializable
data class IntRangeSerializable(val start: Int, val endInclusive: Int)

fun IntRange.toSerializable() = IntRangeSerializable(this.first, this.last)
