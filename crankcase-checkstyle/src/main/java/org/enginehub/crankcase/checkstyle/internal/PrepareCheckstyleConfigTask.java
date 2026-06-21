/*
 * SPDX-FileCopyrightText: EngineHub <https://www.enginehub.org/>
 * SPDX-License-Identifier: MPL-2.0
 */

package org.enginehub.crankcase.checkstyle.internal;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;
import org.gradle.workers.WorkAction;
import org.gradle.workers.WorkParameters;
import org.gradle.workers.WorkerExecutor;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import javax.inject.Inject;

@DisableCachingByDefault(because = "Trivially writes config files derived from bundled content")
public abstract class PrepareCheckstyleConfigTask extends DefaultTask implements CheckstyleConfigSpec {
    private static final String SUPPRESSION_TOKEN = "<!--CRANKCASE_SUPPRESSION_FILTER-->";
    private static final String SUPPRESSION_FILTER_MODULE =
        """
        <module name="SuppressionFilter">
            <property name="file" value="${config_loc}/checkstyle-suppression.xml"/>
        </module>
        """;

    @Inject
    protected abstract WorkerExecutor getWorkerExecutor();

    @TaskAction
    public void prepare() {
        getWorkerExecutor().noIsolation().submit(PrepareAction.class, params -> {
            params.getDefaultContent().set(getDefaultContent());
            params.getSuppressions().set(getSuppressions());
            params.getOutputDirectory().set(getOutputDirectory());
        });
    }

    interface Parameters extends WorkParameters, CheckstyleConfigSpec {
    }

    public abstract static class PrepareAction implements WorkAction<Parameters> {
        @Override
        public void execute() {
            Parameters params = getParameters();
            Path outDir = params.getOutputDirectory().getAsFile().get().toPath();
            boolean includeSuppressions = params.getSuppressions().getOrNull() != null;
            try {
                Files.createDirectories(outDir);
                String content = params.getDefaultContent().get().replace(
                    SUPPRESSION_TOKEN, includeSuppressions ? SUPPRESSION_FILTER_MODULE : ""
                );
                Files.writeString(outDir.resolve("checkstyle.xml"), content, StandardCharsets.UTF_8);

                Path suppressionTarget = outDir.resolve("checkstyle-suppression.xml");
                if (includeSuppressions) {
                    Files.copy(
                        params.getSuppressions().getAsFile().get().toPath(),
                        suppressionTarget,
                        StandardCopyOption.REPLACE_EXISTING
                    );
                } else {
                    Files.deleteIfExists(suppressionTarget);
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
