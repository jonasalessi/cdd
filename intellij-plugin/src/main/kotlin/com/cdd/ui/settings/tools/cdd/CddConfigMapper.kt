package com.cdd.ui.settings.tools.cdd

import com.cdd.CddConstants
import com.cdd.core.config.CddConfig
import com.cdd.core.config.InternalCouplingConfig
import com.cdd.core.config.SlocConfig

object CddConfigMapper {

    fun toSettingsModel(config: CddConfig): CddSettingsModel {
        return CddSettingsModel(
            javaIcpLimits = config.icpLimits[CddConstants.LANGUAGE_JAVA].toRuleMap(),
            kotlinIcpLimits = config.icpLimits[CddConstants.LANGUAGE_KOTLIN].toRuleMap(),
            javaMetrics = config.metrics[CddConstants.LANGUAGE_JAVA].toMetricsMap(),
            kotlinMetrics = config.metrics[CddConstants.LANGUAGE_KOTLIN].toMetricsMap(),
            autoDetect = config.internalCoupling.autoDetect,
            packages = config.internalCoupling.packages.toMutableList(),
            include = config.include.toMutableList(),
            exclude = config.exclude.toMutableList(),
            methodLimit = config.sloc.methodLimit
        )
    }

    fun toCoreConfig(model: CddSettingsModel): CddConfig {
        return CddConfig(
            metrics = mapOf(
                CddConstants.LANGUAGE_JAVA to model.javaMetrics.toImmutableMetrics(),
                CddConstants.LANGUAGE_KOTLIN to model.kotlinMetrics.toImmutableMetrics()
            ),
            icpLimits = mapOf(
                CddConstants.LANGUAGE_JAVA to model.javaIcpLimits.mapValues { it.value.toDouble() },
                CddConstants.LANGUAGE_KOTLIN to model.kotlinIcpLimits.mapValues { it.value.toDouble() }
            ),
            internalCoupling = InternalCouplingConfig(
                autoDetect = model.autoDetect,
                packages = model.packages.toList()
            ),
            include = model.include.toList(),
            exclude = model.exclude.toList(),
            sloc = SlocConfig(methodLimit = model.methodLimit)
        )
    }

    private fun Map<String, Double>?.toRuleMap(): MutableMap<String, Int> {
        return this
            ?.mapValues { (_, value) -> value.toInt() }
            ?.toMutableMap()
            ?: mutableMapOf()
    }

    private fun Map<String, Map<String, Double>>?.toMetricsMap(): MutableMap<String, MutableMap<String, Double>> {
        return this
            ?.mapValues { (_, weights) -> weights.toMutableMap() }
            ?.toMutableMap()
            ?: mutableMapOf()
    }

    private fun Map<String, MutableMap<String, Double>>.toImmutableMetrics(): Map<String, Map<String, Double>> {
        return mapValues { (_, weights) -> weights.toMap() }
    }
}
