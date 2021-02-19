plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    id("org.jetbrains.kotlin.jvm") version embeddedKotlinVersion
}

repositories {
    mavenCentral()
    gradlePluginPortal()
    maven {
        name = "Forge Maven"
        url = uri("https://files.minecraftforge.net/maven")
        content {
            includeGroupByRegex("net\\.minecraftforge.*")
        }
    }
    maven {
        name = "Fabric"
        url = uri("https://maven.fabricmc.net/")
        content {
            includeGroupByRegex("net\\.fabricmc.*")
            includeModule("org.jetbrains", "intellij-fernflower")
        }
    }
    maven {
        name = "sponge"
        url = uri("https://repo.spongepowered.org/maven")
        content {
            includeGroupByRegex("org\\.spongepowered.*")
        }
    }
    maven {
        name = "EngineHub Repository"
        url = uri("https://maven.enginehub.org/repo/")
        content {
            includeGroupByRegex("com\\.sk89q.*")
            includeGroupByRegex("org\\.enginehub.*")
        }
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    implementation("gradle.plugin.net.minecrell:licenser:0.4.1")
    implementation("org.ajoberstar.grgit:grgit-gradle:4.1.0")
    implementation("com.github.jengelman.gradle.plugins:shadow:6.1.0")
    implementation("org.jfrog.buildinfo:build-info-extractor-gradle:4.20.0")
    implementation("org.spongepowered:SpongeGradle:0.11.5")
    implementation("net.minecraftforge.gradle:ForgeGradle:4.0.9")
    implementation("net.fabricmc:fabric-loom:0.5.43")
    implementation("net.fabricmc:sponge-mixin:0.9.2+mixin.0.8.2")
    implementation("org.enginehub.gradle:gradle-codecov-plugin:0.1.0")
    implementation("com.vdurmont:semver4j:3.1.0")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
}

gradlePlugin {
    // Define the plugin
    plugins.create("crankcase") {
        id = "org.enginehub.crankcase"
        implementationClass = "org.enginehub.crankcase.CrankcasePlugin"
    }
}

val functionalTestSourceSet = sourceSets.create("functionalTest") {
}

gradlePlugin.testSourceSets(functionalTestSourceSet)
configurations {
    getByName("functionalTestImplementation").extendsFrom(getByName("testImplementation"))
}

val functionalTest by tasks.registering(Test::class) {
    testClassesDirs = functionalTestSourceSet.output.classesDirs
    classpath = functionalTestSourceSet.runtimeClasspath
}

tasks.named("check") {
    dependsOn(functionalTest)
}
