/*
 * SPDX-FileCopyrightText: EngineHub <https://www.enginehub.org/>
 * SPDX-License-Identifier: MPL-2.0
 */

package org.enginehub.crankcase.licensing;

import org.enginehub.crankcase.testsupport.CrankcaseTestKit;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static org.enginehub.crankcase.testsupport.BuildResultSubject.assertThat;

class LicensingPluginFunctionalTest {
    @TempDir
    Path projectDir;

    private Path source;

    @BeforeEach
    void writeProject() throws IOException {
        Files.writeString(
            projectDir.resolve("settings.gradle.kts"),
            """
            rootProject.name = "licensing-test"
            """
        );
        Files.writeString(
            projectDir.resolve("build.gradle.kts"),
            """
            plugins {
                java
                id("org.enginehub.crankcase.licensing")
            }
            """
        );
        Files.writeString(
            projectDir.resolve("HEADER.txt"),
            """
            Example header for the Crankcase licensing test.
            """
        );
        source = projectDir.resolve("src/main/java/example/Example.java");
        Files.createDirectories(source.getParent());
    }

    private GradleRunner runner(String... args) {
        return CrankcaseTestKit.runner(projectDir, args);
    }

    private void writeUnheaderedSource() throws IOException {
        Files.writeString(
            source,
            """
            package example;

            class Example {
            }
            """
        );
    }

    @Test
    void verifyHeaderFailsAndNamesTheOffendingFile() throws IOException {
        writeUnheaderedSource();
        BuildResult result = runner("verifyHeader").buildAndFail();
        assertThat(result).output().contains("Example.java");
    }

    @Test
    void applyHeaderThenVerifyHeaderRoundTrips() throws IOException {
        writeUnheaderedSource();
        runner("applyHeader").build();
        assertThat(Files.readString(source))
            .contains("Example header for the Crankcase licensing test.");
        BuildResult verify = runner("verifyHeader").build();
        assertThat(verify).output().contains("BUILD SUCCESSFUL");
    }

    @Test
    void applyHeaderIsIdempotent() throws IOException {
        writeUnheaderedSource();
        runner("applyHeader").build();
        String afterFirst = Files.readString(source);
        runner("applyHeader").build();
        String afterSecond = Files.readString(source);
        assertWithMessage("re-applying the header must be a no-op")
            .that(afterSecond)
            .isEqualTo(afterFirst);
    }

    @Test
    void configurationCacheIsReused() {
        CrankcaseTestKit.assertConfigCacheStoredThenReused(
            runner("help", "--configuration-cache")
        );
    }
}
