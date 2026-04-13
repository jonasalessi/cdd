**OO Change Plan**

**Summary**

This document captures a prioritized, low-risk plan to address the OO design issues found in the icp-cli codebase. The goal is to improve testability, reduce global mutable state, and separate concerns so future heuristics and reporters can evolve without increasing cognitive load.

**Findings**

1. High — P2 / P3 — core/core/registry/AnalyzerRegistry.kt: global mutable singleton registry couples many components and hinders testing.
2. High — P1 — cli/CddCli: single class performs configuration loading, discovery, analysis orchestration, reporting, debugging and exit logic.
3. Medium — P1 / P8 — core/aggregator/IcpAggregator.kt: mixes numeric aggregation and human-oriented suggestion heuristics.
4. Medium — P4 / P2 — languages/kotlin/KotlinIcpScanner.kt: coupling-detection heuristics are embedded in the AST scanner (low cohesion).
5. Medium — P2 — core/analyzer/AbstractLanguageAnalyzer.kt: silent exception handling for invalid regex patterns (surprising behaviour).
6. Medium — testability — languages/* analyzers instantiate heavy compiler/AST environments inline (Spoon, Kotlin environments).
7. Low — P6 — core/reporter/ReporterRegistry.kt: mutable global registry mirrors AnalyzerRegistry issues.

**Recommended Changes (Ordered by priority)**

1. Implement AnalyzerProvider abstraction and DefaultAnalyzerProvider, migrate FileScanner and consumers to accept an AnalyzerProvider instead of using the global AnalyzerRegistry directly. Also apply same provider pattern to ReporterRegistry.
   - Why: reduces global mutable state, improves testability and allows multiple analysis contexts.
   - Migration: provide a thin AnalyzerRegistry compatibility façade delegating to DefaultAnalyzerProvider, then update consumers incrementally.
   - Before/After sketch:

```kotlin
// Before (current global)
object AnalyzerRegistry {
    private val analyzers = mutableListOf<LanguageAnalyzer>()
    fun register(analyzer: LanguageAnalyzer) { analyzers.add(analyzer) }
    fun getAnalyzerFor(file: File): LanguageAnalyzer? =
        analyzers.find { it.supportedExtensions.contains(file.extension.lowercase()) }
}

// After (injectable provider)
interface AnalyzerProvider {
    fun register(analyzer: LanguageAnalyzer)
    fun getAnalyzerFor(file: File): LanguageAnalyzer?
    fun clear()
}

object DefaultAnalyzerProvider : AnalyzerProvider {
    private val analyzers = mutableListOf<LanguageAnalyzer>()
    override fun register(analyzer: LanguageAnalyzer) { analyzers.add(analyzer) }
    override fun getAnalyzerFor(file: File) =
        analyzers.find { it.supportedExtensions.contains(file.extension.lowercase()) }
    override fun clear() = analyzers.clear()
}

// FileScanner constructor: add analyzerProvider: AnalyzerProvider = DefaultAnalyzerProvider
```

2. Decompose CddCli into small services (ConfigurationLoader, FileDiscoverer, AnalysisService, ReportService). Keep CddCli as an assembler that wires components and handles CLI concerns only.
   - Why: single responsibility, easier unit testing and incremental changes.
   - Migration approach: create default implementations that use existing code and replace direct calls in CddCli with injected services.
   - Sketch:

```kotlin
class CddCli(
    private val configLoader: ConfigurationLoader = DefaultConfigurationLoader,
    private val fileDiscoverer: FileDiscoverer = DefaultFileDiscoverer(),
    private val analysisService: AnalysisService = DefaultAnalysisService(DefaultAnalyzerProvider),
    private val reportService: ReportService = DefaultReportService(DefaultReporterProvider)
) : CliktCommand(...) {
    override fun run() {
        val config = configLoader.load(...) 
        val files = fileDiscoverer.discover(path, config)
        val results = analysisService.analyze(files, config)
        val aggregated = IcpAggregator().aggregate(results, config)
        reportService.generateAndOutput(aggregated, config)
    }
}
```

3. Extract SuggestionEngine from IcpAggregator.
   - Why: separates numeric aggregation (stats) from heuristic suggestions; easier to test and evolve heuristics.
   - Migration: move addOverLimitSuggestions/addIcpTypeSuggestions/addCorrelationSuggestions/addMethodSuggestions into SuggestionEngine.buildSuggestions(context).

4. Extract CouplingDetector from KotlinIcpScanner (and Java scanner where applicable).
   - Why: decouples complex heuristics from AST traversal to a testable strategy class.
   - Sketch:

```kotlin
interface CouplingDetector {
    fun isInternal(qualifiedName: String, currentFile: KtFile?, analyzedClassName: String?): Boolean
}

class DefaultCouplingDetector(private val config: CddConfig) : CouplingDetector { ... }

// KotlinIcpScanner receives detector and calls detector.isInternal(...)
```

5. Improve logging/validation in AbstractLanguageAnalyzer.resolveWeights / resolveIcpLimit.
   - Why: invalid regex patterns in config should not be silently ignored.
   - Change: log warnings including failing pattern and languageName. Do not change runtime behaviour beyond surfacing the issue.

6. (Optional medium) Introduce factory/wrapper interfaces for heavy environment objects (Spoon Launcher, Kotlin environment) to improve analyzer testability.

**Estimates & Verification**

1. AnalyzerProvider + ReporterProvider: small, ~2-4 hours. Verification: update and run relevant unit tests and module verification.
   - Command: cd icp-cli && ./gradlew classes testClasses --quiet
2. CddCli decomposition: medium, ~4-8 hours (depending on test updates). Verification: cd icp-cli && ./gradlew classes testClasses --quiet
3. SuggestionEngine extraction: small, ~2-4 hours. Verification: run icp-cli module checks above.
4. CouplingDetector extraction: medium, ~3-6 hours (requires unit tests for heuristics). Verification: icp-cli module checks.
5. Logging/validation change: trivial, <1 hour. Verification: run tests.

Important repository rules to follow before refactoring Kotlin code:

- Mandatory: run coverage before any Kotlin refactor: cd icp-cli && ./gradlew clean coverageReport then inspect icp-cli/build/reports/jacoco/coverageReport/coverageReport.xml. The project policy requires 100% coverage before refactoring Kotlin files; if coverage <100% add tests first or get explicit approval to proceed.

**Files Likely Affected**

1. core/src/main/kotlin/com/cdd/core/registry/AnalyzerRegistry.kt
2. core/src/main/kotlin/com/cdd/reporter/ReporterRegistry.kt
3. cli/src/main/kotlin/com/cdd/cli/Main.kt
4. core/src/main/kotlin/com/cdd/core/aggregator/IcpAggregator.kt
5. languages/kotlin/src/main/kotlin/com/cdd/analyzer/kotlin/KotlinIcpScanner.kt
6. core/src/main/kotlin/com/cdd/analyzer/AbstractLanguageAnalyzer.kt
7. languages/java/* and languages/kotlin/* analyzers (for optional environment factories)

**Risks & Notes**

1. Introducing provider interfaces is low-risk if done incrementally with compat facades. Keep AnalyzerRegistry delegating to DefaultAnalyzerProvider during migration.
2. Decomposing CddCli affects CLI wiring; maintain identical runtime behaviour and CLI flags during refactor.
3. If Kotlin coverage is below 100% you must add tests before refactoring significant Kotlin logic (policy requirement).

**Next Steps**

1. I can implement step 1 (AnalyzerProvider + ReporterProvider) now and run the icp-cli module verification. This gives immediate testability benefits and establishes the provider pattern.
2. Alternatively, tell me which numbered step (1-5) to implement first.

End of plan.
