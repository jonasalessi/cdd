plugins {
    kotlin("jvm")
    application
    id("com.gradleup.shadow")
}

dependencies {
    implementation(project(":core"))

    // CLI Framework
    implementation("com.github.ajalt.clikt:clikt:4.2.1")

    // Testing
    testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
    testImplementation("io.mockk:mockk:1.13.8")
}

application {
    mainClass.set("com.cdd.cli.MainKt")
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveFileName.set("cdd-cli.jar")
}

tasks.named<Zip>("distZip") {
    archiveFileName.set("cdd-cli.zip")
}

tasks.named<Tar>("distTar") {
    enabled = false
}

tasks.named<Zip>("shadowDistZip") {
    enabled = false
}

tasks.named<Tar>("shadowDistTar") {
    enabled = false
}
