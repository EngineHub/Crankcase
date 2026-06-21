// SPDX-FileCopyrightText: EngineHub <https://www.enginehub.org/>
// SPDX-License-Identifier: MPL-2.0

plugins {
    id("buildlogic.plugin")
}

ccPlugins.add(
    "repo-reconfiguration",
    "Reconfigure repositories to EngineHub mirrors with content filtering",
    "org.enginehub.crankcase.reporeconfiguration.RepoReconfigurationPlugin",
    setOf("repositories", "mirror"),
)
