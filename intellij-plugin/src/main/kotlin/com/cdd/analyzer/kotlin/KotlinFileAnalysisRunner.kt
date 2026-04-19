package com.cdd.analyzer.kotlin

import com.cdd.CddConstants
import com.cdd.domain.AnalysisResult
import com.cdd.ui.settings.tools.cdd.CddConfigService
import com.cdd.ui.editor.inlay.CddIcpInlayService
import com.cdd.ui.editor.inlay.EditorCddIcpInlayService
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import java.io.File

internal class KotlinFileAnalysisRunner(
    private val analyzeFile: (Project, File) -> AnalysisResult =
        defaultAnalyzeFile(),
    private val inlayService: CddIcpInlayService = EditorCddIcpInlayService
) {
    fun analyze(project: Project, file: VirtualFile) {
        if (!isKotlinFile(file)) {
            return
        }
        val sourceFile = VfsUtilCore.virtualToIoFile(file)
        val result = analyzeFile(project, sourceFile)
        inlayService.render(project, file, result)
    }

    fun isKotlinFile(file: VirtualFile): Boolean {
        return file.extension?.lowercase() == CddConstants.FILE_EXTENSION_KOTLIN
    }

    companion object {
        private fun defaultAnalyzeFile(): (Project, File) -> AnalysisResult {
            return { project, file ->
                val config = CddConfigService.Companion.getInstance(project).loadConfig()
                IntellijKotlinAnalyzer(project).analyze(file, config)
            }
        }
    }
}