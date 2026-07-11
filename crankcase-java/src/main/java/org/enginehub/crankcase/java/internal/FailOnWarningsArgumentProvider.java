/*
 * SPDX-FileCopyrightText: EngineHub <https://www.enginehub.org/>
 * SPDX-License-Identifier: MPL-2.0
 */

package org.enginehub.crankcase.java.internal;

import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.process.CommandLineArgumentProvider;

import java.util.List;

public record FailOnWarningsArgumentProvider(
    Property<Boolean> failOnWarnings
) implements CommandLineArgumentProvider {
    @Input
    public Property<Boolean> getFailOnWarnings() {
        return failOnWarnings;
    }

    @Override
    public Iterable<String> asArguments() {
        return failOnWarnings.get() ? List.of("-Werror") : List.of();
    }
}
