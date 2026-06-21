// SPDX-FileCopyrightText: EngineHub <https://www.enginehub.org/>
// SPDX-License-Identifier: MPL-2.0

plugins {
    id("buildlogic.plugin")
}

ccPlugins.add(
    "java-library",
    "Standard Java Library configuration.",
    "org.enginehub.crankcase.javalibrary.JavaLibraryPlugin",
    setOf("java", "library"),
)

dependencies {
    implementation(project(":crankcase-java"))
}
