/*
 * SPDX-FileCopyrightText: The Gradle team
 * SPDX-FileCopyrightText: EngineHub <https://www.enginehub.org/>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.enginehub.crankcase.japicmp.internal.accept;

import me.champeau.gradle.japicmp.report.SetupRule;
import me.champeau.gradle.japicmp.report.ViolationCheckContext;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Map;

public class AcceptingSetupRule implements SetupRule {
    private final Map<String, String> params;

    public AcceptingSetupRule(Map<String, String> params) {
        this.params = params;
    }

    public static Map<String, String> createParams(Path changeFile) {
        return Map.of(
            "changeFile", changeFile.toAbsolutePath().toString(),
            "fileName", changeFile.getFileName().toString()
        );
    }

    @Override
    public void execute(ViolationCheckContext t) {
        UserData.putChangeParams(t, new ChangeParams(
            ApiChanges.load(Paths.get(params.get("changeFile"))),
            params.get("fileName")
        ));
        UserData.putSeenApiChanges(t, new HashSet<>());
    }
}
