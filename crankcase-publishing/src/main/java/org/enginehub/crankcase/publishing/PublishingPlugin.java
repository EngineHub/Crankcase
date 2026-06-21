/*
 * SPDX-FileCopyrightText: EngineHub <https://www.enginehub.org/>
 * SPDX-License-Identifier: MPL-2.0
 */

package org.enginehub.crankcase.publishing;

import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.publish.PublishingExtension;

import java.util.concurrent.Callable;
import javax.inject.Inject;

public abstract class PublishingPlugin implements Plugin<Project> {
    @Inject
    protected abstract ProviderFactory getProviders();

    @Override
    public void apply(Project project) {
        project.getPluginManager().apply("maven-publish");

        String contextUrl = getProviders().gradleProperty("artifactory_contextUrl")
            .getOrElse("https://localhost");
        Provider<String> user = getProviders().gradleProperty("artifactory_user");
        Provider<String> password = getProviders().gradleProperty("artifactory_password");

        // The repository URL is resolved lazily so applying the plugin never reads `project.version`
        // eagerly (which is unset while Gradle generates precompiled-script-plugin accessors).
        Callable<String> repoUrl = () -> {
            String version = project.getVersion().toString();
            if ("unspecified".equals(version)) {
                throw new GradleException(
                    "crankcase-publishing requires `project.version` to be set before publishing."
                );
            }
            String repoPath = version.contains("SNAPSHOT") ? "libs-snapshot-local" : "libs-release-local";
            return contextUrl + "/" + repoPath;
        };

        project.getExtensions().configure(PublishingExtension.class, publishing ->
            publishing.getRepositories().maven(maven -> {
                maven.setName("EngineHub");
                maven.setUrl(repoUrl);
                maven.credentials(credentials -> {
                    credentials.setUsername(user.getOrNull());
                    credentials.setPassword(password.getOrNull());
                });
            })
        );
    }
}
