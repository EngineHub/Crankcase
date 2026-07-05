/*
 * SPDX-FileCopyrightText: EngineHub <https://www.enginehub.org/>
 * SPDX-License-Identifier: MPL-2.0
 */

package org.enginehub.crankcase.release;

import org.enginehub.crankcase.testsupport.CrankcaseTestKit;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static org.enginehub.crankcase.testsupport.BuildResultSubject.assertThat;

class ReleasePluginFunctionalTest {
    @TempDir
    Path projectDir;

    @BeforeEach
    void writeProject() throws IOException {
        Files.writeString(
            projectDir.resolve("settings.gradle.kts"),
            """
            rootProject.name = "release-test"
            """
        );
        Files.writeString(
            projectDir.resolve("build.gradle.kts"),
            """
            plugins { id("org.enginehub.crankcase.release") }
            tasks.register("printVersion") {
                val printedVersion = project.version.toString()
                doLast { println("VERSION=" + printedVersion) }
            }
            """
        );
    }

    private GradleRunner runner(String... args) {
        return CrankcaseTestKit.runner(projectDir, args);
    }

    private void writeProperties(String content) throws IOException {
        Files.writeString(projectDir.resolve("gradle.properties"), content);
    }

    private String readProperties() throws IOException {
        return Files.readString(projectDir.resolve("gradle.properties"));
    }

    private void initTrackedRepo() throws IOException, InterruptedException {
        git("init", "-q");
        git("config", "user.name", "Test");
        git("config", "user.email", "test@example.com");
        git("add", "gradle.properties");
        git("commit", "-q", "-m", "initial");
    }

    private String commitSubject() throws IOException, InterruptedException {
        return git("log", "-1", "--format=%s").trim();
    }

    private String git(String... args) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>(List.of(
            "git",
            "-c", "user.name=Test",
            "-c", "user.email=test@example.com",
            "-c", "init.defaultBranch=main"
        ));
        Collections.addAll(command, args);
        ProcessBuilder processBuilder = new ProcessBuilder(command)
            .directory(projectDir.toFile())
            .redirectErrorStream(true);
        processBuilder.environment().clear();
        processBuilder.environment().put("GIT_CONFIG_GLOBAL", "/dev/null");
        Process process = processBuilder
            .start();
        // Async collect input in case of pipe filling issues
        CompletableFuture<String> outputFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }, Executors.newVirtualThreadPerTaskExecutor());
        int exit = process.waitFor();
        String output = outputFuture.join();
        assertWithMessage("%s failed:\n%s", command, output)
            .that(exit)
            .isEqualTo(0);
        return output;
    }

    @Test
    void changeSnapshotToReleaseRewritesVersionCommitsAndTags() throws IOException, InterruptedException {
        writeProperties(
            """
            # A comment
            group=com.example
            version=1.2.3-SNAPSHOT
            """
        );
        initTrackedRepo();

        BuildResult result = runner("changeSnapshotToRelease", "-q").build();
        assertThat(result).task(":changeSnapshotToRelease").succeeded();

        assertThat(readProperties()).isEqualTo(
            """
            # A comment
            group=com.example
            version=1.2.3
            """
        );
        assertThat(commitSubject()).isEqualTo("Release version 1.2.3");
        assertThat(git("cat-file", "-t", "v1.2.3").trim()).isEqualTo("tag");
    }

    @Test
    void changeSnapshotToReleaseFailsOnNonSnapshot() throws IOException, InterruptedException {
        writeProperties(
            """
            version=1.2.3
            """
        );
        initTrackedRepo();

        BuildResult result = runner("changeSnapshotToRelease").buildAndFail();
        assertThat(result).output().contains("Version is not a snapshot version");
    }

    @Test
    void changeReleaseToNextSnapshotBumpsAndCommitsWithoutTag() throws IOException, InterruptedException {
        writeProperties(
            """
            # A comment
            version=1.2.3
            """
        );
        initTrackedRepo();

        BuildResult result = runner("changeReleaseToNextSnapshot", "-q").build();
        assertThat(result).task(":changeReleaseToNextSnapshot").succeeded();

        assertThat(readProperties()).isEqualTo(
            """
            # A comment
            version=1.2.4-SNAPSHOT
            """
        );
        assertThat(commitSubject()).isEqualTo("Switch to next snapshot version 1.2.4-SNAPSHOT");
        assertThat(git("tag").trim()).isEmpty();
    }

    @Test
    void changeReleaseToNextSnapshotFailsOnSnapshot() throws IOException, InterruptedException {
        writeProperties(
            """
            version=1.2.3-SNAPSHOT
            """
        );
        initTrackedRepo();

        BuildResult result = runner("changeReleaseToNextSnapshot").buildAndFail();
        assertThat(result).output().contains("Version is already a snapshot version");
    }

    @Test
    void missingVersionPropertyFailsClearly() throws IOException, InterruptedException {
        writeProperties(
            """
            group=com.example
            """
        );
        initTrackedRepo();

        BuildResult result = runner("changeSnapshotToRelease").buildAndFail();
        assertThat(result).output().contains("No 'version' property found in");
        assertThat(result).output().contains("gradle.properties");
    }

    @Test
    void projectVersionComesFromGradleProperties() throws IOException {
        writeProperties(
            """
            version=1.2.3-SNAPSHOT
            """
        );

        BuildResult result = runner("printVersion", "-q").build();
        assertThat(result).output().contains("VERSION=1.2.3-SNAPSHOT");
    }

    @Test
    void releaseTaskIsConfigurationCacheCompatible() throws IOException, InterruptedException {
        writeProperties(
            """
            version=1.2.3-SNAPSHOT
            """
        );
        initTrackedRepo();

        BuildResult result = runner("changeSnapshotToRelease", "--configuration-cache").build();
        assertThat(result).task(":changeSnapshotToRelease").succeeded();
        assertThat(result).output().contains("Configuration cache entry stored");
    }
}
