/*
 * SPDX-FileCopyrightText: EngineHub <https://www.enginehub.org/>
 * SPDX-License-Identifier: MPL-2.0
 */

package org.enginehub.crankcase.release.internal;

import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

@DisableCachingByDefault(because = "Always rewrites the version and runs git")
public abstract class ChangeSnapshotToReleaseTask extends AbstractReleaseTask {
    @TaskAction
    public void changeSnapshotToRelease() {
        String version = readVersion();
        if (!version.endsWith("-SNAPSHOT")) {
            throw new IllegalStateException("Version is not a snapshot version");
        }
        String newVersion = version.substring(0, version.length() - "-SNAPSHOT".length());
        writeVersion(newVersion);

        String propertiesPath = getPropertiesFile().getAsFile().get().getAbsolutePath();
        git("commit", "-m", "Release version " + newVersion, propertiesPath);
        git("tag", "v" + newVersion, "-m", "Release version " + newVersion);
    }
}
