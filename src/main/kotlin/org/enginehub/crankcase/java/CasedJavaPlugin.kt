package org.enginehub.crankcase.java

import org.enginehub.crankcase.crankcase
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.external.javadoc.StandardJavadocDocletOptions
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.withType

open class CasedJavaPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.crankcase.java.javaVersion.convention(JavaLanguageVersion.of(8))
        target.crankcase.java.disabledLints.convention(listOf(
            "processing", "path", "fallthrough", "serial"
        ))

        target.plugins.apply("java")
        target.configure<JavaPluginExtension> {
            withJavadocJar()
            withSourcesJar()
            toolchain {
                languageVersion.set(target.crankcase.java.javaVersion)
            }
        }
        for (name in setOf("compileJava", "compileTestJava")) {
            target.tasks.named<JavaCompile>(name) {
                options.compilerArgs.add("-Xlint:all")
                options.compilerArgumentProviders.add {
                    target.crankcase.java.disabledLints.getOrElse(setOf())
                        .map { "-Xlint:-$it" }
                }
                options.isDeprecation = true
                options.encoding = "UTF-8"
            }
        }

        target.tasks.withType<Javadoc>().configureEach {
            (options as StandardJavadocDocletOptions).apply {
                addStringOption("Xdoclint:none", "-quiet")
                tags(
                    "apiNote:a:API Note:",
                    "implSpec:a:Implementation Requirements:",
                    "implNote:a:Implementation Note:"
                )
            }
        }
    }
}
