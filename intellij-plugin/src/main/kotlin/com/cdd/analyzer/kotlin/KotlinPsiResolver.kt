package com.cdd.analyzer.kotlin

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.psi.KtFile
import java.io.File

internal fun interface KotlinPsiResolver {
    fun resolve(project: Project, file: File): KtFile?
}

internal object ProjectKotlinPsiResolver : KotlinPsiResolver {
    override fun resolve(project: Project, file: File): KtFile? {
        return ReadAction.compute<KtFile?, RuntimeException> {
            val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)
                ?: return@compute null
            if (!virtualFile.isValid) {
                return@compute null
            }

            val psiFile = PsiManager.getInstance(project).findFile(virtualFile) as? KtFile
                ?: return@compute null
            if (!psiFile.isValid) {
                return@compute null
            }

            psiFile
        }
    }
}
