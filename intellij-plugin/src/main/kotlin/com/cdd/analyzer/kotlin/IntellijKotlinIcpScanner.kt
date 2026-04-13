package com.cdd.analyzer.kotlin

import com.cdd.core.config.CddConfig
import com.cdd.domain.IcpInstance
import com.cdd.domain.IcpType
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCatchClause
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDoWhileExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtTryExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.KtWhenExpression
import org.jetbrains.kotlin.psi.KtWhileExpression
import org.jetbrains.kotlin.psi.psiUtil.startOffset

internal class IntellijKotlinIcpScanner(
    private val fullContent: String,
    private val config: CddConfig,
    private val currentKtFile: KtFile,
    private val weights: Map<String, Double>,
    private val analyzedClassName: String?
) : KtTreeVisitorVoid() {
    private val icpInstances = mutableListOf<IcpInstance>()
    private val seenInternalCouplings = mutableSetOf<String>()

    private val imports: Map<String, String> by lazy {
        currentKtFile.importDirectives
            .filter { !it.isAllUnder }
            .mapNotNull { importDirective ->
                val fqName = importDirective.importedFqName?.asString()
                val simpleName = importDirective.importedFqName?.shortName()?.asString()
                if (fqName != null && simpleName != null) {
                    simpleName to fqName
                } else {
                    null
                }
            }
            .toMap()
    }

    private val wildcardImports: List<String> by lazy {
        currentKtFile.importDirectives
            .filter { it.isAllUnder }
            .mapNotNull { it.importedFqName?.asString() }
    }

    fun getIcpInstances(): List<IcpInstance> = icpInstances

    override fun visitIfExpression(expression: KtIfExpression) {
        addInstance(IcpType.CODE_BRANCH, expression, "if branch")
        expression.condition?.let(::analyzeCondition)

        val elseExpression = expression.`else`
        if (elseExpression != null && !isElseIfBranch(elseExpression)) {
            addInstance(IcpType.CODE_BRANCH, elseExpression, "else branch")
        }
        super.visitIfExpression(expression)
    }

    override fun visitWhenExpression(expression: KtWhenExpression) {
        addInstance(IcpType.CODE_BRANCH, expression, "when branch")
        expression.subjectExpression?.let(::analyzeCondition)

        expression.entries
            .filter { it.isElse }
            .forEach { addInstance(IcpType.CODE_BRANCH, it, "else branch") }
        super.visitWhenExpression(expression)
    }

    override fun visitForExpression(expression: KtForExpression) {
        addInstance(IcpType.CODE_BRANCH, expression, "for loop")
        expression.loopRange?.let(::analyzeCondition)
        super.visitForExpression(expression)
    }

    override fun visitWhileExpression(expression: KtWhileExpression) {
        addInstance(IcpType.CODE_BRANCH, expression, "while loop")
        expression.condition?.let(::analyzeCondition)
        super.visitWhileExpression(expression)
    }

    override fun visitDoWhileExpression(expression: KtDoWhileExpression) {
        addInstance(IcpType.CODE_BRANCH, expression, "do-while loop")
        expression.condition?.let(::analyzeCondition)
        super.visitDoWhileExpression(expression)
    }

    override fun visitTryExpression(expression: KtTryExpression) {
        addInstance(IcpType.EXCEPTION_HANDLING, expression, "try block")
        expression.finallyBlock?.let {
            addInstance(IcpType.EXCEPTION_HANDLING, it, "finally block")
        }
        super.visitTryExpression(expression)
    }

    override fun visitCatchSection(catchClause: KtCatchClause) {
        addInstance(IcpType.EXCEPTION_HANDLING, catchClause, "catch block")
        super.visitCatchSection(catchClause)
    }

    override fun visitBinaryExpression(expression: KtBinaryExpression) {
        when (expression.operationToken) {
            KtTokens.ELVIS -> addInstance(IcpType.CONDITION, expression, "elvis condition")
            KtTokens.ANDAND,
            KtTokens.OROR -> addInstance(
                IcpType.CONDITION,
                expression,
                "logical operator ${expression.operationReference.text}"
            )
        }
        super.visitBinaryExpression(expression)
    }

    override fun visitLambdaExpression(lambdaExpression: KtLambdaExpression) {
        addInstance(IcpType.CODE_BRANCH, lambdaExpression, "lambda expression")
        super.visitLambdaExpression(lambdaExpression)
    }

    override fun visitCallExpression(expression: org.jetbrains.kotlin.psi.KtCallExpression) {
        val calleeExpression = expression.calleeExpression as? KtNameReferenceExpression
            ?: return super.visitCallExpression(expression)
        val name = calleeExpression.getReferencedName()
        if (isJdkType(name)) {
            return super.visitCallExpression(expression)
        }

        val resolvedName = imports[name] ?: name
        if (isInternal(resolvedName) && seenInternalCouplings.add(resolvedName)) {
            addInstance(IcpType.INTERNAL_COUPLING, expression, "Internal coupling (call): $resolvedName")
        }
        super.visitCallExpression(expression)
    }

    override fun visitTypeReference(typeReference: KtTypeReference) {
        val typeName = typeReference.text.substringBefore('<').substringBefore('?').trim()
        if (typeName.isNotEmpty() && !isJdkType(typeName)) {
            val resolvedName = imports[typeName] ?: typeName
            if (isInternal(resolvedName) && seenInternalCouplings.add(resolvedName)) {
                addInstance(IcpType.INTERNAL_COUPLING, typeReference, "Internal coupling: $resolvedName")
            }
        }
        super.visitTypeReference(typeReference)
    }

    private fun addInstance(type: IcpType, element: KtElement, description: String) {
        val line = getLineNumber(element.startOffset)
        val column = getColumnNumber(element.startOffset)
        val weight = weights[type.name.lowercase()] ?: type.defaultWeight
        icpInstances.add(IcpInstance(type, line, column, description, weight))
    }

    private fun analyzeCondition(element: KtElement) {
        addInstance(IcpType.CONDITION, element, "condition expression")
    }

    private fun getLineNumber(offset: Int): Int {
        val safeOffset = offset.coerceIn(0, fullContent.length)
        return fullContent.substring(0, safeOffset).count { it == '\n' } + 1
    }

    private fun getColumnNumber(offset: Int): Int {
        val safeOffset = offset.coerceIn(0, fullContent.length)
        val lastNewLine = fullContent.substring(0, safeOffset).lastIndexOf('\n')
        return if (lastNewLine == -1) {
            safeOffset + 1
        } else {
            safeOffset - lastNewLine
        }
    }

    private fun isElseIfBranch(element: KtElement): Boolean {
        return element is KtIfExpression
    }

    private fun isJdkType(qualifiedName: String): Boolean {
        if (qualifiedName.startsWith("java.") ||
            qualifiedName.startsWith("javax.") ||
            qualifiedName.startsWith("kotlin.")
        ) {
            return true
        }

        return qualifiedName in COMMON_TYPES
    }

    private fun isInternal(name: String): Boolean {
        if (analyzedClassName != null) {
            if (name == analyzedClassName) {
                return false
            }
            if (name == analyzedClassName.substringAfterLast('.')) {
                return false
            }
        }

        if (config.internalCoupling.packages.any { packageName -> name.startsWith("$packageName.") || name == packageName }) {
            return true
        }

        if (name.contains(".") || name.isEmpty() || !name.first().isUpperCase()) {
            return false
        }

        if (isDefinedInFile(name)) {
            return true
        }

        val currentPackage = currentKtFile.packageFqName.asString()
        if (currentPackage.isNotEmpty() && isInternalPackage(currentPackage) && !hasExternalWildcardImport()) {
            return true
        }

        return hasInternalWildcardImport() && !hasExternalWildcardImport()
    }

    private fun isDefinedInFile(name: String): Boolean {
        return currentKtFile.declarations.any { declaration ->
            declaration is KtClassOrObject && declaration.name == name
        }
    }

    private fun isInternalPackage(packageName: String): Boolean {
        return config.internalCoupling.packages.any { configuredPackage ->
            packageName.startsWith("$configuredPackage.") || packageName == configuredPackage
        }
    }

    private fun hasInternalWildcardImport(): Boolean {
        return wildcardImports.any(::isInternalPackage)
    }

    private fun hasExternalWildcardImport(): Boolean {
        return wildcardImports.any { !isInternalPackage(it) }
    }

    private companion object {
        val COMMON_TYPES = setOf(
            "String",
            "Int",
            "Long",
            "Boolean",
            "Double",
            "Float",
            "Byte",
            "Short",
            "Char",
            "List",
            "Map",
            "Set",
            "Any",
            "Unit",
            "Array",
            "Exception",
            "RuntimeException",
            "Error",
            "ArithmeticException",
            "NullPointerException",
            "IllegalArgumentException",
            "IllegalStateException",
            "ArrayList",
            "HashMap",
            "HashSet",
            "println",
            "print",
            "require",
            "check",
            "error",
            "assert",
            "lazy",
            "run",
            "let",
            "with",
            "apply",
            "also"
        )
    }
}
