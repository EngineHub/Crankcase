package org.enginehub.crankcase.minecraft

import com.google.common.collect.ImmutableRangeMap
import com.google.common.collect.Range
import com.vdurmont.semver4j.Semver
import org.gradle.api.artifacts.dsl.DependencyConstraintHandler

private typealias MinecraftLibraryConstraintAdder = DependencyConstraintHandler.(configuration: String) -> Unit

private val MAPPING = ImmutableRangeMap.builder<Semver, MinecraftLibraryConstraintAdder>()
    .put(Range.atLeast(looseSemver("1.13"))) { conf ->
        add(conf, "com.google.guava:guava") {
            version { strictly("21.0") }
            because("Mojang provides Guava")
        }
        add(conf, "com.google.code.gson:gson") {
            version { strictly("2.8.0") }
            because("Mojang provides Gson")
        }
        add(conf, "it.unimi.dsi:fastutil") {
            version { strictly("8.2.1") }
            because("Mojang provides FastUtil")
        }
    }
    .build()

private fun looseSemver(minecraftVersion: String) = Semver(minecraftVersion, Semver.SemverType.LOOSE)

fun DependencyConstraintHandler.addMinecraftLibraryConstraints(
    configuration: String,
    minecraftVersion: String
) {
    MAPPING[looseSemver(minecraftVersion)]?.invoke(this, configuration)
}
