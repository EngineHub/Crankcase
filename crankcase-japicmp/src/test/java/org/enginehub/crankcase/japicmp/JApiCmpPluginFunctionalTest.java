/*
 * SPDX-FileCopyrightText: EngineHub <https://www.enginehub.org/>
 * SPDX-License-Identifier: MPL-2.0
 */

package org.enginehub.crankcase.japicmp;

import org.enginehub.crankcase.testsupport.CrankcaseTestKit;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static org.enginehub.crankcase.testsupport.BuildResultSubject.assertThat;
import static org.enginehub.crankcase.testsupport.BuildResultSubject.buildResults;

class JApiCmpPluginFunctionalTest {
    @TempDir
    Path projectDir;

    @BeforeEach
    void writeSettings() throws IOException {
        Files.writeString(
            projectDir.resolve("settings.gradle.kts"),
            """
            rootProject.name = "japicmp-test"
            """
        );
    }

    private void writeBuildScriptWithFooCheck() throws IOException {
        Files.writeString(
            projectDir.resolve("build.gradle.kts"),
            """
            plugins { id("org.enginehub.crankcase.japicmp") }
            japicmp {
                addCheck("foo") {
                    dependencies {
                        oldClasspath("org.jspecify:jspecify:1.0.0")
                        newClasspath("org.jspecify:jspecify:1.0.0")
                    }
                }
            }
            """
        );
    }

    private GradleRunner runner(String... args) {
        return CrankcaseTestKit.runner(projectDir, args);
    }

    @Test
    void configurationCacheIsReused() throws IOException {
        writeBuildScriptWithFooCheck();
        CrankcaseTestKit.assertConfigCacheStoredThenReused(
            runner("help", "--configuration-cache")
        );
    }

    @Test
    void registersExpectedTasksUnderApiCompatibilityGroup() throws IOException {
        writeBuildScriptWithFooCheck();
        BuildResult result = runner("tasks", "--group", "API Compatibility").build();
        String out = result.getOutput();
        assertThat(out).contains("checkApiCompatibility");
        assertThat(out).contains("checkFooApiCompatibility");
        assertThat(out).contains("resetAcceptedApiChangesFiles");
        assertThat(out).contains("resetFooAcceptedApiChangesFile");
    }

    @Test
    void checkApiCompatibilityDependsOnPerCheckTask() throws IOException {
        writeTwoJarCheck("public class Api {}", "public class Api {}");
        BuildResult result = runner("checkApiCompatibility").build();
        assertThat(result).task(":checkApiCompatibility").succeeded();
        assertThat(result).task(":checkFooApiCompatibility").succeeded();
    }

    @Test
    void invalidNameIsRejected() throws IOException {
        Files.writeString(
            projectDir.resolve("build.gradle.kts"),
            """
            plugins { id("org.enginehub.crankcase.japicmp") }
            japicmp {
                addCheck("foo-bar") { }
            }
            """
        );
        BuildResult result = runner("help").buildAndFail();
        assertThat(result).output().contains("Invalid check name 'foo-bar'");
    }

    @Test
    void configurationDoesNotTouchTheFilesystem() throws IOException {
        writeBuildScriptWithFooCheck();
        runner("help").build();
        Path changeFile = projectDir.resolve(
            "src/changes/accepted-foo-public-api-changes.json"
        );
        assertWithMessage("configuration must not write the change file; reset task is the only writer")
            .that(Files.exists(changeFile))
            .isFalse();
    }

    @Test
    void resetTaskOverwritesChangeFile() throws IOException {
        writeBuildScriptWithFooCheck();
        Path changeFile = projectDir.resolve(
            "src/changes/accepted-foo-public-api-changes.json"
        );
        Files.createDirectories(changeFile.getParent());
        Files.writeString(changeFile, "{\"some\":\"content\"}");
        BuildResult result = runner("resetFooAcceptedApiChangesFile").build();
        assertThat(result).task(":resetFooAcceptedApiChangesFile").succeeded();
        assertThat(Files.readString(changeFile)).isEqualTo(
            """
            {
            }
            """
        );
    }

    @Test
    void resetTaskIsNeverUpToDate() throws IOException {
        writeBuildScriptWithFooCheck();
        runner("resetFooAcceptedApiChangesFile").build();
        BuildResult second = runner("resetFooAcceptedApiChangesFile").build();
        assertWithMessage("reset must always re-execute, never UP-TO-DATE")
            .about(buildResults())
            .that(second)
            .task(":resetFooAcceptedApiChangesFile")
            .succeeded();
    }

    @Test
    void staleAcceptedChangeFailsViaPostProcessRule() throws IOException {
        writeTwoJarCheck("public class Api {}", "public class Api {}");
        Path changeFile = projectDir.resolve(
            "src/changes/accepted-foo-public-api-changes.json"
        );
        Files.createDirectories(changeFile.getParent());
        Files.writeString(
            changeFile,
            """
            {
              "Accepted change that no longer matches anything": [
                {
                  "type": "com.example.Gone",
                  "member": "Method gone()",
                  "changes": ["METHOD_REMOVED"]
                }
              ]
            }
            """
        );
        BuildResult result = runner("checkFooApiCompatibility").buildAndFail();
        assertThat(result).output().contains("declared as accepted, but didn't match any rule");
    }

    private static final Pattern TYPE_NAME =
        Pattern.compile("(?:class|interface)\\s+(\\w+)");

    private void writeJavaSubproject(String name, String... sources) throws IOException {
        Path dir = Files.createDirectories(projectDir.resolve(name));
        Files.writeString(
            dir.resolve("build.gradle.kts"),
            """
            plugins { `java-library` }
            """
        );
        Path src = Files.createDirectories(dir.resolve("src/main/java"));
        for (String source : sources) {
            Matcher matcher = TYPE_NAME.matcher(source);
            assertWithMessage("no type declaration in source:\n%s", source)
                .that(matcher.find())
                .isTrue();
            Files.writeString(src.resolve(matcher.group(1) + ".java"), source);
        }
    }

    private void writeTwoJarCheck(String oldApi, String newApi) throws IOException {
        Files.writeString(
            projectDir.resolve("settings.gradle.kts"),
            """
            rootProject.name = "japicmp-test"
            include("old", "new")
            """
        );
        writeJavaSubproject("old", "public interface Marker {}", oldApi);
        writeJavaSubproject("new", "public interface Marker {}", newApi);
        Files.writeString(
            projectDir.resolve("build.gradle.kts"),
            """
            plugins { id("org.enginehub.crankcase.japicmp") }
            repositories { mavenCentral() }
            japicmp {
                addCheck("foo") {
                    dependencies {
                        oldClasspath(project(":old"))
                        newClasspath(project(":new"))
                    }
                }
            }
            """
        );
    }

    @Test
    void ignorableInterfaceRemovalIsSuppressed() throws IOException {
        writeTwoJarCheck("public class Api implements Marker {}", "public class Api {}");
        BuildResult result = runner("checkFooApiCompatibility").build();
        assertThat(result).task(":checkFooApiCompatibility").succeeded();
    }

    @Test
    void realBreakMixedWithIgnorableIsNotSuppressed() throws IOException {
        // Add both @Deprecated (ANNOTATION_DEPRECATED_ADDED, ignorable)
        // and final (CLASS_NOW_FINAL, a real break)
        writeTwoJarCheck("public class Api {}", "@Deprecated public final class Api {}");
        runner("checkFooApiCompatibility").buildAndFail();
        String report = Files.readString(
            projectDir.resolve("build/reports/api-compatibility-foo.html")
        );
        assertThat(report).contains("CLASS_NOW_FINAL");
    }

    private void writeInternalRemovalProjects() throws IOException {
        Files.writeString(
            projectDir.resolve("settings.gradle.kts"),
            """
            rootProject.name = "japicmp-test"
            include("old", "new")
            """
        );
        writeJavaSubproject(
            "old",
            "public interface Marker {}",
            """
            package com.example.internal;
            public class Gone {}
            """
        );
        writeJavaSubproject("new", "public interface Marker {}");
    }

    private void writeInternalRemovalRootCheck(String specBody) throws IOException {
        Files.writeString(
            projectDir.resolve("build.gradle.kts"),
            """
            plugins { id("org.enginehub.crankcase.japicmp") }
            repositories { mavenCentral() }
            japicmp {
                addCheck("foo") {
            %s
                    dependencies {
                        oldClasspath(project(":old"))
                        newClasspath(project(":new"))
                    }
                }
            }
            """.formatted(specBody)
        );
    }

    @Test
    void internalPackageRemovalFailsWithoutExclude() throws IOException {
        writeInternalRemovalProjects();
        writeInternalRemovalRootCheck("");
        runner("checkFooApiCompatibility").buildAndFail();
        String report = Files.readString(
            projectDir.resolve("build/reports/api-compatibility-foo.html")
        );
        assertThat(report).contains("Gone");
    }

    @Test
    void packageExcludeSuppressesInternalRemoval() throws IOException {
        writeInternalRemovalProjects();
        writeInternalRemovalRootCheck("        packageExcludes.add(\"com.example.internal\")");
        BuildResult result = runner("checkFooApiCompatibility").build();
        assertThat(result).task(":checkFooApiCompatibility").succeeded();
    }

    @Test
    void skipWhenOldClasspathMissingSkipsCheckForUnresolvableOldJar() throws IOException {
        Files.writeString(
            projectDir.resolve("build.gradle.kts"),
            """
            plugins { id("org.enginehub.crankcase.japicmp") }
            japicmp {
                addCheck("foo") {
                    skipWhenOldClasspathMissing = true
                    dependencies {
                        oldClasspath("org.jspecify:jspecify:99999.0.0")
                        newClasspath("org.jspecify:jspecify:1.0.0")
                    }
                }
            }
            """
        );
        BuildResult result = runner("checkFooApiCompatibility").build();
        assertThat(result).task(":checkFooApiCompatibility").skipped();
    }

    @Test
    void skipWhenOldClasspathMissingReadsValueSetAfterAddCheck() throws IOException {
        Files.writeString(
            projectDir.resolve("build.gradle.kts"),
            """
            plugins { id("org.enginehub.crankcase.japicmp") }
            val skipFlag = objects.property(Boolean::class.java)
            japicmp {
                addCheck("foo") {
                    skipWhenOldClasspathMissing.set(skipFlag)
                    dependencies {
                        oldClasspath("org.jspecify:jspecify:99999.0.0")
                        newClasspath("org.jspecify:jspecify:1.0.0")
                    }
                }
            }
            skipFlag.set(true)
            """
        );
        BuildResult result = runner("checkFooApiCompatibility").build();
        assertThat(result).task(":checkFooApiCompatibility").skipped();
    }

    @Test
    void unresolvableOldJarFailsWithoutSkipFlag() throws IOException {
        Files.writeString(
            projectDir.resolve("build.gradle.kts"),
            """
            plugins { id("org.enginehub.crankcase.japicmp") }
            japicmp {
                addCheck("foo") {
                    dependencies {
                        oldClasspath("org.jspecify:jspecify:99999.0.0")
                        newClasspath("org.jspecify:jspecify:1.0.0")
                    }
                }
            }
            """
        );
        runner("checkFooApiCompatibility").buildAndFail();
    }

    @Test
    void dependencyCollectorPopulatesResolvableConfiguration() throws IOException {
        writeBuildScriptWithFooCheck();
        BuildResult result = runner(
            "dependencies", "--configuration", "crankcaseJapicmpFooOldClasspath"
        ).build();
        assertThat(result).output().contains("org.jspecify:jspecify:1.0.0");
    }
}
