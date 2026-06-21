/*
 * SPDX-FileCopyrightText: EngineHub <https://www.enginehub.org/>
 * SPDX-License-Identifier: MPL-2.0
 */

package org.enginehub.crankcase.publishing;

import org.enginehub.crankcase.testsupport.CrankcaseTestKit;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.enginehub.crankcase.testsupport.BuildResultSubject.assertThat;

class PublishingPluginFunctionalTest {
    @TempDir
    Path projectDir;

    private static final String INSPECT_REPO =
        """
        import org.gradle.api.publish.PublishingExtension
        import org.gradle.api.artifacts.repositories.MavenArtifactRepository
        plugins { id("org.enginehub.crankcase.publishing") }
        val publishing = extensions.getByType(PublishingExtension::class.java)
        val repo = publishing.repositories.getByName("EngineHub") as MavenArtifactRepository
        val url = repo.url.toString()
        val user = repo.credentials.username
        tasks.register("printRepo") {
            doLast {
                println("URL=" + url)
                println("USER=" + user)
            }
        }
        """;

    private void writeProject(String version, String buildScript) throws IOException {
        Files.writeString(
            projectDir.resolve("settings.gradle.kts"),
            """
            rootProject.name = "publishing-test"
            """
        );
        if (version != null) {
            Files.writeString(
                projectDir.resolve("gradle.properties"),
                """
                version=%s
                """.formatted(version)
            );
        }
        Files.writeString(projectDir.resolve("build.gradle.kts"), buildScript);
    }

    private GradleRunner runner(String... args) {
        return CrankcaseTestKit.runner(projectDir, args);
    }

    @Test
    void configurationCacheIsReused() throws IOException {
        writeProject(
            "1.0.0",
            """
            plugins { id("org.enginehub.crankcase.publishing") }
            """
        );
        CrankcaseTestKit.assertConfigCacheStoredThenReused(
            runner(
                "help", "--configuration-cache",
                "-Partifactory_contextUrl=https://repo.example"
            )
        );
    }

    @Test
    void failsWhenVersionUnspecified() throws IOException {
        writeProject(null, INSPECT_REPO);
        BuildResult result = runner("printRepo").buildAndFail();
        assertThat(result).output().contains("requires `project.version`");
    }

    @Test
    void routesSnapshotVersionsToSnapshotRepo() throws IOException {
        writeProject("1.0.0-SNAPSHOT", INSPECT_REPO);
        BuildResult result = runner(
            "printRepo", "-q", "-Partifactory_contextUrl=https://repo.example"
        ).build();
        assertThat(result).output().contains("URL=https://repo.example/libs-snapshot-local");
    }

    @Test
    void routesReleaseVersionsToReleaseRepo() throws IOException {
        writeProject("1.0.0", INSPECT_REPO);
        BuildResult result = runner(
            "printRepo", "-q", "-Partifactory_contextUrl=https://repo.example"
        ).build();
        assertThat(result).output().contains("URL=https://repo.example/libs-release-local");
    }

    @Test
    void wiresCredentialsFromProperties() throws IOException {
        writeProject("1.0.0", INSPECT_REPO);
        BuildResult result = runner(
            "printRepo", "-q",
            "-Partifactory_contextUrl=https://repo.example",
            "-Partifactory_user=octy"
        ).build();
        assertThat(result).output().contains("USER=octy");
    }

    @Test
    void doesNotRegisterAnyPublication() throws IOException {
        writeProject(
            "1.0.0",
            """
            import org.gradle.api.publish.PublishingExtension
            plugins {
                java
                id("org.enginehub.crankcase.publishing")
            }
            group = "org.example"
            val publishing = extensions.getByType(PublishingExtension::class.java)
            val names = publishing.publications.names.toString()
            tasks.register("printPublications") {
                doLast { println("PUBLICATIONS=" + names) }
            }
            """
        );
        BuildResult result = runner(
            "printPublications", "-q", "-Partifactory_contextUrl=https://repo.example"
        ).build();
        assertThat(result).output().contains("PUBLICATIONS=[]");
    }
}
