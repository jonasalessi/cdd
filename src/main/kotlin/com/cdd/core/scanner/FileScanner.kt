package com.cdd.core.scanner

import com.cdd.core.registry.AnalyzerRegistry
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.PathMatcher
import java.nio.file.Paths
import org.slf4j.LoggerFactory

/**
 * Scans the file system for source files matching include/exclude patterns.
 */
class FileScanner(
    private val includePatterns: List<String>,
    private val excludePatterns: List<String>
) {
    private val logger = LoggerFactory.getLogger(FileScanner::class.java)
    private val includeMatchers: List<PathMatcher> = includePatterns.map { createMatcher(it) }
    private val excludeMatchers: List<PathMatcher> = excludePatterns.map { createMatcher(it) }

    /**
     * Scans the given directory recursively.
     */
    fun scan(root: File): List<File> {
        if (!root.exists()) {
            logger.error("Scan root does not exist: ${root.absolutePath}")
            return emptyList()
        }

        val results = mutableListOf<File>()
        root.walk().forEach { file ->
            if (file.isFile && isMatch(file, root)) {
                if (AnalyzerRegistry.getAnalyzerFor(file) != null) {
                    results.add(file)
                }
            }
        }
        
        logger.info("Scanned ${results.size} files in ${root.absolutePath}")
        return results
    }

    private fun isMatch(file: File, root: File): Boolean {
        // Relative path for matching
        val relativePath = file.toRelativeString(root)
        val path = Paths.get(relativePath)

        // If no include patterns, include all by default (that have an analyzer)
        val isIncluded = if (includeMatchers.isEmpty()) {
            true
        } else {
            includeMatchers.any { it.matches(path) }
        }

        if (!isIncluded) return false

        // Check if excluded
        val isExcluded = excludeMatchers.any { it.matches(path) }
        
        return !isExcluded
    }

    private fun createMatcher(pattern: String): PathMatcher {
        val syntax = if (pattern.startsWith("glob:") || pattern.startsWith("regex:")) "" else "glob:"
        return FileSystems.getDefault().getPathMatcher("$syntax$pattern")
    }
}
