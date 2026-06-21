/*
 * SPDX-FileCopyrightText: EngineHub <https://www.enginehub.org/>
 * SPDX-License-Identifier: MPL-2.0
 */

package org.enginehub.crankcase.javalibrary;

import org.enginehub.crankcase.testsupport.CrankcaseTestKit;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.enginehub.crankcase.testsupport.BuildResultSubject.assertThat;

class JavaLibraryPluginFunctionalTest {
    @TempDir
    Path projectDir;

    @BeforeEach
    void writeSettings() throws IOException {
        Files.writeString(
            projectDir.resolve("settings.gradle.kts"),
            """
            rootProject.name = "java-library-test"
            """
        );
    }

    private void writeBuild(String extraConfig) throws IOException {
        Files.writeString(
            projectDir.resolve("build.gradle.kts"),
            """
            plugins { id("org.enginehub.crankcase.java-library") }
            repositories { mavenCentral() }
            %s
            """.formatted(extraConfig)
        );
    }

    private void writeCleanSource() throws IOException {
        Path source = projectDir.resolve("src/main/java/example/Example.java");
        Files.createDirectories(source.getParent());
        Files.writeString(
            source,
            """
            package example;

            public class Example {
            }
            """
        );
    }

    private GradleRunner runner(String... args) {
        return CrankcaseTestKit.runner(projectDir, args);
    }

    @Test
    void buildsSourcesAndJavadocJars() throws IOException {
        writeBuild("crankcaseJava { javaRelease = 21 }");
        writeCleanSource();
        BuildResult result = runner("sourcesJar", "javadocJar").build();
        assertThat(result).task(":sourcesJar").succeeded();
        assertThat(result).task(":javadocJar").succeeded();
    }

    @Test
    void doesNotApplyCheckstyle() throws IOException {
        writeBuild("crankcaseJava { javaRelease = 21 }");
        writeCleanSource();
        BuildResult result = runner("checkstyleMain").buildAndFail();
        assertThat(result).output().contains("Task 'checkstyleMain' not found");
    }
}
