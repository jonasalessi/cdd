package com.cdd.ui.settings.tools.cdd

import com.cdd.CddConstants
import com.cdd.core.config.CddConfig
import com.cdd.core.config.InternalCouplingConfig
import com.cdd.core.config.SlocConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class CddConfigAdapterTest {

    @Test
    fun `should map core config to settings model`() {
        val config = CddConfig(
            metrics = mapOf(
                CddConstants.LANGUAGE_JAVA to mapOf("src/main/.*" to mapOf(CddConstants.METRIC_CODE_BRANCH to 2.5)),
                CddConstants.LANGUAGE_KOTLIN to mapOf("src/test/.*" to mapOf(CddConstants.METRIC_CONDITION to 3.0))
            ),
            icpLimits = mapOf(
                CddConstants.LANGUAGE_JAVA to mapOf("src/main/.*" to 20.0),
                CddConstants.LANGUAGE_KOTLIN to mapOf("src/test/.*" to 18.0)
            ),
            internalCoupling = InternalCouplingConfig(autoDetect = false, packages = listOf("com.example")),
            include = listOf("src/main/**"),
            exclude = listOf("build/**"),
            sloc = SlocConfig(methodLimit = 42)
        )

        val model = CddConfigMapper.toSettingsModel(config)

        assertEquals(mapOf("src/main/.*" to 20), model.javaIcpLimits)
        assertEquals(mapOf("src/test/.*" to 18), model.kotlinIcpLimits)
        assertEquals(mapOf("src/main/.*" to mapOf(CddConstants.METRIC_CODE_BRANCH to 2.5)), model.javaMetrics)
        assertEquals(mapOf("src/test/.*" to mapOf(CddConstants.METRIC_CONDITION to 3.0)), model.kotlinMetrics)
        assertEquals(false, model.autoDetect)
        assertEquals(listOf("com.example"), model.packages)
        assertEquals(listOf("src/main/**"), model.include)
        assertEquals(listOf("build/**"), model.exclude)
        assertEquals(42, model.methodLimit)
    }

    @Test
    fun `should map settings model to core config`() {
        val model = CddSettingsModel(
            javaIcpLimits = mutableMapOf("src/main/.*" to 16),
            kotlinIcpLimits = mutableMapOf("src/test/.*" to 14),
            javaMetrics = mutableMapOf("src/main/.*" to mutableMapOf(CddConstants.METRIC_CODE_BRANCH to 1.5)),
            kotlinMetrics = mutableMapOf("src/test/.*" to mutableMapOf(CddConstants.METRIC_CONDITION to 2.5)),
            autoDetect = false,
            packages = mutableListOf("com.example"),
            include = mutableListOf("src/**"),
            exclude = mutableListOf("out/**"),
            methodLimit = 31
        )

        val config = CddConfigMapper.toCoreConfig(model)

        assertEquals(mapOf("src/main/.*" to 16.0), config.icpLimits[CddConstants.LANGUAGE_JAVA])
        assertEquals(mapOf("src/test/.*" to 14.0), config.icpLimits[CddConstants.LANGUAGE_KOTLIN])
        assertEquals(mapOf("src/main/.*" to mapOf(CddConstants.METRIC_CODE_BRANCH to 1.5)), config.metrics[CddConstants.LANGUAGE_JAVA])
        assertEquals(mapOf("src/test/.*" to mapOf(CddConstants.METRIC_CONDITION to 2.5)), config.metrics[CddConstants.LANGUAGE_KOTLIN])
        assertEquals(false, config.internalCoupling.autoDetect)
        assertEquals(listOf("com.example"), config.internalCoupling.packages)
        assertEquals(listOf("src/**"), config.include)
        assertEquals(listOf("out/**"), config.exclude)
        assertEquals(31, config.sloc.methodLimit)
    }

    @Test
    fun `should load yaml into immutable core config`() {
        val yaml = """
            icp-limits:
              java:
                src/main/.*: 15
            metrics:
              kotlin:
                src/test/.*:
                  condition: 2.0
            internal_coupling:
              auto_detect: false
              packages:
                - com.example
            include:
              - src/**
            exclude:
              - build/**
            sloc:
              methodLimit: 33
        """.trimIndent()

        val config = CddConfigYamlCodec.load(yaml)

        assertEquals(mapOf("src/main/.*" to 15.0), config.icpLimits[CddConstants.LANGUAGE_JAVA])
        assertEquals(mapOf("src/test/.*" to mapOf(CddConstants.METRIC_CONDITION to 2.0)), config.metrics[CddConstants.LANGUAGE_KOTLIN])
        assertEquals(false, config.internalCoupling.autoDetect)
        assertEquals(listOf("com.example"), config.internalCoupling.packages)
        assertEquals(listOf("src/**"), config.include)
        assertEquals(listOf("build/**"), config.exclude)
        assertEquals(33, config.sloc.methodLimit)
    }

    @Test
    fun `should round trip config through yaml`() {
        val config = CddConfig(
            metrics = mapOf(
                CddConstants.LANGUAGE_JAVA to mapOf(
                    ".*" to mapOf(CddConstants.METRIC_CODE_BRANCH to 1.0, CddConstants.METRIC_CONDITION to 2.0)
                )
            ),
            icpLimits = mapOf(CddConstants.LANGUAGE_JAVA to mapOf(".*" to 12.0)),
            internalCoupling = InternalCouplingConfig(autoDetect = true, packages = listOf("com.example")),
            include = listOf("src/**"),
            exclude = listOf("build/**"),
            sloc = SlocConfig(methodLimit = 28)
        )

        val yaml = CddConfigYamlCodec.dump(config)
        val reloaded = CddConfigYamlCodec.load(yaml)

        assertEquals(config, reloaded)
    }
}
