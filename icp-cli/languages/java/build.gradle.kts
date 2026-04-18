plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":core"))
    implementation("fr.inria.gforge.spoon:spoon-core:11.2.1")

    testImplementation("io.kotest:kotest-runner-junit5:6.1.11")
    testImplementation("io.mockk:mockk:1.14.9")
}
