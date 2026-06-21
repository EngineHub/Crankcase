/*
 * SPDX-FileCopyrightText: EngineHub <https://www.enginehub.org/>
 * SPDX-License-Identifier: MPL-2.0
 */

package org.enginehub.crankcase.licensing;

import net.octyl.levelheadered.LevelHeaderedExtension;
import net.octyl.levelheadered.LevelHeaderedPlugin;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

import java.io.File;

public abstract class LicensingPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.getPluginManager().apply(LevelHeaderedPlugin.class);

        var implExt = project.getExtensions().getByType(LevelHeaderedExtension.class);
        implExt.headerTemplate(new File(project.getRootDir(), "HEADER.txt"));
    }
}
