// SPDX-FileCopyrightText: EngineHub <https://www.enginehub.org/>
// SPDX-License-Identifier: MPL-2.0

pluginManagement {
    repositories {
        maven {
            name = "EngineHub"
            url = uri("https://maven.enginehub.org/repo/")
        }
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositories {
        maven {
            name = "EngineHub"
            url = uri("https://maven.enginehub.org/repo/")
        }
        gradle.settingsEvaluated {
            // Duplicates repositoriesHelper.kt, since we can't import it
            val allowedPrefixes = listOf(
                "https://maven.enginehub.org",
                "https://repo.maven.apache.org/maven2/",
                "file:"
            )

            for (repo in this@repositories) {
                if (repo is MavenArtifactRepository) {
                    val urlString = repo.url.toString()
                    check(allowedPrefixes.any { urlString.startsWith(it) }) {
                        "Only EngineHub/Central repositories are allowed: ${repo.url} found"
                    }
                }
            }
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
