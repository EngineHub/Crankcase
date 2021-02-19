package org.enginehub.crankcase.license

import net.minecrell.gradle.licenser.LicenseExtension
import org.enginehub.crankcase.crankcase
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.configure

open class CasedLicensePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.plugins.apply("net.minecrell.licenser")
        target.configure<LicenseExtension> {
            header = target.rootProject.file("HEADER.txt")
            include("**/*.java")
            include("**/*.kt")
            include("**/*.groovy")
        }
    }
}
