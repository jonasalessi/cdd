package com.cdd.settings

import com.cdd.model.CddConfig
import com.cdd.model.InternalCouplingConfig
import com.cdd.model.SlocConfig
import com.intellij.openapi.project.Project
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.FileInputStream
import java.io.FileWriter

class CddConfigService(private val project: Project) {

    private val configDir by lazy { File(project.basePath, ".cdd") }
    private val configFile by lazy { File(configDir, "cdd.yaml") }

    fun loadConfig(): CddConfig {
        if (!configFile.exists()) {
            return CddConfig()
        }

        return try {
            val yaml = Yaml()
            val data = FileInputStream(configFile).use { stream ->
                yaml.load<Map<String, Any>>(stream)
            } ?: return CddConfig()

            val config = CddConfig()
            
            parseIcpLimits(data, config)
            parseMetrics(data, config)
            parseInternalCoupling(data, config)
            parseFiltering(data, config)
            parseSloc(data, config)

            config
        } catch (e: Exception) {
            e.printStackTrace()
            CddConfig()
        }
    }

    fun saveConfig(config: CddConfig) {
        if (!configDir.exists()) {
            configDir.mkdirs()
        }

        val options = DumperOptions()
        options.defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
        options.isPrettyFlow = true

        val yaml = Yaml(options)
        val data = mapOf(
            "icp-limits" to config.icpLimits,
            "metrics" to config.metrics,
            "internal_coupling" to mapOf(
                "auto_detect" to config.internalCoupling.autoDetect,
                "packages" to config.internalCoupling.packages
            ),
            "include" to config.include,
            "exclude" to config.exclude,
            "sloc" to mapOf(
                "methodLimit" to config.sloc.methodLimit
            )
        )

        FileWriter(configFile).use { writer ->
            yaml.dump(data, writer)
        }
    }

    private fun parseIcpLimits(data: Map<String, Any>, config: CddConfig) {
        val limits = (data["icp-limits"] as? Map<String, Map<String, Int>>)?.mapValues { 
            it.value.toMutableMap() 
        } ?: return

        limits.forEach { (language, patterns) ->
            config.icpLimits[language] = patterns
        }
    }

    private fun parseMetrics(data: Map<String, Any>, config: CddConfig) {
        val metrics = (data["metrics"] as? Map<String, Map<String, Map<String, Any>>>)?.mapValues {
            it.value.mapValues { inner -> 
                inner.value.mapValues { weightEntry ->
                    (weightEntry.value as? Number)?.toDouble() ?: 1.0
                }.toMutableMap()
            }.toMutableMap()
        } ?: return

        metrics.forEach { (language, patterns) ->
            config.metrics[language] = patterns
        }
    }

    private fun parseInternalCoupling(data: Map<String, Any>, config: CddConfig) {
        val map = data["internal_coupling"] as? Map<String, Any> ?: return
        
        (map["auto_detect"] as? Boolean)?.let { config.internalCoupling.autoDetect = it }
        (map["packages"] as? List<String>)?.let { config.internalCoupling.packages = it.toMutableList() }
    }

    private fun parseFiltering(data: Map<String, Any>, config: CddConfig) {
        (data["include"] as? List<String>)?.let { config.include = it.toMutableList() }
        (data["exclude"] as? List<String>)?.let { config.exclude = it.toMutableList() }
    }

    private fun parseSloc(data: Map<String, Any>, config: CddConfig) {
        val map = data["sloc"] as? Map<String, Any> ?: return
        (map["methodLimit"] as? Int)?.let { config.sloc.methodLimit = it }
    }

    companion object {
        fun getInstance(project: Project): CddConfigService {
            return project.getService(CddConfigService::class.java)
        }
    }
}
