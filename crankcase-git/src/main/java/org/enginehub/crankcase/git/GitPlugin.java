/*
 * SPDX-FileCopyrightText: EngineHub <https://www.enginehub.org/>
 * SPDX-License-Identifier: MPL-2.0
 */

package org.enginehub.crankcase.git;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.provider.Provider;

import java.io.File;

public abstract class GitPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        File workingDir = project.getRootDir();
        Provider<GitBuildService> svc = project.getGradle().getSharedServices()
            .registerIfAbsent("git", GitBuildService.class, spec ->
                spec.getParameters().getWorkingDirectory().set(workingDir)
            );
        project.getExtensions().create("git", GitExtension.class, svc);
    }
}
