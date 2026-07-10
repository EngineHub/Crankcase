/*
 * SPDX-FileCopyrightText: EngineHub <https://www.enginehub.org/>
 * SPDX-License-Identifier: MPL-2.0
 */

package org.enginehub.crankcase.checkstyle.internal;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;

interface CheckstyleConfigSpec {
    @Input
    Property<String> getDefaultContent();

    @Input
    Property<Integer> getJavaRelease();

    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.NONE)
    RegularFileProperty getSuppressions();

    @OutputDirectory
    DirectoryProperty getOutputDirectory();
}
