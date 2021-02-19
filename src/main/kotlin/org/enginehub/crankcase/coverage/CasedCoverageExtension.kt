package org.enginehub.crankcase.coverage

import org.enginehub.crankcase.PluginDependentExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply

abstract class CasedCoverageExtension(project: Project) : PluginDependentExtension<CasedCoverageExtension>(project) {
    override fun applyPlugin() {
        project.apply<CasedCoveragePlugin>()
    }
}
