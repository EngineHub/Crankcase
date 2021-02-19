package org.enginehub.crankcase.coverage

import org.enginehub.codecov.CodecovExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.the
import org.gradle.testing.jacoco.tasks.JacocoReport

open class CasedCoveragePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.apply {
            plugin("jacoco")
            // codecov upload only goes on the root project
            if (target == target.rootProject) {
                plugin("org.enginehub.codecov")
            }
        }
        if (target == target.rootProject) {
            val totalReport = target.tasks.register<JacocoReport>("jacocoTotalReport") {
                target.allprojects.forEach { subproject ->
                    subproject.plugins.withId("jacoco") {
                        subproject.plugins.withId("java") {
                            executionData(
                                subproject.fileTree(subproject.buildDir.absolutePath).include("**/jacoco/*.exec")
                            )
                            sourceSets(subproject.the<JavaPluginConvention>().sourceSets["main"])
                            dependsOn(subproject.tasks.named("check"))
                        }
                    }
                }

                reports {
                    xml.isEnabled = true
                    xml.destination = target.rootProject.buildDir.resolve("reports/jacoco/report.xml")
                    html.isEnabled = true
                }

                classDirectories.setFrom(classDirectories.files.map {
                    target.fileTree(it).apply {
                        exclude("**/*AutoValue_*")
                        exclude("**/*Registration.*")
                    }
                })
            }

            target.configure<CodecovExtension> {
                reportTask.set(totalReport)
            }
        }
    }
}
