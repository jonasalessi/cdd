package com.cdd.analyzer.kotlin

import com.cdd.analyzer.AbstractLanguageAnalyzer
import com.cdd.core.config.CddConfig
import com.cdd.core.util.CommentUtils
import com.cdd.domain.*
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import java.io.File

class KotlinAnalyzer : AbstractLanguageAnalyzer() {
    override val supportedExtensions: List<String> = listOf("kt", "kts")
    override val languageName: String = "Kotlin"

    override fun analyze(file: File, config: CddConfig): AnalysisResult {
        return try {
            val disposable = Disposer.newDisposable()
            val configuration = CompilerConfiguration()
            configuration.put(
                CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY,
                PrintingMessageCollector(System.err, MessageRenderer.PLAIN_RELATIVE_PATHS, false)
            )

            val environment = KotlinCoreEnvironment.createForProduction(
                disposable,
                configuration,
                EnvironmentConfigFiles.JVM_CONFIG_FILES
            )

            val content = file.readText()
            val ktFile = KtPsiFactory(environment.project).createFile(content)

            val classes = mutableListOf<ClassAnalysis>()
            ktFile.accept(object : KtTreeVisitorVoid() {
                override fun visitClass(klass: KtClass) {
                    super.visitClass(klass)
                    classes.add(analyzeClass(klass, content, file, config))
                }
            })

            Disposer.dispose(disposable)

            AnalysisResult(
                file = file.absolutePath,
                classes = classes,
                totalIcp = classes.sumOf { it.totalIcp }
            )
        } catch (e: Exception) {
            e.printStackTrace()
            AnalysisResult(
                file = file.absolutePath,
                classes = emptyList(),
                totalIcp = 0.0,
                errors = listOf(
                    AnalysisError(
                        file.absolutePath,
                        message = e.message ?: "Unknown error",
                        severity = ErrorSeverity.ERROR
                    )
                )
            )
        }
    }

    private fun analyzeClass(ktClass: KtClass, fullContent: String, file: File, config: CddConfig): ClassAnalysis {
        val ktFile = ktClass.containingFile as KtFile
        val weights = resolveWeights(file, config)
        val scanner = KotlinIcpScanner(fullContent, config, ktFile, weights, ktClass.fqName?.asString())
        ktClass.accept(scanner)

        val classIcpInstances = scanner.getIcpInstances()
        val totalIcp = classIcpInstances.sumOf { it.weight }
        val classIcpBreakdown = classIcpInstances.groupBy { it.type }

        val startLine = getLineNumber(fullContent, ktClass.startOffset)
        val endLine = getLineNumber(fullContent, ktClass.textRange.endOffset)
        val lineRange = (startLine..endLine).toSerializable()

        val classLimit = resolveIcpLimit(file, config) ?: Double.MAX_VALUE
        val overLimit = totalIcp > classLimit

        val methods = mutableListOf<MethodAnalysis>()
        ktClass.accept(object : KtTreeVisitorVoid() {
            override fun visitNamedFunction(function: KtNamedFunction) {
                if (function.parent == ktClass || function.parent is KtClassBody && function.parent.parent == ktClass) {
                    val methodStart = getLineNumber(fullContent, function.startOffset)
                    val methodEnd = getLineNumber(fullContent, function.textRange.endOffset)
                    val methodRange = methodStart..methodEnd

                    val methodIcpInstances = classIcpInstances.filter { it.line in methodRange }
                    val methodBreakdown = methodIcpInstances.groupBy { it.type }

                    methods.add(
                        MethodAnalysis(
                            name = function.name ?: "Unknown",
                            className = ktClass.name ?: "Unknown",
                            lineRange = methodRange.toSerializable(),
                            totalIcp = methodIcpInstances.sumOf { it.weight },
                            icpBreakdown = methodBreakdown
                        )
                    )
                }
            }
        })

        return ClassAnalysis(
            name = ktClass.name ?: "Unknown",
            packageName = (ktClass.containingFile as? KtFile)?.packageFqName?.asString() ?: "",
            lineRange = lineRange,
            totalIcp = totalIcp,
            icpBreakdown = classIcpBreakdown,
            methods = methods,
            isOverLimit = overLimit
        )
    }

    private fun getLineNumber(content: String, offset: Int): Int {
        if (offset < 0) return 1
        val safeOffset = offset.coerceAtMost(content.length)
        return content.substring(0, safeOffset).count { it == '\n' } + 1
    }

    override fun stripComments(line: String): String {
        val stripFn = { l: String ->
            CommentUtils.stripLineComment(
                CommentUtils.stripBlockComments(l)
            )
        }
        return if (CommentUtils.hasCode(line, stripFn)) {
            stripFn(line).trimEnd()
        } else {
            line
        }
    }
}
