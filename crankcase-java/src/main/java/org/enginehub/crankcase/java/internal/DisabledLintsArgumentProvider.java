/*
 * SPDX-FileCopyrightText: EngineHub <https://www.enginehub.org/>
 * SPDX-License-Identifier: MPL-2.0
 */

package org.enginehub.crankcase.java.internal;

import org.gradle.api.provider.ListProperty;
import org.gradle.api.tasks.Input;
import org.gradle.process.CommandLineArgumentProvider;

import java.util.ArrayList;
import java.util.List;

public record DisabledLintsArgumentProvider(
    ListProperty<String> disabledLints
) implements CommandLineArgumentProvider {
    @Input
    public ListProperty<String> getDisabledLints() {
        return disabledLints;
    }

    @Override
    public Iterable<String> asArguments() {
        List<String> lints = disabledLints.get();
        List<String> args = new ArrayList<>(lints.size());
        for (String lint : lints) {
            args.add("-Xlint:-" + lint);
        }
        return args;
    }
}
