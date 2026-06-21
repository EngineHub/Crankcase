/*
 * SPDX-FileCopyrightText: EngineHub <https://www.enginehub.org/>
 * SPDX-License-Identifier: MPL-2.0
 */

package org.enginehub.crankcase.japicmp.internal;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@DisableCachingByDefault(because = "Always rewrites the change file")
public abstract class ResetAcceptedApiChangesFileTask extends DefaultTask {
    @SuppressWarnings("this-escape")
    public ResetAcceptedApiChangesFileTask() {
        setGroup("API Compatibility");
        getOutputs().upToDateWhen(_ -> false);
    }

    @OutputFile
    public abstract RegularFileProperty getChangeFile();

    @TaskAction
    public void resetChangeFile() {
        Path path = getChangeFile().getAsFile().get().toPath();
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(
                path,
                """
                {
                }
                """,
                StandardCharsets.UTF_8
            );
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
