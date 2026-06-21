// SPDX-FileCopyrightText: EngineHub <https://www.enginehub.org/>
// SPDX-License-Identifier: MPL-2.0

import net.octyl.levelheadered.HeaderApplyTask
import net.octyl.levelheadered.HeaderVerifyTask

plugins {
    id("buildlogic.plugin")
}

repositories {
    gradlePluginPortal {
        content {
            includeGroupAndSubgroups("me.champeau.gradle")
        }
    }
}

ccPlugins.add(
    "japicmp",
    "Interface to JApiCmp for backwards compatibility checks.",
    "org.enginehub.crankcase.japicmp.JApiCmpPlugin",
    setOf("compatibility", "api"),
)

dependencies {
    api(libs.japicmp)
    implementation(libs.gson)
}

// Exclude the classes from Gradle as we license them separately.
listOf(tasks.applyHeader, tasks.verifyHeader).forEach { task ->
    task.configure {
        sourceMatchPatterns.get().apply {
            exclude("**/org/enginehub/crankcase/japicmp/internal/accept/**")
        }
    }
}
// Make sure the license is maintained:
val acceptHeader = resources.text.fromString(
    """
    SPDX-FileCopyrightText: The Gradle team
    SPDX-FileCopyrightText: EngineHub <https://www.enginehub.org/>
    SPDX-License-Identifier: Apache-2.0
    """.trimIndent()
)
val acceptSources = layout.projectDirectory.dir(
    "src/main/java/org/enginehub/crankcase/japicmp/internal/accept"
)
val applyAcceptHeader = tasks.register<HeaderApplyTask>("applyAcceptHeader") {
    description = "Applies the Apache-2.0 file header to the accept rule sources."
    group = "formatting"
    source.from(acceptSources)
    headerTemplate = acceptHeader
}
val verifyAcceptHeader = tasks.register<HeaderVerifyTask>("verifyAcceptHeader") {
    description = "Verifies the Apache-2.0 file headers of the accept rule sources."
    group = "verification"
    source.from(acceptSources)
    headerTemplate = acceptHeader
    headerApplyTaskPath = applyAcceptHeader.map { it.path }
}
tasks.applyHeaderToAll.configure {
    dependsOn(applyAcceptHeader)
}
tasks.check.configure {
    dependsOn(verifyAcceptHeader)
}
