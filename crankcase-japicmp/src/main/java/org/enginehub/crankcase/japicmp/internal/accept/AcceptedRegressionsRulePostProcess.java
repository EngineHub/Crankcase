/*
 * SPDX-FileCopyrightText: The Gradle team
 * SPDX-FileCopyrightText: EngineHub <https://www.enginehub.org/>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.enginehub.crankcase.japicmp.internal.accept;

import me.champeau.gradle.japicmp.report.PostProcessViolationsRule;
import me.champeau.gradle.japicmp.report.ViolationCheckContextWithViolations;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class AcceptedRegressionsRulePostProcess implements PostProcessViolationsRule {
    @Override
    public void execute(ViolationCheckContextWithViolations context) {
        ChangeParams changeParams = UserData.getChangeParams(context);
        Set<ApiChange> seenApiChanges = UserData.getSeenApiChanges(context);
        var left = new HashSet<>(changeParams.changeToReason().keySet());
        left.removeAll(seenApiChanges);
        if (!left.isEmpty()) {
            String formatted = left.stream().map(Object::toString).collect(Collectors.joining("\n"));
            throw new RuntimeException(
                """
                The following regressions are declared as accepted, but didn't match any rule:
                %s
                """.formatted(formatted)
            );
        }
    }
}
