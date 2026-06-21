/*
 * SPDX-FileCopyrightText: The Gradle team
 * SPDX-FileCopyrightText: EngineHub <https://www.enginehub.org/>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.enginehub.crankcase.japicmp.internal.accept;

import japicmp.model.JApiClass;
import japicmp.model.JApiCompatibility;
import japicmp.model.JApiCompatibilityChange;
import japicmp.model.JApiCompatibilityChangeType;
import japicmp.model.JApiImplementedInterface;
import me.champeau.gradle.japicmp.report.Violation;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class BinaryCompatRule extends AbstractAcceptingRule {
    private static final List<JApiCompatibilityChangeType> IGNORED_CHANGE_TYPES = List.of(
        JApiCompatibilityChangeType.METHOD_REMOVED_IN_SUPERCLASS,
        JApiCompatibilityChangeType.INTERFACE_REMOVED,
        JApiCompatibilityChangeType.INTERFACE_ADDED,
        JApiCompatibilityChangeType.ANNOTATION_DEPRECATED_ADDED
    );

    @Override
    public @Nullable Violation maybeViolation(JApiCompatibility member) {
        if (member.isBinaryCompatible()) {
            return null;
        }
        if (member instanceof JApiClass apiClass && apiClass.getCompatibilityChanges().isEmpty()) {
            return null;
        }
        if (member instanceof JApiImplementedInterface) {
            return null;
        }
        List<JApiCompatibilityChange> changes = member.getCompatibilityChanges();
        if (!changes.isEmpty()
            && changes.stream().allMatch(c -> IGNORED_CHANGE_TYPES.contains(c.getType()))) {
            return null;
        }
        List<String> changeNames = member.getCompatibilityChanges().stream()
            .map(c -> c.getType().name())
            .toList();
        return checkAcceptance(member, changeNames, Violation.notBinaryCompatible(member));
    }
}
