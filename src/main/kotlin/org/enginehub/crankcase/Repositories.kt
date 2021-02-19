/**
 * Extensions for `RepositoryHandler` defining commonly used repositories in our org.
 */
package org.enginehub.crankcase

import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.InclusiveRepositoryContentDescriptor
import java.net.URI

fun RepositoryHandler.engineHub() = maven {
    name = "EngineHub"
    url = URI.create("https://maven.enginehub.org/repo/")
    mavenContent {
        includeGroupByPrefix("com.sk89q")
        includeGroupByPrefix("org.enginehub")
        includeGroupByPrefix("com.zachsthings")
    }
}

fun RepositoryHandler.ossSnapshots() = maven {
    name = "Sonatype OSS Snapshots"
    url = URI.create("https://oss.sonatype.org/content/repositories/snapshots/")
    mavenContent {
        snapshotsOnly()
    }
}

fun RepositoryHandler.fabricMc() = maven {
    name = "Fabric MC"
    url = URI.create("https://maven.fabricmc.net/")
    content {
        includeGroupByPrefix("net.fabricmc")
        includeModule("org.jetbrains", "intellij-fernflower")
    }
}

fun RepositoryHandler.minecraftForge() = maven {
    name = "MinecraftForge"
    url = URI.create("https://files.minecraftforge.net/maven")
    content {
        includeGroupByPrefix("net.minecraftforge")
    }
}

fun RepositoryHandler.spongePowered() = maven {
    name = "SpongePowered"
    url = URI.create("https://repo.spongepowered.org/maven")
    content {
        includeGroupByPrefix("org.spongepowered")
    }
}

fun InclusiveRepositoryContentDescriptor.includeGroupByPrefix(prefix: String) {
    includeGroupByRegex(Regex.escape(prefix) + ".*")
}
