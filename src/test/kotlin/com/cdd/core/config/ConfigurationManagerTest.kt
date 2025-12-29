package com.cdd.core.config

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import java.io.File
import java.nio.file.Files

class ConfigurationManagerTest : DescribeSpec({
    val tempDir = Files.createTempDirectory("cdd-test").toFile()

    afterSpec {
        tempDir.deleteRecursively()
    }

    describe("ConfigurationManager") {
        it("should return default config when no file exists") {
            val config = ConfigurationManager.loadConfig(tempDir)
            config.limit shouldBe 10
            config.sloc.methodLimit shouldBe 24
        }

        it("should load valid YAML configuration") {
            val yamlContent = """
                limit: 15
                sloc:
                  methodLimit: 30
            """.trimIndent()
            val yamlFile = File(tempDir, ".cdd.yml")
            yamlFile.writeText(yamlContent)

            val config = ConfigurationManager.loadConfig(tempDir)
            config.limit shouldBe 15
            config.sloc.methodLimit shouldBe 30
            
            yamlFile.delete()
        }

        it("should load valid YAML configuration from .cdd.yaml") {
            val yamlContent = """
                limit: 20
                sloc:
                  methodLimit: 40
            """.trimIndent()
            val yamlFile = File(tempDir, ".cdd.yaml")
            yamlFile.writeText(yamlContent)

            val config = ConfigurationManager.loadConfig(tempDir)
            config.limit shouldBe 20.0
            config.sloc.methodLimit shouldBe 40
            
            yamlFile.delete()
        }

        it("should prefer .cdd.yml over .cdd.yaml if both exist") {
            val ymlContent = "limit: 15"
            val yamlContent = "limit: 20"
            
            val ymlFile = File(tempDir, ".cdd.yml")
            val yamlFile = File(tempDir, ".cdd.yaml")
            
            ymlFile.writeText(ymlContent)
            yamlFile.writeText(yamlContent)

            val config = ConfigurationManager.loadConfig(tempDir)
            config.limit shouldBe 15.0
            
            ymlFile.delete()
            yamlFile.delete()
        }

        it("should fallback to defaults if parsing fails") {
            val invalidYaml = "invalid: : yaml"
            val yamlFile = File(tempDir, ".cdd.yml")
            yamlFile.writeText(invalidYaml)

            val config = ConfigurationManager.loadConfig(tempDir)
            config.limit shouldBe 10 // default
            
            yamlFile.delete()
        }

        it("should fallback to defaults if validation fails") {
            val invalidLimit = "limit: -5"
            val yamlFile = File(tempDir, ".cdd.yml")
            yamlFile.writeText(invalidLimit)

            val config = ConfigurationManager.loadConfig(tempDir)
            config.limit shouldBe 10 // default
            
            yamlFile.delete()
        }
    }
})
