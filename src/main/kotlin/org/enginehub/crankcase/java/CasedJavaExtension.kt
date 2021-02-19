package org.enginehub.crankcase.java

import org.enginehub.crankcase.PluginDependentExtension
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.apply

abstract class CasedJavaExtension(project: Project) : PluginDependentExtension<CasedJavaExtension>(project) {
    /**
     * Binds the Java version used by this project.
     */
    abstract val javaVersion: Property<JavaLanguageVersion>

    /**
     * `-X:lint` flags to disable. By convention, this is "processing", "path", "fallthrough", and "serial".
     */
    abstract val disabledLints: SetProperty<String>

    override fun applyPlugin() {
        project.apply<CasedJavaPlugin>()
    }
}
