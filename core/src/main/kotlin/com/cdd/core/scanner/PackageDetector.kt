package com.cdd.core.scanner

import org.slf4j.LoggerFactory
import java.io.File

/**
 * Detects internal packages from scanned source files.
 */
object PackageDetector {
    private val logger = LoggerFactory.getLogger(PackageDetector::class.java)

    private val packageRegex = Regex("""^package\s+([\w\.]+)""", RegexOption.MULTILINE)

    /**
     * Detects all unique package names from the provided list of files.
     */
    fun detectPackages(files: List<File>): Set<String> {
        val packages = mutableSetOf<String>()

        files.forEach { file ->
            try {
                val content = file.inputStream().bufferedReader().use { reader ->
                    val buffer = CharArray(2048)
                    val read = reader.read(buffer)
                    if (read > 0) String(buffer, 0, read) else ""
                }

                packageRegex.find(content)?.groupValues?.get(1)?.let {
                    packages.add(it)
                }
            } catch (e: Exception) {
                logger.warn("Failed to detect package for ${file.absolutePath}: ${e.message}")
            }
        }

        logger.info("Detected ${packages.size} internal packages")
        return packages
    }
}
