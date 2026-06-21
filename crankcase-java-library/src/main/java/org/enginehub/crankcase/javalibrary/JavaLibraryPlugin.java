/*
 * SPDX-FileCopyrightText: EngineHub <https://www.enginehub.org/>
 * SPDX-License-Identifier: MPL-2.0
 */

package org.enginehub.crankcase.javalibrary;

import org.enginehub.crankcase.java.JavaPlugin;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginExtension;

public abstract class JavaLibraryPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.getPluginManager().apply(JavaPlugin.class);
        project.getPluginManager().apply("java-library");

        project.getExtensions().configure(JavaPluginExtension.class, je -> {
            je.withJavadocJar();
            je.withSourcesJar();
        });
    }
}
