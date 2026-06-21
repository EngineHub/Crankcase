// SPDX-FileCopyrightText: EngineHub <https://www.enginehub.org/>
// SPDX-License-Identifier: MPL-2.0

import buildlogic.getVersion
import buildlogic.stringyLibs
import org.gradle.plugins.ide.idea.model.IdeaModel

plugins {
    id("net.octyl.level-headered")
    checkstyle
}

group = providers.gradleProperty("group").get()
version = providers.gradleProperty("version").get()

repositories {
    mavenCentral()
    maven {
        name = "EngineHub Repository"
        url = uri("https://maven.enginehub.org/repo/")
    }
}

checkstyle {
    toolVersion = stringyLibs.getVersion("checkstyle").toString()
    configFile = rootDir.resolve(
        "crankcase-checkstyle/src/main/resources/org/enginehub/crankcase/checkstyle/default-checkstyle.xml"
    )
}

levelHeadered {
    headerTemplate(rootDir.resolve("HEADER.txt"))
}

plugins.withId("idea") {
    configure<IdeaModel> {
        module {
            isDownloadSources = true
            isDownloadJavadoc = true
        }
    }
}
