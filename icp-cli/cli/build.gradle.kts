plugins {
    kotlin("jvm")
    application
    id("com.gradleup.shadow")
    id("jacoco-report-aggregation")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":languages:java"))
    implementation(project(":languages:kotlin"))

    // CLI Framework
    implementation("com.github.ajalt.clikt:clikt:4.2.1")
    runtimeOnly("org.slf4j:slf4j-simple:2.0.9")

    // Testing
    testImplementation("io.kotest:kotest-runner-junit5:6.1.11")
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

tasks.named<JacocoReport>("testCodeCoverageReport") {
    reports {
        html.required.set(false)
        xml.required.set(true)
    }
}

tasks.check {
    dependsOn(tasks.named<JacocoReport>("testCodeCoverageReport"))
}
