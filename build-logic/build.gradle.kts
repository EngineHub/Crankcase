// SPDX-FileCopyrightText: EngineHub <https://www.enginehub.org/>
// SPDX-License-Identifier: MPL-2.0

plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    maven {
        name = "EngineHub Repository"
        url = uri("https://maven.enginehub.org/repo/")
    }
}

dependencies {
    implementation(gradleApi())
    implementation(libs.publish.plugin)
    implementation(libs.crankcase.common)
    implementation(libs.crankcase.checkstyle)
    implementation(libs.crankcase.licensing)
    implementation(libs.crankcase.publishing)
}
