// SPDX-FileCopyrightText: EngineHub <https://www.enginehub.org/>
// SPDX-License-Identifier: MPL-2.0

plugins {
    id("buildlogic.plugin")
}

ccPlugins.add(
    "release",
    "Flip the gradle.properties version between snapshot and release, committing and tagging via git",
    "org.enginehub.crankcase.release.ReleasePlugin",
    setOf("release", "versioning", "vcs"),
)
