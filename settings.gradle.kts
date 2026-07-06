// SPDX-FileCopyrightText: EngineHub <https://www.enginehub.org/>
// SPDX-License-Identifier: MPL-2.0

pluginManagement {
    repositories {
        maven {
            name = "EngineHub"
            url = uri("https://repo.enginehub.org/libs-release/")
        }
        maven {
            name = "EngineHub Central Mirror"
            url = uri("https://repo.enginehub.org/internal/maven-central-proxy/")
        }
        maven {
            name = "EngineHub Plugin Portal Mirror"
            url = uri("https://repo.enginehub.org/internal/plugin-portal-proxy/")
        }
    }
}
plugins {
    id("org.enginehub.crankcase.repo-reconfiguration") version "0.1.0"
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositories {
        maven {
            name = "EngineHub"
            url = uri("https://repo.enginehub.org/libs-release/")
        }
    }
}

rootProject.name = "Crankcase"

includeBuild("build-logic")

include(
    "crankcase-checkstyle",
    "crankcase-common",
    "crankcase-git",
    "crankcase-japicmp",
    "crankcase-java",
    "crankcase-java-library",
    "crankcase-licensing",
    "crankcase-publishing",
    "crankcase-release",
    "crankcase-repo-reconfiguration",
    "crankcase-test-support",
)
