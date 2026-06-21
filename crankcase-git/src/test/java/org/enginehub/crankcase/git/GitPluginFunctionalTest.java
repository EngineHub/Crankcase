/*
 * SPDX-FileCopyrightText: EngineHub <https://www.enginehub.org/>
 * SPDX-License-Identifier: MPL-2.0
 */

package org.enginehub.crankcase.git;

import com.google.common.truth.StringSubject;
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

import static com.google.common.truth.Truth.assertWithMessage;
import static org.enginehub.crankcase.testsupport.BuildResultSubject.assertThat;

class GitPluginFunctionalTest {
    @TempDir
    Path projectDir;

    @BeforeEach
    void writeProject() throws IOException {
        Files.writeString(
            projectDir.resolve("settings.gradle.kts"),
            """
            rootProject.name = "git-test"
            """
        );
        Files.writeString(
            projectDir.resolve("build.gradle.kts"),
            """
            import org.enginehub.crankcase.git.GitExtension
            plugins { id("org.enginehub.crankcase.git") }
            val git = extensions.getByType(GitExtension::class.java)
            tasks.register("printHash") {
                val hash = git.commitHash
                doLast { println("HASH=" + hash.get()) }
            }
            tasks.register("printDirty") {
                val dirty = git.dirty
                doLast { println("DIRTY=" + dirty.get()) }
            }
            """
        );
    }

    private GradleRunner runner(String... args) {
        return CrankcaseTestKit.runner(projectDir, args);
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

    private String shortHead() throws IOException, InterruptedException {
        return git("rev-parse", "--short", "HEAD").trim();
    }

    @Test
    void exposesShortCommitHashFromRepository() throws IOException, InterruptedException {
        git("init", "-q");
        Files.writeString(projectDir.resolve("a.txt"), "hello");
        git("add", "a.txt");
        git("commit", "-q", "-m", "initial");

        String expected = shortHead();
        BuildResult result = runner("printHash", "-q").build();
        assertThat(result).output().contains("HASH=" + expected);
    }

    @Test
    void gitCommitHashPropertyOverridesProbe() {
        BuildResult result = runner("printHash", "-q", "-PgitCommitHash=deadbee").build();
        assertThat(result).output().contains("HASH=deadbee");
    }

    @Test
    void configurationCacheIsReused() throws IOException, InterruptedException {
        git("init", "-q");
        Files.writeString(projectDir.resolve("a.txt"), "hello");
        git("add", "a.txt");
        git("commit", "-q", "-m", "initial");
        CrankcaseTestKit.assertConfigCacheStoredThenReused(
            runner("printHash", "--configuration-cache")
        );
    }

    @Test
    void missingRepositoryProducesClearError() {
        BuildResult result = runner("printHash").buildAndFail();
        StringSubject output = assertThat(result).output();
        output.contains("Could not determine the git commit hash");
        output.contains("'git rev-parse --short HEAD' exited with");
        output.contains("Ensure the build runs inside a git repository with at least one commit");
        output.contains("-PgitCommitHash=<hash>");
    }

    @Test
    void reportsWorkingTreeDirtyState() throws IOException, InterruptedException {
        git("init", "-q");
        Files.writeString(
            projectDir.resolve(".gitignore"),
            """
            .gradle/
            build/
            """
        );
        Files.writeString(projectDir.resolve("a.txt"), "hello");
        git("add", "-A");
        git("commit", "-q", "-m", "initial");

        BuildResult clean = runner("printDirty", "-q").build();
        assertThat(clean).output().contains("DIRTY=false");

        Files.writeString(projectDir.resolve("a.txt"), "changed");
        BuildResult dirty = runner("printDirty", "-q").build();
        assertThat(dirty).output().contains("DIRTY=true");
    }

    @Test
    void gitDirtyPropertyOverridesProbe() {
        BuildResult result = runner("printDirty", "-q", "-PgitDirty=true").build();
        assertThat(result).output().contains("DIRTY=true");
    }

    @Test
    void applyingPluginOutsideRepositoryWithoutQueryingDoesNotThrow() {
        BuildResult result = runner("help").build();
        assertThat(result).task(":help").succeeded();
    }
}
