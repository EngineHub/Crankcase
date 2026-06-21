/*
 * SPDX-FileCopyrightText: EngineHub <https://www.enginehub.org/>
 * SPDX-License-Identifier: MPL-2.0
 */

package org.enginehub.crankcase.japicmp.internal.accept;

import org.gradle.api.GradleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ApiChangesTest {
    @TempDir
    Path dir;

    @Test
    void missingFileReturnsEmpty() {
        assertThat(ApiChanges.load(dir.resolve("nope.json"))).isEmpty();
    }

    @Test
    void emptyFileThrowsErrorPointingAtResetTask() throws IOException {
        Path file = dir.resolve("accepted.json");
        Files.writeString(file, "   ");
        GradleException ex = assertThrows(GradleException.class, () -> ApiChanges.load(file));
        assertThat(ex.getMessage()).contains("reset");
    }
}
