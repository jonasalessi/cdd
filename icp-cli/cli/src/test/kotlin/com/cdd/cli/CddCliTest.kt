package com.cdd.cli

import com.cdd.core.registry.AnalyzerRegistry
import com.github.ajalt.clikt.core.parse
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.io.File

class CddCliTest : StringSpec({
    beforeTest {
        AnalyzerRegistry.clear()
    }

    afterTest {
        AnalyzerRegistry.clear()
    }

    "cdd-cli should parse format correctly" {
        val cli = CddCli()
        cli.parse(listOf("src/test/resources/empty-folder", "--format", "json"))
        cli.format shouldBe com.cdd.core.config.OutputFormat.JSON
    }

    "cdd-cli should parse include patterns correctly" {
        val cli = CddCli()
        cli.parse(listOf("src/test/resources/empty-folder", "--include", "src/**/*.java", "--include", "*.kt"))
        cli.include shouldBe listOf("src/**/*.java", "*.kt")
    }

    "cdd-cli should parse flags correctly" {
        val cli = CddCli()
        cli.parse(listOf("src/test/resources/empty-folder", "--fail-on-violations"))
        cli.failOnViolations shouldBe true
    }

    "cdd-cli should parse config path correctly" {
        val tempFile = java.io.File.createTempFile("test-config", ".yml")
        try {
            val cli = CddCli()
            cli.parse(listOf("src/test/resources/empty-folder", "--config", tempFile.path))
            cli.configPath?.path shouldBe tempFile.path
        } finally {
            tempFile.delete()
        }
    }

    "cdd-cli should register Java and Kotlin analyzers" {
        CddCli()

        AnalyzerRegistry.getAnalyzerFor(File("Example.java"))?.languageName shouldBe "Java"
        AnalyzerRegistry.getAnalyzerFor(File("Example.kt"))?.languageName shouldBe "Kotlin"
        AnalyzerRegistry.getAnalyzerFor(File("Example.kts")) shouldNotBe null
    }
})
