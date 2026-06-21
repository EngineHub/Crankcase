// SPDX-FileCopyrightText: EngineHub <https://www.enginehub.org/>
// SPDX-License-Identifier: MPL-2.0

package buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import javax.inject.Inject

interface GenerateConstantsSpec {
    @get:Input
    val packageName: Property<String>

    @get:Input
    val className: Property<String>

    @get:Input
    val constants: MapProperty<String, String>

    @get:OutputDirectory
    val outputDirectory: DirectoryProperty
}

abstract class GenerateConstantsTask : DefaultTask(), GenerateConstantsSpec {
    @get:Inject
    abstract val workerExecutor: WorkerExecutor

    @TaskAction
    fun generate() {
        val task = this
        workerExecutor.noIsolation().submit(GenerateConstantsWorkAction::class.java) {
            packageName.set(task.packageName)
            className.set(task.className)
            constants.set(task.constants)
            outputDirectory.set(task.outputDirectory)
        }
    }
}

interface GenerateConstantsParameters : GenerateConstantsSpec, WorkParameters

abstract class GenerateConstantsWorkAction : WorkAction<GenerateConstantsParameters> {
    override fun execute() {
        val pkg = parameters.packageName.get()
        val cls = parameters.className.get()
        val fields = parameters.constants.get().toSortedMap()
            .entries.joinToString("\n") { (name, value) ->
                "    public static final String $name = \"${escape(value)}\";"
            }

        val content = """
            /*
             * SPDX-FileCopyrightText: EngineHub <https://www.enginehub.org/>
             * SPDX-License-Identifier: MPL-2.0
             */

            package $pkg;

            public final class $cls {
            @CONSTANTS@

                private $cls() {
                }
            }
            """.trimIndent().replaceFirst("@CONSTANTS@", fields) + "\n"

        val targetDir = parameters.outputDirectory.get().asFile.toPath().resolve(pkg.replace('.', '/'))
        Files.createDirectories(targetDir)
        Files.writeString(targetDir.resolve("$cls.java"), content, StandardCharsets.UTF_8)
    }

    private fun escape(value: String): String =
        value.replace("\\", "\\\\").replace("\"", "\\\"")
}
