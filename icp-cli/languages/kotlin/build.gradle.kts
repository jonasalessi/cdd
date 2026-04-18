plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":core"))
    implementation("org.jetbrains.kotlin:kotlin-compiler:2.3.0")

    testImplementation("io.kotest:kotest-runner-junit5:6.1.11")
    testImplementation("io.mockk:mockk:1.13.8")
}
