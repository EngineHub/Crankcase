// SPDX-FileCopyrightText: EngineHub <https://www.enginehub.org/>
// SPDX-License-Identifier: MPL-2.0

plugins {
    id("buildlogic.plugin")
}

ccPlugins.add(
    "checkstyle",
    "Checkstyle configuration with EngineHub's bundled rules.",
    "org.enginehub.crankcase.checkstyle.CheckstylePlugin",
    setOf("checkstyle", "java"),
)

val testPluginDeps = configurations.dependencyScope("testPluginDeps")
val testPluginClasspath = configurations.resolvable("testPluginClasspath") {
    extendsFrom(testPluginDeps.get())
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
        attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
    }
}

dependencies {
    implementation(project(":crankcase-common"))
    compileOnly(project(":crankcase-java"))
    "testPluginDeps"(project(":crankcase-java"))
}

tasks.pluginUnderTestMetadata {
    pluginClasspath.from(testPluginClasspath)
}
