plugins {
    kotlin("jvm") version "2.3.20" apply false
    kotlin("plugin.serialization") version "2.3.0" apply false
    id("com.gradleup.shadow") version "9.3.0" apply false
}

allprojects {
    group = "com.cdd"
    version = "0.1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "jacoco")

    configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
        jvmToolchain(21)
    }


    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions {
            freeCompilerArgs.add("-Xjsr305=strict")
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        finalizedBy(tasks.named("jacocoTestReport"))
    }

    tasks.named<JacocoReport>("jacocoTestReport") {
        dependsOn(tasks.named<Test>("test"))
        reports {
            html.required.set(false)
            xml.required.set(true)
        }
    }
}

tasks.register<Sync>("coverageReport") {
    group = "verification"
    description = "Generates the aggregated JaCoCo coverage report for the full build."
    dependsOn(":cli:testCodeCoverageReport")
    from(project(":cli").layout.buildDirectory.dir("reports/jacoco/testCodeCoverageReport")) {
        rename("testCodeCoverageReport.xml", "coverageReport.xml")
    }
    into(layout.buildDirectory.dir("reports/jacoco/coverageReport"))
}
