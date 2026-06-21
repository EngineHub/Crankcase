/*
 * SPDX-FileCopyrightText: EngineHub <https://www.enginehub.org/>
 * SPDX-License-Identifier: MPL-2.0
 */

package org.enginehub.crankcase.common;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.plugins.ide.idea.model.IdeaModel;

import java.util.concurrent.TimeUnit;

public abstract class CommonPlugin implements Plugin<Project> {
    @Override
    public void apply(Project target) {
        target.getConfigurations().configureEach(c ->
            c.resolutionStrategy(rs -> rs.cacheChangingModulesFor(1, TimeUnit.DAYS)));

        target.getPluginManager().withPlugin("idea", _ ->
            target.getExtensions().configure(IdeaModel.class, ideaModel -> {
                ideaModel.getModule().setDownloadJavadoc(true);
                ideaModel.getModule().setDownloadSources(true);
            })
        );
    }
}
