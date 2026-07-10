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

    private void writeJavaPluginBuild(String extraConfig) throws IOException {
        Files.writeString(
            projectDir.resolve("build.gradle.kts"),
            """
            plugins {
                id("org.enginehub.crankcase.java")
                id("org.enginehub.crankcase.checkstyle")
            }
            repositories { mavenCentral() }
            %s
            """.formatted(extraConfig)
        );
    }

    private void writeUnusedLocalVariableSource() throws IOException {
        Path source = projectDir.resolve("src/main/java/example/Example.java");
        Files.createDirectories(source.getParent());
        Files.writeString(
            source,
            """
            package example;

            class Example {
                boolean check() {
                    int unused = 1;
                    return true;
                }
            }
            """
        );
    }

    private void writeUnusedPatternVariableSource() throws IOException {
        Path source = projectDir.resolve("src/main/java/example/Example.java");
        Files.createDirectories(source.getParent());
        Files.writeString(
            source,
            """
            package example;

            class Example {
                String check(Object value) {
                    return switch (value) {
                        case String text -> "string";
                        default -> "other";
                    };
                }
            }
            """
        );
    }

    private void writeUnusedCatchParameterSource() throws IOException {
        Path source = projectDir.resolve("src/main/java/example/Example.java");
        Files.createDirectories(source.getParent());
        Files.writeString(
            source,
            """
            package example;

            class Example {
                boolean check(String value) {
                    try {
                        return Boolean.parseBoolean(value);
                    } catch (RuntimeException e) {
                        return false;
                    }
                }
            }
            """
        );
    }

    private String generatedConfig() throws IOException {
        return Files.readString(projectDir.resolve("build/crankcase/checkstyle/checkstyle.xml"));
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
        assertThat(result).output().contains("checkstyle-version=13.7.0");
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

    @Test
    void configurationCacheIsReusedWithJavaRelease() throws IOException {
        writeBuild("crankcaseCheckstyle { javaRelease = 22 }");
        writeCleanSource();
        CrankcaseTestKit.assertConfigCacheStoredThenReused(
            runner("checkstyleMain", "--configuration-cache")
        );
    }

    @Test
    void javaReleaseBelow21Fails() throws IOException {
        writeBuild("crankcaseCheckstyle { javaRelease = 17 }");
        writeCleanSource();
        BuildResult result = runner("crankcasePrepareCheckstyleConfig").buildAndFail();
        assertThat(result).output()
            .contains("Crankcase requires Java 21 or later, but javaRelease is 17");
    }

    @Test
    void javaReleaseDefaultsTo21() throws IOException {
        writeBuild("");
        writeCleanSource();
        runner("crankcasePrepareCheckstyleConfig").build();

        String config = generatedConfig();
        assertThat(config).contains("value=\"21\"");
        assertThat(config).doesNotContain("UnusedCatchParameterShouldBeUnnamed");
        assertThat(config).doesNotContain("UnusedLambdaParameterShouldBeUnnamed");
        assertThat(config).doesNotContain("UnusedTryResourceShouldBeUnnamed");
    }

    @Test
    void unusedLocalVariableIsReported() throws IOException {
        writeBuild("");
        writeUnusedLocalVariableSource();
        BuildResult result = runner("checkstyleMain").buildAndFail();
        assertThat(result).task(":checkstyleMain").failed();
        assertThat(result).output().contains("Unused named local variable 'unused'");
    }

    @Test
    void unusedNamedPatternVariableIsIgnoredBelowJava22() throws IOException {
        writeBuild("");
        writeUnusedPatternVariableSource();
        BuildResult result = runner("checkstyleMain").build();
        assertThat(result).task(":checkstyleMain").succeeded();
    }

    @Test
    void unusedNamedPatternVariableIsReportedFromJava22() throws IOException {
        writeBuild("crankcaseCheckstyle { javaRelease = 22 }");
        writeUnusedPatternVariableSource();
        BuildResult result = runner("checkstyleMain").buildAndFail();
        assertThat(result).task(":checkstyleMain").failed();
        assertThat(result).output().contains("Unused named local variable 'text'");
    }

    @Test
    void unnamedVariableChecksAppearOnlyFromJava22() throws IOException {
        writeBuild("crankcaseCheckstyle { javaRelease = 22 }");
        writeUnusedCatchParameterSource();
        BuildResult result = runner("checkstyleMain").buildAndFail();
        assertThat(result).task(":checkstyleMain").failed();
        assertThat(result).output().contains("Unused catch parameter");

        String config = generatedConfig();
        assertThat(config).contains("UnusedCatchParameterShouldBeUnnamed");
        assertThat(config).contains("UnusedLambdaParameterShouldBeUnnamed");
        assertThat(config).contains("UnusedTryResourceShouldBeUnnamed");
    }

    @Test
    void unusedCatchParameterIsIgnoredBelowJava22() throws IOException {
        writeBuild("");
        writeUnusedCatchParameterSource();
        BuildResult result = runner("checkstyleMain").build();
        assertThat(result).task(":checkstyleMain").succeeded();
    }

    @Test
    void javaReleaseIsWiredFromCrankcaseJava() throws IOException {
        writeJavaPluginBuild("crankcaseJava { javaRelease = 22 }");
        writeCleanSource();
        runner("crankcasePrepareCheckstyleConfig").build();

        String config = generatedConfig();
        assertThat(config).contains("value=\"22\"");
        assertThat(config).contains("UnusedCatchParameterShouldBeUnnamed");
    }

    @Test
    void javaReleaseFromCrankcaseJavaBelow21Fails() throws IOException {
        writeJavaPluginBuild("crankcaseJava { javaRelease = 17 }");
        writeCleanSource();
        BuildResult result = runner("crankcasePrepareCheckstyleConfig").buildAndFail();
        assertThat(result).output()
            .contains("Crankcase requires Java 21 or later, but javaRelease is 17");
    }
}
