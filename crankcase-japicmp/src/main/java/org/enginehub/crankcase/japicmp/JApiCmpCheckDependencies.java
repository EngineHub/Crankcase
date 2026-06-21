/*
 * SPDX-FileCopyrightText: EngineHub <https://www.enginehub.org/>
 * SPDX-License-Identifier: MPL-2.0
 */

package org.enginehub.crankcase.japicmp;

import org.gradle.api.artifacts.dsl.Dependencies;
import org.gradle.api.artifacts.dsl.DependencyCollector;

public interface JApiCmpCheckDependencies extends Dependencies {
    DependencyCollector getOldClasspath();

    DependencyCollector getNewClasspath();
}
