plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "cdd-intellij-plugin"

val icpCliPath = file("../icp-cli")
if (icpCliPath.exists()) {
    includeBuild("../icp-cli") {
        dependencySubstitution {
            substitute(module("com.cdd:core")).using(project(":core"))
        }
    }
}
