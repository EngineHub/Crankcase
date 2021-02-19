package org.enginehub.crankcase.relocatedlib

import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.ProjectLayout
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.get
import javax.inject.Inject

/**
 * Creates a relocated source jar from the given configurations.
 */
abstract class RelocatedSourceJar : Jar() {

    // Implicitly @Input because the files are computed from it
    @get:Internal
    abstract val sourceConfigurations: SetProperty<String>

    @get:Input
    abstract val relocations: MapProperty<String, String>

    @get:Inject
    protected abstract val projectLayout: ProjectLayout
    @get:Inject
    protected abstract val archiveOperations: ArchiveOperations

    // This doesn't need to be @InputFiles because it's added to `from`
    private fun getInputFiles() {
        val deps = sourceConfigurations.get().flatMap { conf ->
            project.configurations[conf].incoming.dependencies
                .filterIsInstance<ModuleDependency>()
                .map { it.copy() }
                .map { dependency ->
                    dependency.artifact {
                        name = dependency.name
                        type = "sources"
                        extension = "jar"
                        classifier = "sources"
                    }
                    dependency
                }
        }

        projectLayout.files(project.configurations.detachedConfiguration(*deps.toTypedArray())
            .resolvedConfiguration.lenientConfiguration.artifacts
            .filter { it.classifier == "sources" }
            .map { archiveOperations.zipTree(it.file) })
    }

    init {
        from({ getInputFiles() })
        eachFile {
            relocations.getOrElse(mapOf()).forEach { (from, to) ->
                val filePattern = Regex("(.*)${from.replace('.', '/')}((?:/|$).*)")
                val textPattern = Regex.fromLiteral(from)
                filter {
                    it.replaceFirst(textPattern, to)
                }
                path = path.replaceFirst(filePattern, "$1${to.replace('.', '/')}$2")
            }
        }
        archiveClassifier.convention("sources")
    }
}
