/*
 * SPDX-FileCopyrightText: The Gradle team
 * SPDX-FileCopyrightText: EngineHub <https://www.enginehub.org/>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.enginehub.crankcase.japicmp.internal.accept;

import me.champeau.gradle.japicmp.report.ViolationCheckContext;

import java.util.Set;

public final class UserData {
    private static final String CHANGE_PARAMS = "changeParams";
    private static final String SEEN_API_CHANGES = "seenApiChanges";

    private UserData() {
    }

    public static ChangeParams getChangeParams(ViolationCheckContext context) {
        return context.getUserData(CHANGE_PARAMS);
    }

    public static Set<ApiChange> getSeenApiChanges(ViolationCheckContext context) {
        return context.getUserData(SEEN_API_CHANGES);
    }

    public static void putChangeParams(ViolationCheckContext context, ChangeParams params) {
        context.putUserData(CHANGE_PARAMS, params);
    }

    public static void putSeenApiChanges(ViolationCheckContext context, Set<ApiChange> seen) {
        context.putUserData(SEEN_API_CHANGES, seen);
    }
}
