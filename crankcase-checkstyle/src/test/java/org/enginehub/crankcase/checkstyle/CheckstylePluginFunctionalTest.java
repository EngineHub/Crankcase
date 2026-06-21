/*
 * SPDX-FileCopyrightText: EngineHub <https://www.enginehub.org/>
 * SPDX-License-Identifier: MPL-2.0
 */

package org.enginehub.crankcase.checkstyle;

import org.enginehub.crankcase.testsupport.CrankcaseTestKit;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.google.common.truth.Truth.assertThat;
import static org.enginehub.crankcase.testsupport.BuildResultSubject.assertThat;

class CheckstylePluginFunctionalTest {
    @TempDir
    Path projectDir;

    @BeforeEach
    void writeSettings() throws IOException {
        Files.writeString(
            projectDir.resolve("settings.gradle.kts"),
            """
            rootProject.name = "checkstyle-test"
            """
        );
    }

    private void writeBuild(String extraConfig) throws IOException {
        Files.writeString(
            projectDir.resolve("build.gradle.kts"),
            """
            plugins {
                java
                id("org.enginehub.crankcase.checkstyle")
            }
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

            class Example {
            }
            """
        );
    }

    private void writeUnusedImportSource() throws IOException {
        Path source = projectDir.resolve("src/main/java/example/Example.java");
        Files.createDirectories(source.getParent());
        Files.writeString(
            source,
            """
            package example;

            import java.util.List;

            class Example {
            }
            """
        );
    }

    private void writeSuppressionsFile(Path path) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(
            path,
            """
            <?xml version="1.0"?>
            <!DOCTYPE suppressions PUBLIC
                "-//Checkstyle//DTD SuppressionFilter Configuration 1.2//EN"
                "https://checkstyle.org/dtds/suppressions_1_2.dtd">
            <suppressions>
                <suppress checks="UnusedImports" files=".*"/>
            </suppressions>
            """
        );
    }

    private GradleRunner runner(String... args) {
        return CrankcaseTestKit.runner(projectDir, args);
    }

    @Test
    void checkstylePassesOnCleanSource() throws IOException {
        writeBuild("");
        writeCleanSource();
        BuildResult result = runner("check").build();
        assertThat(result).task(":crankcasePrepareCheckstyleConfig").succeeded();
        assertThat(result).task(":checkstyleMain").succeeded();
    }

    @Test
    void checkstyleFailsOnViolationUsingBundledConfig() throws IOException {
        writeBuild("");
        writeUnusedImportSource();
        BuildResult result = runner("checkstyleMain").buildAndFail();
        assertThat(result).task(":checkstyleMain").failed();
        assertThat(result).output().contains("Unused import");
    }

    @Test
    void checkstyleToolVersionComesFromTheVersionCatalog() throws IOException {
        writeBuild(
            """
            val csVersion = checkstyle.toolVersion
            tasks.register("printCheckstyleVersion") {
                doLast { println("checkstyle-version=" + csVersion) }
            }
            """
        );
        BuildResult result = runner("printCheckstyleVersion").build();
        assertThat(result).output().contains("checkstyle-version=13.3.0");
    }

    @Test
    void suppressionsFileSuppressesAViolation() throws IOException {
        writeBuild("crankcaseCheckstyle { suppressionsFile = file(\"checkstyle-suppression.xml\") }");
        writeUnusedImportSource();
        writeSuppressionsFile(projectDir.resolve("checkstyle-suppression.xml"));
        BuildResult result = runner("checkstyleMain").build();
        assertThat(result).task(":checkstyleMain").succeeded();
    }

    @Test
    void withoutSuppressionsTheViolationStillFails() throws IOException {
        writeBuild("");
        writeUnusedImportSource();
        BuildResult result = runner("checkstyleMain").buildAndFail();
        assertThat(result).task(":checkstyleMain").failed();

        Path generated = projectDir.resolve("build/crankcase/checkstyle/checkstyle.xml");
        assertThat(Files.readString(generated)).doesNotContain("SuppressionFilter");
        assertThat(Files.exists(projectDir.resolve("build/crankcase/checkstyle/checkstyle-suppression.xml")))
            .isFalse();
    }

    @Test
    void movingTheSuppressionsFileDoesNotInvalidateTheCache() throws IOException {
        writeBuild("crankcaseCheckstyle { suppressionsFile = file(\"suppressions/a.xml\") }");
        writeUnusedImportSource();
        writeSuppressionsFile(projectDir.resolve("suppressions/a.xml"));
        runner("checkstyleMain").build();

        Files.move(
            projectDir.resolve("suppressions/a.xml"),
            projectDir.resolve("suppressions/b.xml")
        );
        writeBuild("crankcaseCheckstyle { suppressionsFile = file(\"suppressions/b.xml\") }");

        BuildResult result = runner("checkstyleMain").build();
        assertThat(result).task(":crankcasePrepareCheckstyleConfig").hasOutcome(TaskOutcome.UP_TO_DATE);
    }

    @Test
    void configurationCacheIsReused() throws IOException {
        writeBuild("crankcaseCheckstyle { suppressionsFile = file(\"checkstyle-suppression.xml\") }");
        writeCleanSource();
        writeSuppressionsFile(projectDir.resolve("checkstyle-suppression.xml"));
        CrankcaseTestKit.assertConfigCacheStoredThenReused(
            runner("checkstyleMain", "--configuration-cache")
        );
    }
}
