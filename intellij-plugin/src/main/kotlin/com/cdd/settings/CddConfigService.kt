package com.cdd.settings

import com.cdd.core.config.CddConfig
import com.intellij.openapi.project.Project
import java.io.File

class CddConfigService(private val project: Project) {

    fun loadConfig(): CddConfig {
        val file = getConfigFile() ?: return CddConfig.DEFAULT
        if (!file.exists()) return CddConfig.DEFAULT
        return CddConfigYamlCodec.load(file.readText())
    }

    fun saveConfig(config: CddConfig) {
        val file = getConfigFile() ?: return
        file.parentFile?.mkdirs()
        file.writeText(CddConfigYamlCodec.dump(config))
    }

    companion object {
        fun getInstance(project: Project): CddConfigService {
            return project.getService(CddConfigService::class.java)
        }
    }

    private fun getConfigFile(): File? {
        val basePath = project.basePath ?: return null
        return File(File(basePath, ".cdd"), "cdd.yaml")
    }
}
