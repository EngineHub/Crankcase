// SPDX-FileCopyrightText: EngineHub <https://www.enginehub.org/>
// SPDX-License-Identifier: MPL-2.0

import buildlogic.GenerateConstantsTask
import org.gradle.api.plugins.quality.Checkstyle
import org.gradle.plugins.ide.idea.model.IdeaModel

plugins {
    id("buildlogic.plugin")
}

ccPlugins.add(
    "common",
    "Common code for all Crankcase plugins",
    "org.enginehub.crankcase.common.CommonPlugin",
    setOf("conventions"),
)

val constantsDir = layout.buildDirectory.dir("generated/sources/constants/main/java")
val generateConstants = tasks.register<GenerateConstantsTask>("generateConstants") {
    description = "Generates the CrankcaseVersions constants source file"
    packageName = "org.enginehub.crankcase.common"
    className = "CrankcaseVersions"
    constants.put("CHECKSTYLE", libs.versions.checkstyle)
    constants.put("ERROR_PRONE", libs.versions.errorProne)
    constants.put("JUNIT", libs.versions.junit)
    outputDirectory = constantsDir
}

sourceSets.main {
    java.srcDir(generateConstants.flatMap { it.outputDirectory })
}

tasks.withType<Checkstyle>().configureEach {
    exclude("**/CrankcaseVersions.java")
}

plugins.withId("idea") {
    configure<IdeaModel> {
        module {
            generatedSourceDirs.add(constantsDir.get().asFile)
        }
    }
}
