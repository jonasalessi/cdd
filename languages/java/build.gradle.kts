plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":core"))
    implementation("fr.inria.gforge.spoon:spoon-core:11.2.1")

    testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
    testImplementation("io.mockk:mockk:1.13.8")
}
