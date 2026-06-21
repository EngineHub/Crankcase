// SPDX-FileCopyrightText: EngineHub <https://www.enginehub.org/>
// SPDX-License-Identifier: MPL-2.0

plugins {
    id("buildlogic.plugin")
}

ccPlugins.add(
    "git",
    "Local git repository operations",
    "org.enginehub.crankcase.git.GitPlugin",
    setOf("git", "vcs"),
)

dependencies {
    implementation(libs.guava)
}
