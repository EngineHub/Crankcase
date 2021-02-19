package org.enginehub.crankcase.license

import org.enginehub.crankcase.PluginDependentExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply

abstract class CasedLicenseExtension(project: Project) : PluginDependentExtension<CasedLicenseExtension>(project) {
    override fun applyPlugin() {
        project.apply<CasedLicensePlugin>()
    }
}
