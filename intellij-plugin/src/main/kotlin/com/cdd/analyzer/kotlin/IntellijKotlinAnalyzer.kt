package com.cdd.analyzer.kotlin

import com.cdd.analyzer.AbstractLanguageAnalyzer
import com.cdd.core.config.CddConfig
import com.cdd.core.util.CommentUtils
import com.cdd.domain.AnalysisError
import com.cdd.domain.AnalysisResult
import com.cdd.domain.ClassAnalysis
import com.cdd.domain.ErrorSeverity
import com.cdd.domain.IntRangeSerializable
import com.cdd.domain.MethodAnalysis
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import java.io.File

internal class IntellijKotlinAnalyzer(
    private val project: Project,
    private val kotlinPsiResolver: KotlinPsiResolver = ProjectKotlinPsiResolver
) : AbstractLanguageAnalyzer() {

    override val supportedExtensions: List<String> = listOf("kt", "kts")

    override val languageName: String = "Kotlin"

    override fun analyze(file: File, config: CddConfig): AnalysisResult {
        return try {
            val ktFile = kotlinPsiResolver.resolve(project, file)
            if (ktFile == null) {
                return unresolvedResult(file)
            }

            val content = ktFile.text
            val classes = mutableListOf<ClassAnalysis>()
            ktFile.accept(object : KtTreeVisitorVoid() {
                override fun visitClass(klass: KtClass) {
                    super.visitClass(klass)
                    classes.add(analyzeClass(klass, ktFile, content, file, config))
                }
            })

            AnalysisResult(
                file = file.absolutePath,
                classes = classes,
                totalIcp = classes.sumOf { it.totalIcp },
                errors = emptyList()
            )
        } catch (exception: Exception) {
            failedResult(file, exception.message ?: "Failed to analyze Kotlin PSI")
        }
    }

    override fun stripComments(line: String): String {
        val stripLine = { sourceLine: String ->
            CommentUtils.stripLineComment(CommentUtils.stripBlockComments(sourceLine))
        }

        return if (CommentUtils.hasCode(line, stripLine)) {
            stripLine(line).trimEnd()
        } else {
            line
        }
    }

    private fun unresolvedResult(file: File): AnalysisResult {
        return failedResult(file, "Unable to resolve Kotlin PSI for ${file.absolutePath}")
    }

    private fun analyzeClass(
        ktClass: KtClass,
        ktFile: KtFile,
        fullContent: String,
        file: File,
        config: CddConfig
    ): ClassAnalysis {
        val weights = resolveWeights(file, config)
        val scanner = IntellijKotlinIcpScanner(
            fullContent = fullContent,
            config = config,
            currentKtFile = ktFile,
            weights = weights,
            analyzedClassName = ktClass.fqName?.asString()
        )
        ktClass.accept(scanner)

        val classIcpInstances = scanner.getIcpInstances()
        val lineRange = createLineRange(fullContent, ktClass.startOffset, ktClass.textRange.endOffset)
        val methods = analyzeMethods(ktClass, classIcpInstances, fullContent)
        val totalIcp = classIcpInstances.sumOf { it.weight }
        val classLimit = resolveIcpLimit(file, config) ?: Double.MAX_VALUE

        return ClassAnalysis(
            name = ktClass.name ?: "Unknown",
            packageName = ktFile.packageFqName.asString(),
            lineRange = lineRange,
            totalIcp = totalIcp,
            icpBreakdown = classIcpInstances.groupBy { it.type },
            methods = methods,
            isOverLimit = totalIcp > classLimit
        )
    }

    private fun analyzeMethods(
        ktClass: KtClass,
        classIcpInstances: List<com.cdd.domain.IcpInstance>,
        fullContent: String
    ): List<MethodAnalysis> {
        val methods = mutableListOf<MethodAnalysis>()
        ktClass.accept(object : KtTreeVisitorVoid() {
            override fun visitNamedFunction(function: KtNamedFunction) {
                super.visitNamedFunction(function)
                if (!isDirectClassMethod(function, ktClass)) {
                    return
                }

                val lineRange = createLineRange(fullContent, function.startOffset, function.textRange.endOffset)
                val methodIcpInstances = classIcpInstances.filter { instance ->
                    instance.line in lineRange.start..lineRange.endInclusive
                }
                methods.add(
                    MethodAnalysis(
                        name = function.name ?: "Unknown",
                        className = ktClass.name ?: "Unknown",
                        lineRange = lineRange,
                        totalIcp = methodIcpInstances.sumOf { it.weight },
                        icpBreakdown = methodIcpInstances.groupBy { it.type }
                    )
                )
            }
        })
        return methods
    }

    private fun isDirectClassMethod(function: KtNamedFunction, ktClass: KtClass): Boolean {
        return function.parent == ktClass ||
            (function.parent is KtClassBody && function.parent.parent == ktClass)
    }

    private fun createLineRange(content: String, startOffset: Int, endOffset: Int): IntRangeSerializable {
        val startLine = getLineNumber(content, startOffset)
        val endLine = getLineNumber(content, endOffset)
        return IntRangeSerializable(startLine, endLine)
    }

    private fun getLineNumber(content: String, offset: Int): Int {
        val safeOffset = offset.coerceIn(0, content.length)
        return content.substring(0, safeOffset).count { it == '\n' } + 1
    }

    private fun failedResult(file: File, message: String): AnalysisResult {
        return AnalysisResult(
            file = file.absolutePath,
            classes = emptyList(),
            totalIcp = 0.0,
            errors = listOf(
                AnalysisError(
                    file = file.absolutePath,
                    message = message,
                    severity = ErrorSeverity.ERROR
                )
            )
        )
    }
}
