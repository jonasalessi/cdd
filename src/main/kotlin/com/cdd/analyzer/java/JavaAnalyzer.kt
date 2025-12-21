package com.cdd.analyzer.java

import com.cdd.analyzer.LanguageAnalyzer
import com.cdd.core.config.CddConfig
import com.cdd.core.config.ConfigurationManager
import com.cdd.domain.*
import org.slf4j.LoggerFactory
import spoon.Launcher
import spoon.reflect.CtModel
import spoon.reflect.declaration.CtClass
import spoon.reflect.declaration.CtMethod
import spoon.reflect.declaration.CtType
import spoon.reflect.declaration.CtElement
import spoon.reflect.visitor.CtScanner
import spoon.reflect.code.*
import spoon.reflect.reference.CtTypeReference
import spoon.reflect.reference.CtPackageReference
import java.io.File

class JavaAnalyzer : LanguageAnalyzer {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override val supportedExtensions: List<String> = listOf("java")
    override val languageName: String = "Java"

    override fun analyze(file: File, config: CddConfig): AnalysisResult {
        return try {
            val launcher = createLauncher(file)
            launcher.buildModel()
            val model = launcher.model
            
            val classes = mutableListOf<ClassAnalysis>()
            
            model.getAllTypes().filterIsInstance<CtClass<*>>().forEach { ctClass ->
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
        val scanner = IcpScanner(config)
        ctClass.accept(scanner)
        
        val classIcpInstances = scanner.icpInstances.values.flatten()
        
        val methods = ctClass.methods.map { ctMethod ->
            val methodRange = ctMethod.position.line..ctMethod.position.endLine
            val methodIcpInstances = classIcpInstances.filter { it.line in methodRange }
            
            val methodIcpBreakdown = methodIcpInstances.groupBy { it.type }
            
            MethodAnalysis(
                name = ctMethod.simpleName,
                lineRange = methodRange.toSerializable(),
                totalIcp = methodIcpInstances.sumOf { it.weight },
                icpBreakdown = methodIcpBreakdown,
                sloc = calculateMethodSloc(ctMethod),
                isOverSlocLimit = false // Will be set by aggregator if needed
            )
        }

        val classIcpBreakdown = classIcpInstances.groupBy { it.type }
        val lineRange = ctClass.position.line..ctClass.position.endLine

        val totalIcp = classIcpInstances.sumOf { it.weight }

        return ClassAnalysis(
            name = ctClass.simpleName,
            packageName = ctClass.`package`?.qualifiedName ?: "",
            lineRange = lineRange.toSerializable(),
            totalIcp = totalIcp,
            icpBreakdown = classIcpBreakdown,
            methods = methods,
            isOverLimit = totalIcp > config.limit,
            sloc = calculateClassSloc(ctClass)
        )
    }

    private fun calculateClassSloc(ctClass: CtClass<*>): SlocMetrics {
        return try {
            val position = ctClass.position
            if (position.isValidPosition) {
                val content = position.compilationUnit?.originalSourceCode ?: ""
                calculateSloc(content, position.line, position.endLine)
            } else {
                SlocMetrics(0, 0, 0, 0, 0)
            }
        } catch (e: Exception) {
            SlocMetrics(0, 0, 0, 0, 0)
        }
    }

    private fun calculateMethodSloc(method: CtMethod<*>): SlocMetrics {
        return try {
            val position = method.position
            if (position.isValidPosition) {
                val content = position.compilationUnit?.originalSourceCode ?: ""
                calculateSloc(content, position.line, position.endLine)
            } else {
                SlocMetrics(0, 0, 0, 0, 0)
            }
        } catch (e: Exception) {
            SlocMetrics(0, 0, 0, 0, 0)
        }
    }

    private fun calculateSloc(fullContent: String, startLine: Int, endLine: Int): SlocMetrics {
        val lines = fullContent.lines().subList(startLine - 1, endLine)
        
        var total = 0
        var codeOnly = 0
        var comments = 0
        var blankLines = 0
        
        var inBlockComment = false
        
        lines.forEach { line ->
            total++
            val trimmed = line.trim()
            
            if (trimmed.isEmpty()) {
                blankLines++
            } else if (trimmed.startsWith("//")) {
                comments++
            } else if (trimmed.startsWith("/*")) {
                comments++
                if (!trimmed.endsWith("*/")) inBlockComment = true
            } else if (inBlockComment) {
                comments++
                if (trimmed.endsWith("*/")) inBlockComment = false
            } else {
                codeOnly++
            }
        }
        
        return SlocMetrics(
            total = total,
            codeOnly = codeOnly,
            withComments = codeOnly + comments,
            comments = comments,
            blankLines = blankLines
        )
    }

    private class IcpScanner(val config: CddConfig) : CtScanner() {
        val icpInstances = mutableMapOf<IcpType, MutableList<IcpInstance>>()

        private fun addInstance(type: IcpType, element: CtElement, description: String) {
            val position = element.position
            if (!position.isValidPosition) return
            
            val weight = config.icpTypes[type] ?: type.defaultWeight
            val instance = IcpInstance(
                type = type,
                line = position.line,
                column = position.column,
                description = description,
                weight = weight
            )
            icpInstances.getOrPut(type) { mutableListOf() }.add(instance)
        }

        override fun visitCtIf(ifElement: CtIf) {
            addInstance(IcpType.CODE_BRANCH, ifElement, "if branch")
            analyzeCondition(ifElement.condition)
            
            val elseBlock = ifElement.getElseStatement<CtStatement>()
            if (elseBlock != null) {
                // If the else block is an 'if' or a block containing only an 'if', it's an 'else if' chain.
                // We don't count it as a separate 'else branch' here because the next 'if' will count itself.
                val isElseIf = elseBlock is CtIf || (elseBlock is CtBlock<*> && elseBlock.statements.size == 1 && elseBlock.statements[0] is CtIf)
                if (!isElseIf) {
                    addInstance(IcpType.CODE_BRANCH, elseBlock, "else branch")
                }
            }
            super.visitCtIf(ifElement)
        }

        override fun <T : Any?> visitCtSwitch(switchElement: CtSwitch<T>) {
            addInstance(IcpType.CODE_BRANCH, switchElement, "switch branch")
            super.visitCtSwitch(switchElement)
        }

        override fun <T : Any?> visitCtCase(caseElement: CtCase<T>) {
            if (caseElement.caseExpressions.isNotEmpty()) {
                addInstance(IcpType.CODE_BRANCH, caseElement, "case branch")
            }
            super.visitCtCase(caseElement)
        }

        override fun <T : Any?> visitCtConditional(conditional: CtConditional<T>) {
            addInstance(IcpType.CODE_BRANCH, conditional, "ternary operator")
            analyzeCondition(conditional.condition)
            super.visitCtConditional(conditional)
        }

        override fun visitCtFor(forLoop: CtFor) {
            addInstance(IcpType.CODE_BRANCH, forLoop, "for loop")
            analyzeCondition(forLoop.expression)
            super.visitCtFor(forLoop)
        }

        override fun visitCtForEach(foreach: CtForEach) {
            addInstance(IcpType.CODE_BRANCH, foreach, "foreach loop")
            super.visitCtForEach(foreach)
        }

        override fun visitCtWhile(whileLoop: CtWhile) {
            addInstance(IcpType.CODE_BRANCH, whileLoop, "while loop")
            analyzeCondition(whileLoop.loopingExpression)
            super.visitCtWhile(whileLoop)
        }

        override fun visitCtDo(doLoop: CtDo) {
            addInstance(IcpType.CODE_BRANCH, doLoop, "do-while loop")
            analyzeCondition(doLoop.loopingExpression)
            super.visitCtDo(doLoop)
        }

        override fun visitCtTry(tryBlock: CtTry) {
            addInstance(IcpType.EXCEPTION_HANDLING, tryBlock, "try block")
            if (tryBlock.finalizer != null) {
                addInstance(IcpType.EXCEPTION_HANDLING, tryBlock.finalizer, "finally block")
            }
            super.visitCtTry(tryBlock)
        }

        override fun visitCtCatch(catchBlock: CtCatch) {
            addInstance(IcpType.EXCEPTION_HANDLING, catchBlock, "catch block")
            super.visitCtCatch(catchBlock)
        }

        override fun <T : Any?> visitCtTypeReference(reference: CtTypeReference<T>) {
            if (isCouplingType(reference)) {
                val qualifiedName = reference.qualifiedName
                val isInternal = config.internalCoupling.packages.any { qualifiedName.startsWith(it) }
                
                if (isInternal) {
                    addInstance(IcpType.INTERNAL_COUPLING, reference, "Internal coupling: $qualifiedName")
                } else if (!isJdkType(reference)) {
                    addInstance(IcpType.EXTERNAL_COUPLING, reference, "External coupling: $qualifiedName")
                } else {
                }
            }
            super.visitCtTypeReference(reference)
        }

        private fun isCouplingType(reference: CtTypeReference<*>): Boolean {
            if (reference.isPrimitive) return false
            if (reference.simpleName == "void") return false
            // Avoid duplicate counts in some cases like package references
            val parent = reference.parent
            return parent !is CtPackageReference
        }

        private fun isJdkType(reference: CtTypeReference<*>): Boolean {
            val qn = reference.qualifiedName
            if (qn.startsWith("java.") || qn.startsWith("javax.") || qn.startsWith("sun.")) return true
            
            // In no-classpath mode, if package is not resolved, check simple names for standard types
            val standardNames = setOf("String", "List", "ArrayList", "Map", "HashMap", "Set", "HashSet", "Collection", "Integer", "Long", "Double", "Boolean", "Object")
            if (standardNames.contains(reference.simpleName) && (reference.`package` == null || reference.`package`.isImplicit)) {
                return true
            }
            return false
        }

        private fun analyzeCondition(condition: CtExpression<Boolean>?) {
            if (condition == null) return
            
            // Initial condition counts as 1 if it's not a compound expression
            // Actually, PRD says: "Code branches (if-else, switch-case...) - 1 point each"
            // "Conditions (boolean expressions in conditionals) - 1 point each"
            // Example: if (a > b && c < d) = 1 for if, 2 for conditions (a>b and c<d)
            
            // Initial condition
            addInstance(IcpType.CONDITION, condition, "condition expression")
            
            // Count logical operators (&&, ||)
            condition.accept(object : CtScanner() {
                override fun <T : Any?> visitCtBinaryOperator(operator: CtBinaryOperator<T>) {
                    if (operator.kind == BinaryOperatorKind.AND || operator.kind == BinaryOperatorKind.OR) {
                        addInstance(IcpType.CONDITION, operator, "logical operator ${operator.kind}")
                    }
                    super.visitCtBinaryOperator(operator)
                }
            })
        }
    }
}
