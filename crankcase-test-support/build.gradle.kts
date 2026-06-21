// SPDX-FileCopyrightText: EngineHub <https://www.enginehub.org/>
// SPDX-License-Identifier: MPL-2.0

import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
    `java-library`
    id("buildlogic.common")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

dependencies {
    implementation(libs.guava)
    api(gradleTestKit())
    api(platform(libs.junit.bom))
    api(libs.junit.jupiter.api)
    api(libs.truth) {
        exclude(group = "junit", module = "junit")
    }
}
