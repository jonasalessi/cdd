plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    // Java AST Analysis (Spoon)
    implementation("fr.inria.gforge.spoon:spoon-core:11.2.1")

    // Kotlin Compiler (for Kotlin analysis)
    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:1.9.24")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

    // YAML Config
    implementation("com.charleskorn.kaml:kaml:0.57.0")

    // Markdown Generation
    implementation("org.commonmark:commonmark:0.21.0")

    // Logging
    implementation("org.slf4j:slf4j-simple:2.0.9")

    // Testing
    testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
    testImplementation("io.mockk:mockk:1.13.8")
}
