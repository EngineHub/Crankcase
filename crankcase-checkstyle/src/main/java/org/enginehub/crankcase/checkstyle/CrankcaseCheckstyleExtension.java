/*
 * SPDX-FileCopyrightText: EngineHub <https://www.enginehub.org/>
 * SPDX-License-Identifier: MPL-2.0
 */

package org.enginehub.crankcase.checkstyle;

import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;

public abstract class CrankcaseCheckstyleExtension {
    public abstract RegularFileProperty getSuppressionsFile();

    public abstract Property<Integer> getJavaRelease();
}
