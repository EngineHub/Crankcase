/*
 * SPDX-FileCopyrightText: EngineHub <https://www.enginehub.org/>
 * SPDX-License-Identifier: MPL-2.0
 */

package org.enginehub.crankcase.japicmp;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.TaskProvider;

public abstract class JApiCmpPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.getPluginManager().apply("me.champeau.gradle.japicmp");
        project.getPluginManager().apply("base");

        TaskProvider<Task> checkApiCompatibility = project.getTasks().register("checkApiCompatibility", t -> {
            t.setGroup("API Compatibility");
            t.setDescription("Checks ALL API compatibility");
        });
        project.getTasks().register("resetAcceptedApiChangesFiles", t -> {
            t.setGroup("API Compatibility");
            t.setDescription("Resets ALL the accepted API changes files");
        });

        project.getTasks().named("check").configure(t -> t.dependsOn(checkApiCompatibility));

        project.getExtensions().create("japicmp", JApiCmpExtension.class, project);
    }
}
