/*
 * SPDX-FileCopyrightText: EngineHub <https://www.enginehub.org/>
 * SPDX-License-Identifier: MPL-2.0
 */

package org.enginehub.crankcase.release.internal;

import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import java.util.Arrays;

@DisableCachingByDefault(because = "Always rewrites the version and runs git")
public abstract class ChangeReleaseToNextSnapshotTask extends AbstractReleaseTask {
    @TaskAction
    public void changeReleaseToNextSnapshot() {
        String version = readVersion();
        if (version.endsWith("-SNAPSHOT")) {
            throw new IllegalStateException("Version is already a snapshot version");
        }
        String[] parts = version.split("\\.");
        int nextLast = Integer.parseInt(parts[parts.length - 1]) + 1;
        String prefix = String.join(".", Arrays.copyOf(parts, parts.length - 1));
        String newVersion = prefix + "." + nextLast + "-SNAPSHOT";
        writeVersion(newVersion);

        String propertiesPath = getPropertiesFile().getAsFile().get().getAbsolutePath();
        git("commit", "-m", "Switch to next snapshot version " + newVersion, propertiesPath);
    }
}
