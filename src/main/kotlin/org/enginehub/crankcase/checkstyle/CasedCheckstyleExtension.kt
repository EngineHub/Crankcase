package org.enginehub.crankcase.checkstyle

import org.enginehub.crankcase.PluginDependentExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply

abstract class CasedCheckstyleExtension(project: Project) : PluginDependentExtension<CasedCheckstyleExtension>(project) {
    override fun applyPlugin() {
        project.apply<CasedCheckstylePlugin>()
    }
}
