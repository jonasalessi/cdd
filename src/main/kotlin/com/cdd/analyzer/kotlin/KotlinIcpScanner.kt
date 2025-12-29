package com.cdd.analyzer.kotlin

import com.cdd.core.config.CddConfig
import com.cdd.domain.IcpInstance
import com.cdd.domain.IcpType
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class KotlinIcpScanner(val fullContent: String, val config: CddConfig, val currentKtFile: KtFile) :
    KtTreeVisitorVoid() {
    private val icpInstances = mutableListOf<IcpInstance>()

    fun getIcpInstances() = icpInstances

    private val imports: Map<String, String> by lazy {
        currentKtFile.importDirectives.mapNotNull { import ->
            val fqName = import.importedFqName?.asString()
            val simpleName = import.importedFqName?.shortName()?.asString()
            if (fqName != null && simpleName != null) simpleName to fqName else null
        }.toMap()
    }

    private fun addInstance(type: IcpType, element: PsiElement, description: String) {
        val line = getLineNumber(fullContent, element.startOffset)
        val column = getColumnNumber(fullContent, element.startOffset)
        val weight = config.icpTypes[type] ?: type.defaultWeight

        icpInstances.add(IcpInstance(type, line, column, description, weight))
    }


    private fun getLineNumber(content: String, offset: Int): Int {
        if (offset < 0) return 1
        val safeOffset = offset.coerceAtMost(content.length)
        return content.substring(0, safeOffset).count { it == '\n' } + 1
    }

    private fun getColumnNumber(content: String, offset: Int): Int {
        if (offset <= 0) return 1
        val safeOffset = offset.coerceAtMost(content.length)
        val lastNewLine = content.substring(0, safeOffset).lastIndexOf('\n')
        return if (lastNewLine == -1) safeOffset + 1 else safeOffset - lastNewLine
    }

    override fun visitIfExpression(expression: KtIfExpression) {
        addInstance(IcpType.CODE_BRANCH, expression, "if branch")
        expression.condition?.let { analyzeCondition(it) }

        val elseExpr = expression.`else`
        if (elseExpr != null) {
            // If the else branch is a block containing only an if, it's an else-if chain
            val isElseIf =
                elseExpr is KtIfExpression || (elseExpr is KtBlockExpression && elseExpr.statements.size == 1 && elseExpr.statements[0] is KtIfExpression)
            if (!isElseIf) {
                addInstance(IcpType.CODE_BRANCH, elseExpr, "else branch")
            }
        }
        super.visitIfExpression(expression)
    }

    override fun visitWhenExpression(expression: KtWhenExpression) {
        addInstance(IcpType.CODE_BRANCH, expression, "when branch")
        expression.subjectExpression?.let { analyzeCondition(it) }

        expression.entries.forEach { entry ->
            if (entry.isElse) {
                addInstance(IcpType.CODE_BRANCH, entry, "else branch")
            }
        }
        super.visitWhenExpression(expression)
    }

    override fun visitForExpression(expression: KtForExpression) {
        addInstance(IcpType.CODE_BRANCH, expression, "for loop")
        expression.loopRange?.let { analyzeCondition(it) }
        super.visitForExpression(expression)
    }

    override fun visitWhileExpression(expression: KtWhileExpression) {
        addInstance(IcpType.CODE_BRANCH, expression, "while loop")
        expression.condition?.let { analyzeCondition(it) }
        super.visitWhileExpression(expression)
    }

    override fun visitDoWhileExpression(expression: KtDoWhileExpression) {
        addInstance(IcpType.CODE_BRANCH, expression, "do-while loop")
        expression.condition?.let { analyzeCondition(it) }
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
        val operationToken = expression.operationToken
        if (operationToken == KtTokens.ELVIS) {
            addInstance(IcpType.CODE_BRANCH, expression, "elvis operator")
            addInstance(IcpType.CONDITION, expression, "elvis condition")
        } else if (operationToken == KtTokens.ANDAND || operationToken == KtTokens.OROR) {
            addInstance(IcpType.CONDITION, expression, "logical operator ${expression.operationReference.text}")
        }
        super.visitBinaryExpression(expression)
    }

    override fun visitSafeQualifiedExpression(expression: KtSafeQualifiedExpression) {
        addInstance(IcpType.CODE_BRANCH, expression, "safe call")
        addInstance(IcpType.CONDITION, expression, "safe call condition")
        super.visitSafeQualifiedExpression(expression)
    }

    override fun visitCallExpression(expression: KtCallExpression) {
        val callee = expression.calleeExpression
        if (callee is KtNameReferenceExpression) {
            val text = callee.getReferencedName()
            if (!isJdkType(text)) {
                val resolvedFqName = imports[text] ?: text
                if (isInternal(resolvedFqName)) {
                    addInstance(IcpType.INTERNAL_COUPLING, expression, "Internal coupling (call): $resolvedFqName")
                }
            }
        }
        super.visitCallExpression(expression)
    }

    override fun visitConstructorCalleeExpression(constructorCalleeExpression: KtConstructorCalleeExpression) {
        constructorCalleeExpression.typeReference?.let { analyzeTypeReference(it) }
        super.visitConstructorCalleeExpression(constructorCalleeExpression)
    }

    override fun visitTypeReference(typeReference: KtTypeReference) {
        analyzeTypeReference(typeReference)
        super.visitTypeReference(typeReference)
    }

    private fun analyzeTypeReference(typeReference: KtTypeReference) {
        val text = typeReference.text.substringBefore('<').substringBefore('?').trim()
        if (text.isEmpty() || isJdkType(text)) return

        val resolvedFqName = imports[text] ?: text
        if (isInternal(resolvedFqName)) {
            addInstance(IcpType.INTERNAL_COUPLING, typeReference, "Internal coupling: $resolvedFqName")
        }
    }

    private fun analyzeCondition(element: PsiElement) {
        // Only add as a condition if it's not already handled (like logical operators)
        // Actually, to match Java's high ICP counts, we'll keep adding it for now.
        addInstance(IcpType.CONDITION, element, "condition expression")
    }

    private fun isJdkType(qualifiedName: String): Boolean {
        if (qualifiedName.startsWith("java.") ||
            qualifiedName.startsWith("javax.") ||
            qualifiedName.startsWith("kotlin.")
        ) return true

        val commonTypes = listOf(
            "String", "Int", "Long", "Boolean", "Double", "Float", "Byte", "Short", "Char",
            "List", "Map", "Set", "Any", "Unit", "Array", "Exception", "RuntimeException",
            "Error", "ArithmeticException", "NullPointerException", "IllegalArgumentException",
            "IllegalStateException", "ArrayList", "HashMap", "HashSet",
            "println", "print", "require", "check", "error", "assert", "lazy", "run", "let", "with", "apply", "also"
        )
        return commonTypes.contains(qualifiedName)
    }

    private fun isCommonType(name: String): Boolean {
        return listOf("String", "Int", "Long", "Boolean", "Double", "Float", "Any", "Unit").contains(name)
    }

    private fun isInternal(qualifiedName: String): Boolean {
        return config.internalCoupling.packages.any { pkg ->
            qualifiedName.startsWith("$pkg.") || qualifiedName == pkg
        }
    }
}