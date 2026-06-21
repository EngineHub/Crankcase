/*
 * SPDX-FileCopyrightText: EngineHub <https://www.enginehub.org/>
 * SPDX-License-Identifier: MPL-2.0
 */

package org.enginehub.crankcase.java;

import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

public abstract class JavaExtension {
    public abstract Property<Integer> getJavaRelease();

    public abstract ListProperty<String> getDisabledLints();

    public abstract ListProperty<String> getDisabledErrorprone();

    public abstract Property<String> getErrorproneVersion();

    public abstract Property<String> getJunitVersion();
}
