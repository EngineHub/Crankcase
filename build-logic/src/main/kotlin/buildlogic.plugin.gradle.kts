// SPDX-FileCopyrightText: EngineHub <https://www.enginehub.org/>
// SPDX-License-Identifier: MPL-2.0

import buildlogic.PluginExtension
import buildlogic.stringyLibs
import buildlogic.getLibrary
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion
import java.util.concurrent.Callable

plugins {
    `java-gradle-plugin`
    id("com.gradle.plugin-publish")
    id("buildlogic.common")
}


repositories {
    gradlePluginPortal {
        content {
            includeGroupAndSubgroups("net.ltgt.errorprone")
            includeGroupAndSubgroups("net.ltgt.gradle")
        }
    }
}

extensions.create<PluginExtension>("ccPlugins", project)
gradlePlugin {
    val thisProjectUrl = "https://github.com/EngineHub/Crankcase"
    website = thisProjectUrl
    vcsUrl = thisProjectUrl
}

publishing {
    publications {
        create<MavenPublication>("pluginMaven") {
            artifactId = project.name.removePrefix("crankcase-")
        }
    }
    // TODO: remove after published using published plugins
    repositories {
        maven {
            name = "EngineHub"
            val contextUrl = providers.gradleProperty("artifactory_contextUrl")
                .getOrElse("https://localhost")
            val artifactoryUser = providers.gradleProperty("artifactory_user")
            val artifactoryPassword = providers.gradleProperty("artifactory_password")
            setUrl(Callable {
                val version = project.version.toString()
                if (version == "unspecified") {
                    throw GradleException("requires `project.version` to be set before publishing.")
                }
                val repoPath =
                    if (version.contains("SNAPSHOT")) "libs-snapshot-local" else "libs-release-local"
                "$contextUrl/$repoPath"
            })
            credentials {
                username = artifactoryUser.orNull
                password = artifactoryPassword.orNull
            }
        }
    }
}

java.toolchain.languageVersion = JavaLanguageVersion.of(25)

tasks
    .withType<JavaCompile>()
    .matching { it.name == "compileJava" || it.name == "compileTestJava" }
    .configureEach {
        options.release = java.toolchain.languageVersion.map { it.asInt() }
        options.compilerArgs.addAll(listOf("-Xlint:all", "-parameters", "-Werror"))
        options.isDeprecation = true
        options.encoding = "UTF-8"
    }

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

dependencies {
    "compileOnlyApi"(stringyLibs.getLibrary("jspecify"))
    "implementation"(stringyLibs.getLibrary("guava"))
    "testImplementation"(project(":crankcase-test-support"))
    "testImplementation"(gradleTestKit())
    "testImplementation"(platform(stringyLibs.getLibrary("junit-bom")))
    "testImplementation"(stringyLibs.getLibrary("junit-jupiter-api"))
    "testImplementation"(stringyLibs.getLibrary("junit-jupiter-params"))
    "testRuntimeOnly"(stringyLibs.getLibrary("junit-jupiter-engine"))
    "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Javadoc>().configureEach {
    options.encoding = "UTF-8"
    (options as StandardJavadocDocletOptions).apply {
        addBooleanOption("Werror", true)
        addBooleanOption("Xdoclint:all", true)
        addBooleanOption("Xdoclint:-missing", true)
        tags(
            "apiNote:a:API Note:",
            "implSpec:a:Implementation Requirements:",
            "implNote:a:Implementation Note:"
        )
    }
}

configure<JavaPluginExtension> {
    withJavadocJar()
    withSourcesJar()
}
