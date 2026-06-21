// SPDX-FileCopyrightText: EngineHub <https://www.enginehub.org/>
// SPDX-License-Identifier: MPL-2.0

plugins {
    id("buildlogic.plugin")
}

ccPlugins.add(
    "checkstyle",
    "Checkstyle configuration with EngineHub's bundled rules.",
    "org.enginehub.crankcase.checkstyle.CheckstylePlugin",
    setOf("checkstyle", "java"),
)

dependencies {
    implementation(project(":crankcase-common"))
}
