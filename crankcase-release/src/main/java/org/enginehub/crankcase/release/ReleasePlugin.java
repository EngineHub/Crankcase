/*
 * SPDX-FileCopyrightText: EngineHub <https://www.enginehub.org/>
 * SPDX-License-Identifier: MPL-2.0
 */

package org.enginehub.crankcase.release;

import org.enginehub.crankcase.release.internal.AbstractReleaseTask;
import org.enginehub.crankcase.release.internal.ChangeReleaseToNextSnapshotTask;
import org.enginehub.crankcase.release.internal.ChangeSnapshotToReleaseTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class ReleasePlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.getTasks().register(
            "changeSnapshotToRelease",
            ChangeSnapshotToReleaseTask.class,
            task -> {
                task.setDescription(
                    "Change the version from a snapshot to a release version, commit it, and tag it"
                );
                configure(project, task);
            }
        );
        project.getTasks().register(
            "changeReleaseToNextSnapshot",
            ChangeReleaseToNextSnapshotTask.class,
            task -> {
                task.setDescription(
                    "Change the version from a release to the next snapshot version and commit it"
                );
                configure(project, task);
            }
        );
    }

    private static void configure(Project project, AbstractReleaseTask task) {
        task.getPropertiesFile().set(project.file("gradle.properties"));
        task.getWorkingDirectory().set(project.getProjectDir());
    }
}
