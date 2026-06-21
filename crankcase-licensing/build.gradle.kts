// SPDX-FileCopyrightText: EngineHub <https://www.enginehub.org/>
// SPDX-License-Identifier: MPL-2.0

plugins {
    id("buildlogic.plugin")
}

repositories {
    gradlePluginPortal {
        content {
            includeGroupAndSubgroups("net.octyl.level-headered")
        }
    }
}

ccPlugins.add(
    "licensing",
    "Standard licensing configuration.",
    "org.enginehub.crankcase.licensing.LicensingPlugin",
    setOf("license", "license-header"),
)

dependencies {
    implementation(libs.levelHeadered)
}
