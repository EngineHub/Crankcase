/*
 * SPDX-FileCopyrightText: EngineHub <https://www.enginehub.org/>
 * SPDX-License-Identifier: MPL-2.0
 */

package org.enginehub.crankcase.checkstyle.internal;

import java.util.List;
import java.util.stream.Collectors;

final class JavaVersionGatedModules {
    private static final int MIN_UNNAMED_RELEASE = 22;
    private static final List<String> UNNAMED_MODULES = List.of(
        "UnusedCatchParameterShouldBeUnnamed",
        "UnusedLambdaParameterShouldBeUnnamed",
        "UnusedTryResourceShouldBeUnnamed"
    );

    static String render(int javaRelease) {
        if (javaRelease < MIN_UNNAMED_RELEASE) {
            return "";
        }
        return UNNAMED_MODULES.stream()
            .map(name -> "<module name=\"" + name + "\"/>")
            .collect(Collectors.joining("\n        "));
    }

    private JavaVersionGatedModules() {
    }
}
