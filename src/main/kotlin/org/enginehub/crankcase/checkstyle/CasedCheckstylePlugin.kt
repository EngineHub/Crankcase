package org.enginehub.crankcase.checkstyle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.quality.CheckstyleExtension
import org.gradle.kotlin.dsl.configure

open class CasedCheckstylePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.plugins.apply("checkstyle")
        target.configure<CheckstyleExtension> {
            val checkstyleXmlUrl = CasedCheckstylePlugin::class.java.getResource("checkstyle.xml")
            checkNotNull(checkstyleXmlUrl) { "No checkstyle.xml was found relative to the task" }
            config = target.resources.text.fromUri(checkstyleXmlUrl)
            toolVersion = "8.40"
        }
    }
}
