// SPDX-FileCopyrightText: EngineHub <https://www.enginehub.org/>
// SPDX-License-Identifier: MPL-2.0

plugins {
    id("buildlogic.plugin")
}

ccPlugins.add(
    "java",
    "Standard Java configuration.",
    "org.enginehub.crankcase.java.JavaPlugin",
    setOf("java"),
)

dependencies {
    implementation(project(":crankcase-common"))
    implementation(libs.errorprone.gradle.plugin)
}
