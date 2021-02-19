package org.enginehub.crankcase.publish

import org.enginehub.crankcase.crankcase
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.*
import org.jfrog.gradle.plugin.artifactory.dsl.ArtifactoryPluginConvention
import org.jfrog.gradle.plugin.artifactory.task.ArtifactoryTask

private const val ARTIFACTORY_CONTEXT_URL = "artifactory_contextUrl"
private const val ARTIFACTORY_USER = "artifactory_user"
private const val ARTIFACTORY_PASSWORD = "artifactory_password"

open class CasedPublishPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        // By default, do not publish root project unless it is the only project
        target.crankcase.publish.publishEnabled.convention(
            target != target.rootProject || target.allprojects.size == 1
        )

        target.apply("maven-publish")
        target.configure<PublishingExtension> {
            publications {
                register<MavenPublication>("maven") {
                    from(target.components["java"])
                }
            }
        }

        if (target == target.rootProject) {
            target.applyRootArtifactoryConfig()
        }
        target.applyCommonArtifactoryConfig()
    }

    private fun Project.applyRootArtifactoryConfig() {
        if (!hasProperty(ARTIFACTORY_CONTEXT_URL)) {
            extensions.extraProperties[ARTIFACTORY_CONTEXT_URL] = "http://localhost"
        }
        if (!hasProperty(ARTIFACTORY_USER)) {
            extensions.extraProperties[ARTIFACTORY_USER] = "guest"
        }
        if (!hasProperty(ARTIFACTORY_PASSWORD)) {
            extensions.extraProperties[ARTIFACTORY_PASSWORD] = ""
        }

        apply(plugin = "com.jfrog.artifactory")
        configure<ArtifactoryPluginConvention> {
            setContextUrl("${project.property(ARTIFACTORY_CONTEXT_URL)}")
            clientConfig.publisher.run {
                repoKey = when {
                    "${project.version}".contains("SNAPSHOT") -> "libs-snapshot-local"
                    else -> "libs-release-local"
                }
                username = "${project.property(ARTIFACTORY_USER)}"
                password = "${project.property(ARTIFACTORY_PASSWORD)}"
                isMaven = true
                isIvy = false
            }
        }
    }

    private fun Project.applyCommonArtifactoryConfig() {
        tasks.named<ArtifactoryTask>("artifactoryPublish") {
            onlyIf { crankcase.publish.publishEnabled.get() }
            publications("maven")
            setPublishArtifacts(true)
        }
    }
}
