/*
 * SPDX-FileCopyrightText: EngineHub <https://www.enginehub.org/>
 * SPDX-License-Identifier: MPL-2.0
 */

package org.enginehub.crankcase.checkstyle;

import org.enginehub.crankcase.checkstyle.internal.PrepareCheckstyleConfigTask;
import org.enginehub.crankcase.common.CrankcaseVersions;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.plugins.quality.CheckstyleExtension;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public abstract class CheckstylePlugin implements Plugin<Project> {
    private static final String DEFAULT_CHECKSTYLE_RESOURCE =
        "/org/enginehub/crankcase/checkstyle/default-checkstyle.xml";

    @Override
    public void apply(Project project) {
        project.getPluginManager().apply("checkstyle");
        var ext = project.getExtensions().create("crankcaseCheckstyle", CrankcaseCheckstyleExtension.class);

        Provider<String> defaultContent = project.getProviders().provider(() -> {
            try (InputStream in = Objects.requireNonNull(
                CheckstylePlugin.class.getResourceAsStream(DEFAULT_CHECKSTYLE_RESOURCE),
                "missing default-checkstyle.xml resource"
            )) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        Provider<Directory> outDir = project.getLayout().getBuildDirectory().dir("crankcase/checkstyle");

        TaskProvider<PrepareCheckstyleConfigTask> prepareTask = project.getTasks().register(
            "crankcasePrepareCheckstyleConfig", PrepareCheckstyleConfigTask.class, task -> {
                task.getDefaultContent().set(defaultContent);
                task.getSuppressions().set(ext.getSuppressionsFile());
                task.getOutputDirectory().set(outDir);
            });

        project.getExtensions().configure(CheckstyleExtension.class, cs -> {
            cs.setToolVersion(CrankcaseVersions.CHECKSTYLE);
            cs.setConfig(project.getResources().getText().fromFile(
                prepareTask.flatMap(t -> t.getOutputDirectory().file("checkstyle.xml"))
            ));
            cs.getConfigDirectory().set(prepareTask.flatMap(PrepareCheckstyleConfigTask::getOutputDirectory));
        });
    }
}
