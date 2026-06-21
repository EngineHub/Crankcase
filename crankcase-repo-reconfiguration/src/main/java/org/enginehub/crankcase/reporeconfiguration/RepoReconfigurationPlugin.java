/*
 * SPDX-FileCopyrightText: EngineHub <https://www.enginehub.org/>
 * SPDX-License-Identifier: MPL-2.0
 */

package org.enginehub.crankcase.reporeconfiguration;

import com.google.common.collect.ImmutableMap;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.artifacts.repositories.ArtifactRepository;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.api.artifacts.repositories.MavenRepositoryContentDescriptor;
import org.gradle.api.artifacts.repositories.UrlArtifactRepository;
import org.gradle.api.initialization.Settings;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class RepoReconfigurationPlugin implements Plugin<Settings> {

    private static final Logger LOGGER = Logging.getLogger("enginehub-reconfiguring-repositories");
    private static final String BASE = "https://repo.enginehub.org/";
    private static final String OVERRIDE_PROP = "crankcase.repoReconfiguration.baseOverride";

    private record RepoKey(URI url) {
        RepoKey {
            String s = url.toString();
            if (s.endsWith("/")) {
                url = URI.create(s.substring(0, s.length() - 1));
            }
            url = url.normalize();
        }

        static RepoKey of(String url) {
            return new RepoKey(URI.create(url));
        }
    }

    private record Reconfiguration(String suffix, @Nullable Action<MavenArtifactRepository> content) {
    }

    @Override
    public void apply(Settings settings) {
        String override = settings.getProviders().systemProperty(OVERRIDE_PROP).getOrNull();
        String effectiveBase = override != null ? override : BASE;
        List<String> allowedPrefixes = List.of(effectiveBase, "file:");
        Map<RepoKey, Reconfiguration> table = buildTable();
        Action<RepositoryHandler> mirror = repos -> repos.configureEach(
            repo -> reconfigure(repo, effectiveBase, allowedPrefixes, table)
        );
        Action<RepositoryHandler> requireAllowed = repos -> repos.configureEach(
            repo -> requireAllowedRepository(repo, allowedPrefixes)
        );

        settings.getGradle().getLifecycle().beforeProject(project -> {
            mirror.execute(project.getBuildscript().getRepositories());
            mirror.execute(project.getRepositories());
        });

        requireAllowed.execute(settings.getBuildscript().getRepositories());
        requireAllowed.execute(settings.getPluginManagement().getRepositories());
        mirror.execute(settings.getDependencyResolutionManagement().getRepositories());
    }

    private static void reconfigure(
        ArtifactRepository repo,
        String effectiveBase,
        List<String> allowedPrefixes,
        Map<RepoKey, Reconfiguration> table
    ) {
        if (!(repo instanceof UrlArtifactRepository urlRepo)) {
            return;
        }
        URI url = Objects.requireNonNull(
            urlRepo.getUrl(), "Repository URL is null for repository: " + repo.getName()
        );
        Reconfiguration reconfig = table.get(new RepoKey(url));
        String urlStr = url.toString();
        boolean mustReplace = allowedPrefixes.stream().noneMatch(urlStr::startsWith);
        if (mustReplace) {
            if (reconfig == null) {
                throw new IllegalStateException(
                    "No replacement found for non-EngineHub repository: " + repo.getName() + " " + url
                );
            }
            URI newUri = URI.create(effectiveBase + reconfig.suffix()).normalize();
            LOGGER.info("Replacing non-EngineHub repository: {} {} -> {}", repo.getName(), url, newUri);
            urlRepo.setUrl(newUri);
        }
        if (reconfig != null && reconfig.content() != null) {
            if (!(repo instanceof MavenArtifactRepository maven)) {
                throw new IllegalStateException(
                    "Cannot configure content on non-Maven repository: " + repo.getName() + " " + url
                );
            }
            reconfig.content().execute(maven);
        }
    }

    private static void requireAllowedRepository(ArtifactRepository repo, List<String> allowedPrefixes) {
        if (!(repo instanceof UrlArtifactRepository urlRepo)) {
            return;
        }
        URI url = Objects.requireNonNull(
            urlRepo.getUrl(), "Repository URL is null for repository: " + repo.getName()
        );
        String urlStr = url.toString();
        if (allowedPrefixes.stream().noneMatch(urlStr::startsWith)) {
            throw new IllegalStateException(
                "Settings buildscript and pluginManagement repositories may be used to resolve "
                    + "plugins before this plugin applies; declare an EngineHub repository manually "
                    + "instead: " + repo.getName() + " " + url
            );
        }
    }

    private static Reconfiguration reconfig(String suffix, Action<MavenArtifactRepository> content) {
        return new Reconfiguration(suffix, content);
    }

    private static ImmutableMap<RepoKey, Reconfiguration> buildTable() {
        ImmutableMap.Builder<RepoKey, Reconfiguration> table = ImmutableMap.builder();
        table.put(
            RepoKey.of("https://repo.maven.apache.org/maven2/"),
            reconfig(
                "/internal/maven-central-proxy/",
                maven -> maven.mavenContent(MavenRepositoryContentDescriptor::releasesOnly)
            )
        );
        table.put(
            RepoKey.of("https://plugins.gradle.org/m2/"),
            reconfig(
                "/internal/plugin-portal-proxy/",
                maven -> maven.mavenContent(MavenRepositoryContentDescriptor::releasesOnly)
            )
        );
        table.put(
            RepoKey.of("https://libraries.minecraft.net/"),
            reconfig(
                "/internal/minecraft/",
                maven -> maven.mavenContent(MavenRepositoryContentDescriptor::releasesOnly)
            )
        );
        table.put(
            RepoKey.of("https://maven.neoforged.net/releases/"),
            reconfig(
                "/internal/neoforged/",
                maven -> maven.mavenContent(content -> {
                    content.releasesOnly();
                    content.includeGroupAndSubgroups("net.minecraftforge");
                    content.includeGroupAndSubgroups("net.neoforged");
                })
            )
        );
        table.put(
            RepoKey.of("https://maven.minecraftforge.net/"),
            reconfig(
                "/internal/forge/",
                maven -> maven.mavenContent(content -> {
                    content.releasesOnly();
                    content.includeGroupAndSubgroups("net.minecraftforge");
                })
            )
        );
        table.put(
            RepoKey.of("https://maven.parchmentmc.org/"),
            reconfig(
                "/internal/parchment/",
                maven -> maven.mavenContent(content -> {
                    content.releasesOnly();
                    content.includeGroup("org.parchmentmc.data");
                })
            )
        );
        table.put(
            RepoKey.of("https://repo.papermc.io/repository/maven-public/"),
            reconfig(
                "/internal/papermc-proxy/",
                maven -> maven.mavenContent(content -> {
                    content.includeGroupAndSubgroups("io.papermc");
                    content.includeGroupAndSubgroups("com.velocitypowered");
                    content.includeGroupAndSubgroups("ca.spottedleaf");
                    content.includeGroupAndSubgroups("me.lucko");
                    content.includeModule("net.md-5", "bungeecord-chat");
                })
            )
        );
        table.put(
            RepoKey.of("https://maven.fabricmc.net/"),
            reconfig(
                "/internal/fabricmc/",
                maven -> maven.mavenContent(content -> {
                    content.releasesOnly();
                    content.includeGroupAndSubgroups("fabric-loom");
                    content.includeGroupAndSubgroups("net.fabricmc");
                    content.excludeModule("net.fabricmc", "yarn");
                })
            )
        );
        table.put(
            RepoKey.of("https://maven.fabricmc.net/#yarn-only"),
            reconfig(
                "/internal/fabricmc-yarn/",
                maven -> maven.mavenContent(content -> {
                    content.releasesOnly();
                    content.includeModule("net.fabricmc", "yarn");
                })
            )
        );
        table.put(
            RepoKey.of("https://repo.spongepowered.org/repository/maven-releases/"),
            reconfig(
                "/internal/spongepowered-releases/",
                maven -> maven.mavenContent(content -> {
                    content.releasesOnly();
                    content.includeGroupAndSubgroups("org.spongepowered");
                })
            )
        );
        table.put(
            RepoKey.of("https://repo.spongepowered.org/repository/maven-snapshots/"),
            reconfig(
                "/internal/spongepowered-snapshots/",
                maven -> maven.mavenContent(content -> {
                    content.snapshotsOnly();
                    content.includeGroupAndSubgroups("org.spongepowered");
                })
            )
        );
        table.put(
            RepoKey.of("https://repo.enginehub.org/libs-release/"),
            reconfig(
                "/libs-release/",
                maven -> {
                    maven.mavenContent(content -> {
                        content.releasesOnly();
                        content.includeGroupAndSubgroups("com.sk89q");
                        content.includeGroupAndSubgroups("org.enginehub");
                    });
                    maven.metadataSources(sources -> {
                        sources.gradleMetadata();
                        sources.mavenPom();
                        sources.artifact();
                    });
                }
            )
        );
        return table.build();
    }
}
