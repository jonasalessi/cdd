plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

    // YAML Config
    implementation("com.charleskorn.kaml:kaml:0.57.0")

    // Markdown Generation
    implementation("org.commonmark:commonmark:0.28.0")

    // Logging API
    implementation("org.slf4j:slf4j-api:2.0.9")

    // Testing
    testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
    testImplementation("io.mockk:mockk:1.13.8")
}
