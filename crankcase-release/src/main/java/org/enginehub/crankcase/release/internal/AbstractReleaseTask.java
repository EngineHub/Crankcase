/*
 * SPDX-FileCopyrightText: EngineHub <https://www.enginehub.org/>
 * SPDX-License-Identifier: MPL-2.0
 */

package org.enginehub.crankcase.release.internal;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.Internal;
import org.gradle.process.ExecOperations;
import org.gradle.work.DisableCachingByDefault;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;

@DisableCachingByDefault(because = "Always rewrites the version and runs git")
public abstract class AbstractReleaseTask extends DefaultTask {
    private static final String VERSION_KEY = "version";

    @SuppressWarnings("this-escape")
    public AbstractReleaseTask() {
        setGroup("Release Tasks");
        getOutputs().upToDateWhen(_ -> false);
    }

    @Inject
    public abstract ExecOperations getExecOperations();

    @Internal
    public abstract RegularFileProperty getPropertiesFile();

    @Internal
    public abstract DirectoryProperty getWorkingDirectory();

    protected String readVersion() {
        File file = getPropertiesFile().getAsFile().get();
        for (String line : readLines(file)) {
            String value = versionValue(line);
            if (value != null) {
                return value;
            }
        }
        throw new IllegalStateException("No '" + VERSION_KEY + "' property found in " + file);
    }

    protected void writeVersion(String newVersion) {
        File file = getPropertiesFile().getAsFile().get();
        List<String> lines = readLines(file);
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (versionValue(line) != null) {
                int separator = line.indexOf('=');
                lines.set(i, line.substring(0, separator + 1) + newVersion);
                writeLines(file, lines);
                return;
            }
        }
        throw new IllegalStateException("No '" + VERSION_KEY + "' property found in " + file);
    }

    protected void git(String... args) {
        File workingDir = getWorkingDirectory().get().getAsFile();
        List<String> command = new ArrayList<>();
        command.add("git");
        Collections.addAll(command, args);
        getExecOperations().exec(spec -> {
            spec.setWorkingDir(workingDir);
            spec.commandLine(command);
        });
    }

    private static @Nullable String versionValue(String line) {
        String leading = line.stripLeading();
        if (leading.startsWith("#") || leading.startsWith("!")) {
            return null;
        }
        int separator = line.indexOf('=');
        if (separator < 0 || !line.substring(0, separator).trim().equals(VERSION_KEY)) {
            return null;
        }
        return line.substring(separator + 1).trim();
    }

    private static List<String> readLines(File file) {
        if (!file.isFile()) {
            throw new IllegalStateException(file + " does not exist");
        }
        try {
            return new ArrayList<>(List.of(Files.readString(file.toPath()).split("\n", -1)));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void writeLines(File file, List<String> lines) {
        try {
            Files.writeString(file.toPath(), String.join("\n", lines));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
