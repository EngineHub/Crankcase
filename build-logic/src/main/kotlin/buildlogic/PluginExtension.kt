// SPDX-FileCopyrightText: EngineHub <https://www.enginehub.org/>
// SPDX-License-Identifier: MPL-2.0

package buildlogic

import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType
import org.gradle.plugin.devel.GradlePluginDevelopmentExtension

abstract class PluginExtension(
    private val project: Project,
) {
    private val addedPlugins = mutableSetOf<String>()

    fun add(
        shortId: String,
        description: String,
        implementationClass: String,
        tags: Set<String> = emptySet(),
    ) {
        if (!addedPlugins.add(shortId)) {
            throw IllegalArgumentException("Plugin $shortId already added")
        }
        project.extensions.getByType<GradlePluginDevelopmentExtension>().plugins {
            register(shortId) {
                this.id = "org.enginehub.crankcase.$shortId"
                this.displayName = "Crankcase `$shortId` plugin"
                this.description = description
                this.implementationClass = implementationClass
                this.tags.set(tags)
                this.tags.add("crankcase")
            }
        }
    }
}
