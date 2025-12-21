package com.cdd.cli

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class CddCliTest : StringSpec({
    "cdd-cli should parse limit correctly" {
        val cli = CddCli()
        cli.parse(listOf("src/test/resources/empty-folder", "--limit", "15"))
        cli.limit shouldBe 15
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
        cli.parse(listOf("src/test/resources/empty-folder", "--method-level", "--fail-on-violations"))
        cli.methodLevel shouldBe true
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
})
