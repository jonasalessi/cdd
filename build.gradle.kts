import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.24"
    kotlin("plugin.serialization") version "1.9.24"
    application
    id("com.gradleup.shadow") version "9.3.0"
}

group = "com.cdd"
version = "0.1.0-SNAPSHOT"

kotlin {
    jvmToolchain(21)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}


repositories {
    mavenCentral()
}

dependencies {
    // CLI Framework
    implementation("com.github.ajalt.clikt:clikt:4.2.1")

    // Java AST Analysis (Spoon)
    implementation("fr.inria.gforge.spoon:spoon-core:11.2.1")

    // Kotlin Compiler (for Kotlin analysis)
    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:1.9.24")


    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    // implementation("org.jetbrains.kotlinx:kotlinx-serialization-xml:0.86.2") // Uncomment if needed

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

application {
    mainClass.set("com.cdd.cli.MainKt")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
    }
}


tasks.withType<Test> {
    useJUnitPlatform()
}
