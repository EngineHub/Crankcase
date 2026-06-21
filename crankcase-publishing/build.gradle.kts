// SPDX-FileCopyrightText: EngineHub <https://www.enginehub.org/>
// SPDX-License-Identifier: MPL-2.0

plugins {
    id("buildlogic.plugin")
}

ccPlugins.add(
    "publishing",
    "Publishes to the EngineHub Maven repository.",
    "org.enginehub.crankcase.publishing.PublishingPlugin",
    setOf("publishing", "maven"),
)
