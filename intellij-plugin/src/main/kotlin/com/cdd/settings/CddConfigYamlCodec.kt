package com.cdd.settings

import com.cdd.core.config.CddConfig
import com.cdd.core.config.InternalCouplingConfig
import com.cdd.core.config.SlocConfig
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml

object CddConfigYamlCodec {
    private const val ICP_LIMITS_KEY = "icp-limits"
    private const val METRICS_KEY = "metrics"
    private const val INTERNAL_COUPLING_KEY = "internal_coupling"
    private const val AUTO_DETECT_KEY = "auto_detect"
    private const val PACKAGES_KEY = "packages"
    private const val INCLUDE_KEY = "include"
    private const val EXCLUDE_KEY = "exclude"
    private const val SLOC_KEY = "sloc"
    private const val METHOD_LIMIT_KEY = "methodLimit"

    fun load(content: String): CddConfig {
        return try {
            val yaml = Yaml()
            val data = yaml.load<Map<String, Any?>>(content) ?: return CddConfig.DEFAULT
            parseConfig(data)
        } catch (_: Exception) {
            CddConfig.DEFAULT
        }
    }

    fun dump(config: CddConfig): String {
        val options = DumperOptions().apply {
            defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
            isPrettyFlow = true
        }

        return Yaml(options).dump(
            mapOf(
                ICP_LIMITS_KEY to config.icpLimits,
                METRICS_KEY to config.metrics,
                INTERNAL_COUPLING_KEY to mapOf(
                    AUTO_DETECT_KEY to config.internalCoupling.autoDetect,
                    PACKAGES_KEY to config.internalCoupling.packages
                ),
                INCLUDE_KEY to config.include,
                EXCLUDE_KEY to config.exclude,
                SLOC_KEY to mapOf(METHOD_LIMIT_KEY to config.sloc.methodLimit)
            )
        )
    }

    private fun parseConfig(data: Map<String, Any?>): CddConfig {
        return CddConfig(
            metrics = parseMetrics(data[METRICS_KEY]),
            icpLimits = parseIcpLimits(data[ICP_LIMITS_KEY]),
            internalCoupling = parseInternalCoupling(data[INTERNAL_COUPLING_KEY]),
            include = parseStringList(data[INCLUDE_KEY]),
            exclude = parseStringList(data[EXCLUDE_KEY]),
            sloc = parseSloc(data[SLOC_KEY])
        )
    }

    private fun parseIcpLimits(value: Any?): Map<String, Map<String, Double>> {
        val languageMap = value as? Map<*, *> ?: return emptyMap()
        val limits = mutableMapOf<String, Map<String, Double>>()
        languageMap.forEach { (language, patterns) ->
            val languageKey = language as? String ?: return@forEach
            val patternMap = patterns as? Map<*, *> ?: return@forEach
            val parsedPatterns = mutableMapOf<String, Double>()
            patternMap.forEach { (pattern, limit) ->
                val patternKey = pattern as? String ?: return@forEach
                val number = (limit as? Number)?.toDouble() ?: return@forEach
                parsedPatterns[patternKey] = number
            }
            limits[languageKey] = parsedPatterns
        }
        return limits
    }

    private fun parseMetrics(value: Any?): Map<String, Map<String, Map<String, Double>>> {
        val languageMap = value as? Map<*, *> ?: return emptyMap()
        val metrics = mutableMapOf<String, Map<String, Map<String, Double>>>()
        languageMap.forEach { (language, patterns) ->
            val languageKey = language as? String ?: return@forEach
            val patternMap = patterns as? Map<*, *> ?: return@forEach
            val parsedPatterns = mutableMapOf<String, Map<String, Double>>()
            patternMap.forEach { (pattern, metricValues) ->
                val patternKey = pattern as? String ?: return@forEach
                val metricsMap = metricValues as? Map<*, *> ?: return@forEach
                val parsedMetrics = mutableMapOf<String, Double>()
                metricsMap.forEach { (metric, weight) ->
                    val metricKey = metric as? String ?: return@forEach
                    val number = (weight as? Number)?.toDouble() ?: return@forEach
                    parsedMetrics[metricKey] = number
                }
                parsedPatterns[patternKey] = parsedMetrics
            }
            metrics[languageKey] = parsedPatterns
        }
        return metrics
    }

    private fun parseInternalCoupling(value: Any?): InternalCouplingConfig {
        val map = value as? Map<*, *> ?: return InternalCouplingConfig()
        val autoDetect = map[AUTO_DETECT_KEY] as? Boolean ?: true
        val packages = parseStringList(map[PACKAGES_KEY])
        return InternalCouplingConfig(autoDetect = autoDetect, packages = packages)
    }

    private fun parseSloc(value: Any?): SlocConfig {
        val map = value as? Map<*, *> ?: return SlocConfig()
        val methodLimit = (map[METHOD_LIMIT_KEY] as? Number)?.toInt() ?: SlocConfig().methodLimit
        return SlocConfig(methodLimit = methodLimit)
    }

    private fun parseStringList(value: Any?): List<String> {
        return (value as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
    }
}
