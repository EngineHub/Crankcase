/*
 * SPDX-FileCopyrightText: The Gradle team
 * SPDX-FileCopyrightText: EngineHub <https://www.enginehub.org/>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.enginehub.crankcase.japicmp.internal.accept;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.gradle.api.GradleException;
import org.jspecify.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ApiChanges {
    // @Nullable because Gson.fromJson doesn't report null as a possible return type,
    // but it can return null if the file is empty or malformed.
    public static final TypeToken<@Nullable Map<String, List<ApiChange>>> TYPE_TOKEN = new TypeToken<>() {};

    private ApiChanges() {
    }

    public static Map<ApiChange, String> load(Path path) {
        if (!Files.exists(path)) {
            return Map.of();
        }
        Map<String, List<ApiChange>> fromDisk;
        IOException cause = null;
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            fromDisk = new Gson().fromJson(reader, TYPE_TOKEN);
        } catch (IOException e) {
            fromDisk = null;
            cause = e;
        }
        if (fromDisk == null) {
            throw new GradleException(
                "Accepted API changes file " + path + " is empty or malformed. "
                    + "Run the matching reset task (e.g. resetAcceptedApiChangesFiles) "
                    + "to recreate it.",
                cause
            );
        }
        var changeToReason = new HashMap<ApiChange, String>();
        fromDisk.forEach((reason, changes) -> {
            for (ApiChange change : changes) {
                changeToReason.put(change, reason);
            }
        });
        return changeToReason;
    }
}
