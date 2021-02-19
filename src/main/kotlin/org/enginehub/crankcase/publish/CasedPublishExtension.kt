package org.enginehub.crankcase.publish

import org.enginehub.crankcase.PluginDependentExtension
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.apply

abstract class CasedPublishExtension(project: Project) : PluginDependentExtension<CasedPublishExtension>(project) {
    abstract val publishEnabled: Property<Boolean>

    override fun applyPlugin() {
        project.apply<CasedPublishPlugin>()
    }
}
