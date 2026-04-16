package com.cdd.ui.inlay

import com.cdd.domain.ClassAnalysis
import com.cdd.domain.IcpInstance
import com.cdd.domain.IcpType
import com.cdd.domain.IntRangeSerializable
import com.cdd.domain.MethodAnalysis
import com.cdd.domain.SlocMetrics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CddIcpReportFactoryTest {
    @Test
    fun shouldCreateClassReportFromAnalysis() {
        val report = AnalysisCddIcpReportFactory.createClassReport(sampleClassAnalysis())

        assertEquals("CDD Report: Sample", report.title)
        assertTrue(report.message.contains("Type: Class"))
        assertTrue(report.message.contains("Package: <default>"))
        assertTrue(report.message.contains("- CODE_BRANCH: 1"))
    }

    @Test
    fun shouldCreateMethodReportFromAnalysis() {
        val report = AnalysisCddIcpReportFactory.createMethodReport(sampleMethodAnalysis())

        assertEquals("CDD Report: Sample.first", report.title)
        assertTrue(report.message.contains("Type: Method"))
        assertTrue(report.message.contains("Class: Sample"))
        assertTrue(report.message.contains("Over SLOC Limit: false"))
    }

    @Test
    fun shouldRenderEmptyBreakdownWhenAnalysisHasNoInstances() {
        val report = AnalysisCddIcpReportFactory.createMethodReport(
            sampleMethodAnalysis().copy(icpBreakdown = emptyMap())
        )

        assertTrue(report.message.contains("Breakdown:\n- none"))
    }

    private fun sampleClassAnalysis(): ClassAnalysis {
        return ClassAnalysis(
            name = "Sample",
            packageName = "",
            lineRange = IntRangeSerializable(1, 4),
            totalIcp = 3.0,
            icpBreakdown = mapOf(
                IcpType.CODE_BRANCH to listOf(
                    IcpInstance(
                        type = IcpType.CODE_BRANCH,
                        line = 2,
                        column = 5,
                        description = "if branch",
                        weight = 1.0
                    )
                )
            ),
            methods = listOf(sampleMethodAnalysis()),
            isOverLimit = false,
            sloc = SlocMetrics(4, 3, 3, 0, 1)
        )
    }

    private fun sampleMethodAnalysis(): MethodAnalysis {
        return MethodAnalysis(
            name = "first",
            className = "Sample",
            lineRange = IntRangeSerializable(2, 2),
            totalIcp = 1.0,
            icpBreakdown = mapOf(
                IcpType.CONDITION to listOf(
                    IcpInstance(
                        type = IcpType.CONDITION,
                        line = 2,
                        column = 12,
                        description = "condition expression",
                        weight = 1.0
                    )
                )
            ),
            sloc = SlocMetrics(1, 1, 1, 0, 0)
        )
    }
}
