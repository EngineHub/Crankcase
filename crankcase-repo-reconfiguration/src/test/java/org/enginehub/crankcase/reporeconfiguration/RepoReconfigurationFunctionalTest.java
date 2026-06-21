/*
 * SPDX-FileCopyrightText: EngineHub <https://www.enginehub.org/>
 * SPDX-License-Identifier: MPL-2.0
 */

package org.enginehub.crankcase.reporeconfiguration;

import com.google.common.collect.ImmutableList;
import org.enginehub.crankcase.testsupport.CrankcaseTestKit;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.enginehub.crankcase.testsupport.BuildResultSubject.assertThat;

class RepoReconfigurationFunctionalTest {

    private static String withAllowedPluginManagement(String rest) {
        return
            """
            pluginManagement {
                repositories {
                    maven {
                        url = uri(providers.systemProperty("crankcase.repoReconfiguration.baseOverride").get())
                    }
                }
            }
            """ + rest;
    }

    private static final String PLUGIN_ONLY_SETTINGS = withAllowedPluginManagement(
        """
        plugins {
            id("org.enginehub.crankcase.repo-reconfiguration")
        }
        rootProject.name = "consumer"
        """
    );

    private static final String PLUGIN_AND_DRM_SETTINGS = withAllowedPluginManagement(
        """
        plugins {
            id("org.enginehub.crankcase.repo-reconfiguration")
        }
        dependencyResolutionManagement {
            repositories {
                maven {
                    url = uri("https://repo.maven.apache.org/maven2/")
                }
            }
        }
        rootProject.name = "consumer"
        """
    );

    private static final String PROJECT_REPOS_BLOCK =
        """
        repositories {
            maven {
                url = uri("https://repo.maven.apache.org/maven2/")
            }
        }
        """;

    private static final String RESOLVE_CONFIG_TIME =
        """
        val cfg = configurations.create("probe")
        dependencies {
            add("probe", "%s")
        }
        val names = cfg.incoming.files.files.map { it.name }
        tasks.register("resolve") {
            doLast {
                println("RESOLVED=" + names)
            }
        }
        """.formatted(LocalMirror.DEFAULT_NOTATION);

    private static final String RESOLVE_EXEC_TIME =
        """
        val cfg = configurations.create("probe")
        dependencies {
            add("probe", "%s")
        }
        tasks.register("resolve") {
            val files = cfg.incoming.files
            doLast {
                println("RESOLVED=" + files.files.map { it.name })
            }
        }
        """.formatted(LocalMirror.DEFAULT_NOTATION);

    private enum ResolutionTime {
        CONFIG_TIME,
        EXEC_TIME;

        String buildBlock() {
            return switch (this) {
                case CONFIG_TIME -> RESOLVE_CONFIG_TIME;
                case EXEC_TIME -> RESOLVE_EXEC_TIME;
            };
        }
    }

    @TempDir
    Path projectDir;
    @TempDir
    Path mirrorDir;

    @ParameterizedTest
    @EnumSource
    void projectRepositories(ResolutionTime resolutionTime) throws IOException {
        assertResolvesThroughMirror(PLUGIN_ONLY_SETTINGS, PROJECT_REPOS_BLOCK + resolutionTime.buildBlock());
    }

    @ParameterizedTest
    @EnumSource
    void dependencyResolutionManagement(ResolutionTime resolutionTime) throws IOException {
        assertResolvesThroughMirror(PLUGIN_AND_DRM_SETTINGS, resolutionTime.buildBlock());
    }

    @Test
    void buildscriptClasspathResolvesThroughMirror() throws IOException {
        LocalMirror mirror = new LocalMirror(mirrorDir, "internal/maven-central-proxy");
        mirror.putArtifact();
        String build =
            """
            buildscript {
                repositories {
                    maven {
                        url = uri("https://repo.maven.apache.org/maven2/")
                    }
                }
                dependencies {
                    classpath("%s")
                }
            }
            tasks.register("verify")
            """.formatted(LocalMirror.DEFAULT_NOTATION);
        write(PLUGIN_ONLY_SETTINGS, build);

        BuildResult result = runner(mirror.baseUri(), "verify").build();

        assertThat(result).task(":verify").upToDate();
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://repo.maven.apache.org/maven2", "https://repo.maven.apache.org/maven2/"})
    void matchesMavenCentral(String mavenCentral) throws IOException {
        assertRepositoryMatchesMirror(mavenCentral, "internal/maven-central-proxy");
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://plugins.gradle.org/m2", "https://plugins.gradle.org/m2/"})
    void matchesPluginPortal(String pluginPortal) throws IOException {
        assertRepositoryMatchesMirror(pluginPortal, "internal/plugin-portal-proxy");
    }

    @Test
    void fragmentSelectsYarnMirror() throws IOException {
        LocalMirror mirror = new LocalMirror(mirrorDir, "internal/fabricmc-yarn");
        mirror.putArtifact("net.fabricmc", "yarn", "1.0");
        String build =
            """
            repositories {
                maven {
                    name = "fabric"
                    url = uri("https://maven.fabricmc.net/")
                }
                maven {
                    name = "fabricYarn"
                    url = uri("https://maven.fabricmc.net/#yarn-only")
                }
            }
            val cfg = configurations.create("probe")
            dependencies {
                add("probe", "net.fabricmc:yarn:1.0")
            }
            tasks.register("resolve") {
                val files = cfg.incoming.files
                doLast {
                    println("RESOLVED=" + files.files.map { it.name })
                }
            }
            """;
        write(PLUGIN_ONLY_SETTINGS, build);

        BuildResult result = runner(mirror.baseUri(), "resolve", "-q").build();

        assertThat(result).output().contains("RESOLVED=[yarn-1.0.jar]");
    }

    @Test
    void failsWithoutYarnFragmentRepository() throws IOException {
        LocalMirror mirror = new LocalMirror(mirrorDir, "internal/fabricmc-yarn");
        mirror.putArtifact("net.fabricmc", "yarn", "1.0");
        String build =
            """
            repositories {
                maven {
                    name = "fabric"
                    url = uri("https://maven.fabricmc.net/")
                }
            }
            val cfg = configurations.create("probe")
            dependencies {
                add("probe", "net.fabricmc:yarn:1.0")
            }
            tasks.register("resolve") {
                val files = cfg.incoming.files
                doLast {
                    println("RESOLVED=" + files.files.map { it.name })
                }
            }
            """;
        write(PLUGIN_ONLY_SETTINGS, build);

        BuildResult result = runner(mirror.baseUri(), "resolve").buildAndFail();

        assertThat(result).output().contains("Could not find net.fabricmc:yarn:1.0");
    }

    @Test
    void contentFilterExcludesModule() throws IOException {
        LocalMirror mirror = new LocalMirror(mirrorDir, "internal/fabricmc");
        mirror.putArtifact("net.fabricmc", "yarn", "1.0");
        String build =
            """
            repositories {
                maven {
                    url = uri("https://maven.fabricmc.net/")
                }
            }
            val cfg = configurations.create("probe")
            dependencies {
                add("probe", "net.fabricmc:yarn:1.0")
            }
            tasks.register("resolve") {
                val files = cfg.incoming.files
                doLast {
                    println("RESOLVED=" + files.files.map { it.name })
                }
            }
            """;
        write(PLUGIN_ONLY_SETTINGS, build);

        BuildResult result = runner(mirror.baseUri(), "resolve").buildAndFail();

        assertThat(result).output().contains("Could not find net.fabricmc:yarn:1.0");
    }

    @Test
    void unknownRepositoryFails() throws IOException {
        LocalMirror mirror = new LocalMirror(mirrorDir);
        String build =
            """
            repositories {
                maven {
                    url = uri("https://nope.example.invalid/repo/")
                }
            }
            tasks.register("noop") {
            }
            """;
        write(PLUGIN_ONLY_SETTINGS, build);

        BuildResult result = runner(mirror.baseUri(), "noop").buildAndFail();

        assertThat(result).output().contains("No replacement found for non-EngineHub repository");
    }

    @Test
    void settingsBuildscriptHookRejectsUnknownRepository() throws IOException {
        // The settings buildscript classpath resolves before this plugin applies, so a post-swap
        // success resolution isn't achievable here. Instead prove the hook is installed: declare
        // an unmapped repository after the plugin applies and assert it is rejected at config time.
        String settings = withAllowedPluginManagement(
            """
            plugins {
                id("org.enginehub.crankcase.repo-reconfiguration")
            }
            settings.buildscript.repositories {
                maven {
                    url = uri("https://nope.example.invalid/repo/")
                }
            }
            rootProject.name = "consumer"
            """
        );
        String build =
            """
            tasks.register("noop") {
            }
            """;
        write(settings, build);

        LocalMirror mirror = new LocalMirror(mirrorDir);
        BuildResult result = runner(mirror.baseUri(), "noop").buildAndFail();

        assertThat(result).output().contains("declare an EngineHub repository manually");
    }

    @Test
    void settingsWithoutPluginManagementRepositoriesFailsByDefault() throws IOException {
        // With no pluginManagement repositories configured, Gradle injects the default plugin
        // portal into the settings buildscript.
        String settings =
            """
            plugins {
                id("org.enginehub.crankcase.repo-reconfiguration")
            }
            rootProject.name = "consumer"
            """;
        String build =
            """
            tasks.register("noop") {
            }
            """;
        write(settings, build);

        LocalMirror mirror = new LocalMirror(mirrorDir);
        BuildResult result = runner(mirror.baseUri(), "noop").buildAndFail();

        assertThat(result).output().contains("declare an EngineHub repository manually");
    }

    @Test
    void settingsBuildscriptRejectsMappableRepository() throws IOException {
        // We cannot apply early enough to catch this, so ensure the user handles it.
        String settings = withAllowedPluginManagement(
            """
            plugins {
                id("org.enginehub.crankcase.repo-reconfiguration")
            }
            settings.buildscript.repositories {
                maven {
                    url = uri("https://repo.maven.apache.org/maven2/")
                }
            }
            rootProject.name = "consumer"
            """
        );
        String build =
            """
            tasks.register("noop") {
            }
            """;
        write(settings, build);

        LocalMirror mirror = new LocalMirror(mirrorDir);
        BuildResult result = runner(mirror.baseUri(), "noop").buildAndFail();

        assertThat(result).output().contains("declare an EngineHub repository manually");
    }

    @Test
    void settingsBuildscriptAllowsManualEngineHubRepository() throws IOException {
        LocalMirror mirror = new LocalMirror(mirrorDir);
        String settings = withAllowedPluginManagement(
            """
            plugins {
                id("org.enginehub.crankcase.repo-reconfiguration")
            }
            settings.buildscript.repositories {
                maven {
                    url = uri(providers.systemProperty("crankcase.repoReconfiguration.baseOverride").get())
                }
            }
            rootProject.name = "consumer"
            """
        );
        String build =
            """
            tasks.register("noop") {
            }
            """;
        write(settings, build);

        BuildResult result = runner(mirror.baseUri(), "noop").build();

        assertThat(result).task(":noop").upToDate();
    }

    @Test
    void pluginManagementRejectsMappableRepository() throws IOException {
        // Unfortunately, pluginManagement is injected into settings repos, so we need to hard enforce on it too
        String settings =
            """
            pluginManagement {
                repositories {
                    gradlePluginPortal()
                }
            }
            plugins {
                id("org.enginehub.crankcase.repo-reconfiguration")
            }
            rootProject.name = "consumer"
            """;
        String build =
            """
            tasks.register("noop") {
            }
            """;
        write(settings, build);

        LocalMirror mirror = new LocalMirror(mirrorDir);
        BuildResult result = runner(mirror.baseUri(), "noop").buildAndFail();

        assertThat(result).output().contains("declare an EngineHub repository manually");
    }

    @Test
    void pluginManagementAllowsManualEngineHubRepository() throws IOException {
        LocalMirror mirror = new LocalMirror(mirrorDir);
        String build =
            """
            tasks.register("noop") {
            }
            """;
        write(PLUGIN_ONLY_SETTINGS, build);

        BuildResult result = runner(mirror.baseUri(), "noop").build();

        assertThat(result).task(":noop").upToDate();
    }

    @Test
    void configurationCacheIsReused() throws IOException {
        LocalMirror mirror = new LocalMirror(mirrorDir, "internal/maven-central-proxy");
        mirror.putArtifact();
        write(PLUGIN_ONLY_SETTINGS, PROJECT_REPOS_BLOCK + RESOLVE_EXEC_TIME);

        CrankcaseTestKit.assertConfigCacheStoredThenReused(
            runner(mirror.baseUri(), "resolve", "--configuration-cache")
        );
    }

    @Test
    void resolutionUsesSwappedMirrorLocation() throws IOException {
        LocalMirror mirror = new LocalMirror(mirrorDir);
        write(PLUGIN_ONLY_SETTINGS, PROJECT_REPOS_BLOCK + RESOLVE_EXEC_TIME);

        BuildResult result = runner(mirror.baseUri(), "resolve").buildAndFail();

        assertThat(result).output().contains("internal/maven-central-proxy");
    }

    private void assertResolvesThroughMirror(String settings, String build) throws IOException {
        LocalMirror mirror = new LocalMirror(mirrorDir, "internal/maven-central-proxy");
        mirror.putArtifact();
        write(settings, build);

        BuildResult result = runner(mirror.baseUri(), "resolve", "-q").build();

        assertThat(result).output().contains("RESOLVED=[" + LocalMirror.DEFAULT_JAR + "]");
    }

    private void assertRepositoryMatchesMirror(String repoUrl, String mirrorSubpath) throws IOException {
        LocalMirror mirror = new LocalMirror(mirrorDir, mirrorSubpath);
        mirror.putArtifact();
        String build =
            """
            repositories {
                maven {
                    url = uri("%s")
                }
            }
            """.formatted(repoUrl) + RESOLVE_EXEC_TIME;
        write(PLUGIN_ONLY_SETTINGS, build);

        BuildResult result = runner(mirror.baseUri(), "resolve", "-q").build();

        assertThat(result).output().contains("RESOLVED=[" + LocalMirror.DEFAULT_JAR + "]");
    }

    private void write(String settings, String build) throws IOException {
        Files.writeString(projectDir.resolve("settings.gradle.kts"), settings);
        Files.writeString(projectDir.resolve("build.gradle.kts"), build);
    }

    private GradleRunner runner(String override, String... args) {
        return CrankcaseTestKit.runner(
            projectDir,
            ImmutableList.<String>builder()
                .add(args)
                .add("-Dcrankcase.repoReconfiguration.baseOverride=" + override)
                .build()
        );
    }
}
