/*
 * SPDX-FileCopyrightText: EngineHub <https://www.enginehub.org/>
 * SPDX-License-Identifier: MPL-2.0
 */

package org.enginehub.crankcase.java;

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

class JavaPluginFunctionalTest {
    @TempDir
    Path projectDir;

    @BeforeEach
    void writeSettings() throws IOException {
        Files.writeString(
            projectDir.resolve("settings.gradle.kts"),
            """
            rootProject.name = "java-test"
            """
        );
    }

    private void writeBuild(String extraConfig) throws IOException {
        Files.writeString(
            projectDir.resolve("build.gradle.kts"),
            """
            plugins { id("org.enginehub.crankcase.java") }
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

    private void writeCastWarningSource() throws IOException {
        Path source = projectDir.resolve("src/main/java/example/Example.java");
        Files.createDirectories(source.getParent());
        Files.writeString(
            source,
            """
            package example;

            class Example {
                static final int X = (int) 5;
            }
            """
        );
    }

    private GradleRunner runner(String... args) {
        return CrankcaseTestKit.runner(projectDir, args);
    }

    @Test
    void configurationCacheIsReused() throws IOException {
        writeBuild("crankcaseJava { javaRelease = 21 }");
        writeCleanSource();
        CrankcaseTestKit.assertConfigCacheStoredThenReused(
            runner("compileJava", "--configuration-cache")
        );
    }

    @Test
    void appliesJavaAndCompilesWithJavaReleaseSet() throws IOException {
        writeBuild("crankcaseJava { javaRelease = 21 }");
        writeCleanSource();
        BuildResult result = runner("compileJava").build();
        assertThat(result).task(":compileJava").succeeded();
    }

    @Test
    void disabledLintsSuppressesOtherwiseFatalWarning() throws IOException {
        writeBuild(
            """
            crankcaseJava {
              javaRelease = 21
              disabledLints = listOf("cast")
            }
            """
        );
        writeCastWarningSource();
        BuildResult result = runner("compileJava").build();
        assertThat(result).task(":compileJava").succeeded();
    }

    @Test
    void lintWarningIsFatalWithoutDisablingTheLint() throws IOException {
        writeBuild("crankcaseJava { javaRelease = 21 }");
        writeCastWarningSource();
        BuildResult result = runner("compileJava").buildAndFail();
        assertThat(result).task(":compileJava").failed();
    }

    @Test
    void javaReleaseConfiguresToolchainLanguageVersion() throws IOException {
        writeBuild(
            """
            crankcaseJava { javaRelease = 21 }
            val languageVersion = java.toolchain.languageVersion
            tasks.register("printToolchain") {
                doLast { println("LANG=" + languageVersion.get()) }
            }
            """
        );
        BuildResult result = runner("printToolchain").build();
        assertThat(result).output().contains("LANG=21");
    }

    @Test
    void doesNotApplyCheckstyle() throws IOException {
        writeBuild("crankcaseJava { javaRelease = 21 }");
        writeCleanSource();
        BuildResult result = runner("checkstyleMain").buildAndFail();
        assertThat(result).output().contains("Task 'checkstyleMain' not found");
    }

    @Test
    void basePluginDoesNotRegisterSourcesJar() throws IOException {
        writeBuild("crankcaseJava { javaRelease = 21 }");
        BuildResult result = runner("sourcesJar").buildAndFail();
        assertThat(result).output().contains("Task 'sourcesJar' not found");
    }

    @Test
    void toolVersionsAreOverridable() throws IOException {
        writeBuild(
            """
            crankcaseJava {
              javaRelease = 21
              errorproneVersion = "2.41.0"
              junitVersion = "5.13.0"
            }
            val tools = (configurations["errorprone"].dependencies + configurations["testImplementation"].dependencies)
                .joinToString(",") { "${it.group}:${it.name}:${it.version}" }
            tasks.register("printToolVersions") {
                doLast { println("TOOLS=" + tools) }
            }
            """
        );
        BuildResult result = runner("printToolVersions").build();
        assertThat(result).output().contains("com.google.errorprone:error_prone_core:2.41.0");
        assertThat(result).output().contains("org.junit:junit-bom:5.13.0");
    }

    @Test
    void basePluginDoesNotCreateApiConfiguration() throws IOException {
        writeBuild(
            """
            val hasApi = configurations.findByName("api") != null
            tasks.register("printApiConfig") {
                doLast { println("has-api=" + hasApi) }
            }
            """
        );
        BuildResult result = runner("printApiConfig").build();
        assertThat(result).output().contains("has-api=false");
    }
}
