package com.cdd.analyzer.java

import com.cdd.analyzer.AbstractLanguageAnalyzer
import com.cdd.core.config.CddConfig
import com.cdd.domain.*
import org.slf4j.LoggerFactory
import spoon.Launcher
import spoon.reflect.declaration.CtClass
import java.io.File

class JavaAnalyzer : AbstractLanguageAnalyzer() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override val supportedExtensions: List<String> = listOf("java")
    override val languageName: String = "Java"

    override fun analyze(file: File, config: CddConfig): AnalysisResult {
        return try {
            val launcher = createLauncher(file)
            launcher.buildModel()
            val model = launcher.model

            val classes = mutableListOf<ClassAnalysis>()

            model.allTypes.filterIsInstance<CtClass<*>>().forEach { ctClass ->
                classes.add(analyzeClass(ctClass, config))
            }

            AnalysisResult(
                file = file.absolutePath,
                classes = classes,
                totalIcp = classes.sumOf { it.totalIcp }
            )
        } catch (e: Exception) {
            logger.error("Error analyzing ${file.name}: ${e.message}", e)
            AnalysisResult(
                file = file.absolutePath,
                classes = emptyList(),
                totalIcp = 0.0,
                errors = listOf(AnalysisError(file.absolutePath, null, e.message ?: "Unknown error", ErrorSeverity.ERROR))
            )
        }
    }

    private fun createLauncher(file: File): Launcher {
        val launcher = Launcher()
        launcher.environment.complianceLevel = 21
        launcher.environment.noClasspath = true
        launcher.environment.setCommentEnabled(true)
        launcher.addInputResource(file.absolutePath)
        return launcher
    }

    private fun analyzeClass(ctClass: CtClass<*>, config: CddConfig): ClassAnalysis {
        val file = ctClass.position?.file ?: File("unknown")
        val weights = resolveWeights(file, config)
        val scanner = JavaCtScanner(config, weights)
        ctClass.accept(scanner)

        val classIcpInstances = scanner.icpInstances.values.flatten()
        val classIcpBreakdown = classIcpInstances.groupBy { it.type }

        val totalIcp = classIcpInstances.sumOf { it.weight }
        val classLimit = resolveIcpLimit(file, config) ?: Double.MAX_VALUE
        val overLimit = totalIcp > classLimit

        val methods = ctClass.methods.map { ctMethod ->
            val methodRange = ctMethod.position.line..ctMethod.position.endLine
            val methodIcpInstances = classIcpInstances.filter { it.line in methodRange }
            val methodIcpBreakdown = methodIcpInstances.groupBy { it.type }

            MethodAnalysis(
                name = ctMethod.simpleName,
                className = ctClass.simpleName,
                lineRange = methodRange.toSerializable(),
                totalIcp = methodIcpInstances.sumOf { it.weight },
                icpBreakdown = methodIcpBreakdown
            )
        }

        val lineRange = ctClass.position.line..ctClass.position.endLine

        return ClassAnalysis(
            name = ctClass.simpleName,
            packageName = ctClass.`package`?.qualifiedName ?: "",
            lineRange = lineRange.toSerializable(),
            totalIcp = totalIcp,
            icpBreakdown = classIcpBreakdown,
            methods = methods,
            isOverLimit = overLimit
        )
    }

    override fun stripComments(line: String): String {
        val stripFn = { l: String ->
            com.cdd.core.util.CommentUtils.stripLineComment(
                com.cdd.core.util.CommentUtils.stripBlockComments(l)
            )
        }
        return if (com.cdd.core.util.CommentUtils.hasCode(line, stripFn)) {
            stripFn(line).trimEnd()
        } else {
            line
        }
    }
}
