/*
 * SPDX-FileCopyrightText: EngineHub <https://www.enginehub.org/>
 * SPDX-License-Identifier: MPL-2.0
 */

package org.enginehub.crankcase.common;

import org.enginehub.crankcase.testsupport.CrankcaseTestKit;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.enginehub.crankcase.testsupport.BuildResultSubject.assertThat;

class CommonPluginFunctionalTest {
    @TempDir
    Path projectDir;

    private void writeSettings() throws IOException {
        Files.writeString(
            projectDir.resolve("settings.gradle.kts"),
            """
            rootProject.name = "common-test"
            """
        );
    }

    private GradleRunner runner(String... args) {
        return CrankcaseTestKit.runner(projectDir, args);
    }

    @Test
    void configurationCacheIsReused() throws IOException {
        writeSettings();
        Files.writeString(
            projectDir.resolve("build.gradle.kts"),
            """
            plugins { id("org.enginehub.crankcase.common") }
            """
        );
        CrankcaseTestKit.assertConfigCacheStoredThenReused(
            runner("help", "--configuration-cache")
        );
    }

    @Test
    void appliesCleanlyWithoutIdea() throws IOException {
        writeSettings();
        Files.writeString(
            projectDir.resolve("build.gradle.kts"),
            """
            plugins { id("org.enginehub.crankcase.common") }
            """
        );
        BuildResult result = runner("help").build();
        assertThat(result).output().contains("BUILD SUCCESSFUL");
    }

    @Test
    void configuresIdeaToDownloadSourcesAndJavadoc() throws IOException {
        writeSettings();
        Files.writeString(
            projectDir.resolve("build.gradle.kts"),
            """
            import org.gradle.plugins.ide.idea.model.IdeaModel
            plugins {
                id("org.enginehub.crankcase.common")
                idea
            }
            tasks.register("checkIdea") {
                val idea = project.extensions.getByType(IdeaModel::class.java)
                val downloadSources = idea.module.isDownloadSources
                val downloadJavadoc = idea.module.isDownloadJavadoc
                doLast {
                    check(downloadSources) { "expected idea to download sources" }
                    check(downloadJavadoc) { "expected idea to download javadoc" }
                }
            }
            """
        );
        BuildResult result = runner("checkIdea").build();
        assertThat(result).output().contains("BUILD SUCCESSFUL");
    }
}
