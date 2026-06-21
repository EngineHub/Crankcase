/*
 * SPDX-FileCopyrightText: EngineHub <https://www.enginehub.org/>
 * SPDX-License-Identifier: MPL-2.0
 */

package org.enginehub.crankcase.japicmp;

import me.champeau.gradle.japicmp.JapicmpTask;
import org.enginehub.crankcase.japicmp.internal.ResetAcceptedApiChangesFileTask;
import org.enginehub.crankcase.japicmp.internal.accept.AcceptedRegressionsRulePostProcess;
import org.enginehub.crankcase.japicmp.internal.accept.AcceptingSetupRule;
import org.enginehub.crankcase.japicmp.internal.accept.BinaryCompatRule;
import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.DependencyScopeConfiguration;
import org.gradle.api.artifacts.ResolvableConfiguration;
import org.gradle.api.artifacts.ResolveException;
import org.gradle.api.artifacts.dsl.DependencyCollector;
import org.gradle.api.file.FileCollection;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.internal.resolve.ModuleVersionNotFoundException;

import java.nio.file.Path;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;
import javax.inject.Inject;

public abstract class JApiCmpExtension {
    private static final Pattern NAME_PATTERN = Pattern.compile("[A-Za-z][A-Za-z0-9]*");

    private final Project project;

    @Inject
    public JApiCmpExtension(Project project) {
        this.project = project;
    }

    @Inject
    protected abstract ObjectFactory getObjectFactory();

    public void addCheck(String name, Action<? super JApiCmpCheckSpec> action) {
        if (!NAME_PATTERN.matcher(name).matches()) {
            throw new IllegalArgumentException(
                "Invalid check name '" + name + "'; must match " + NAME_PATTERN.pattern()
            );
        }
        String capitalized = Character.toTitleCase(name.charAt(0)) + name.substring(1);
        Path changeFile = project.file(
            "src/changes/accepted-" + name + "-public-api-changes.json"
        ).toPath();

        var spec = getObjectFactory().newInstance(JApiCmpCheckSpec.class);
        action.execute(spec);

        NamedDomainObjectProvider<ResolvableConfiguration> oldClasspath = registerClasspathConfigurations(
            capitalized, "Old", spec.getDependencies().getOldClasspath()
        );
        NamedDomainObjectProvider<ResolvableConfiguration> newClasspath = registerClasspathConfigurations(
            capitalized, "New", spec.getDependencies().getNewClasspath()
        );

        Provider<Boolean> skipWhenOldClasspathMissing = spec.getSkipWhenOldClasspathMissing();
        FileCollection oldClasspathFiles = resolveOldClasspath(capitalized, oldClasspath, skipWhenOldClasspathMissing);

        TaskProvider<JapicmpTask> checkTask = project.getTasks().register(
            "check" + capitalized + "ApiCompatibility",
            JapicmpTask.class,
            task -> {
                task.setGroup("API Compatibility");
                task.setDescription("Check API compatibility for " + capitalized + " API");
                task.getInputs().files(changeFile.toFile())
                    .withPropertyName("acceptedApiChanges")
                    .withPathSensitivity(PathSensitivity.RELATIVE);
                task.richReport(rr -> {
                    rr.addSetupRule(
                        AcceptingSetupRule.class,
                        AcceptingSetupRule.createParams(changeFile)
                    );
                    rr.addRule(BinaryCompatRule.class);
                    rr.addPostProcessRule(AcceptedRegressionsRulePostProcess.class);
                    rr.getReportName().set("api-compatibility-" + name + ".html");
                });
                task.getOldClasspath().from(oldClasspathFiles);
                task.getNewClasspath().from(newClasspath);
                task.getPackageIncludes().set(spec.getPackageIncludes());
                task.getPackageExcludes().set(spec.getPackageExcludes());
                task.getClassIncludes().set(spec.getClassIncludes());
                task.getClassExcludes().set(spec.getClassExcludes());
                task.getOnlyModified().set(false);
                task.getFailOnModification().set(false);
                task.onlyIf(t -> !skipWhenOldClasspathMissing.getOrElse(false) || !oldClasspathFiles.isEmpty());
            }
        );
        project.getTasks().named("checkApiCompatibility")
            .configure(t -> t.dependsOn(checkTask));

        TaskProvider<ResetAcceptedApiChangesFileTask> resetTask = project.getTasks().register(
            "reset" + capitalized + "AcceptedApiChangesFile",
            ResetAcceptedApiChangesFileTask.class,
            task -> {
                task.setDescription("Reset the accepted API changes file for " + name);
                task.getChangeFile().fileValue(changeFile.toFile());
            }
        );
        project.getTasks().named("resetAcceptedApiChangesFiles")
            .configure(t -> t.dependsOn(resetTask));
    }

    private FileCollection resolveOldClasspath(
        String capitalized,
        NamedDomainObjectProvider<ResolvableConfiguration> oldClasspath,
        Provider<Boolean> skipWhenMissing
    ) {
        return project.files((Callable<Object>) () -> {
            ResolvableConfiguration configuration = oldClasspath.get();
            if (!skipWhenMissing.getOrElse(false)) {
                return configuration;
            }
            try {
                configuration.getResolvedConfiguration().rethrowFailure();
                return configuration;
            } catch (ResolveException e) {
                if (e.getCause() instanceof ModuleVersionNotFoundException) {
                    project.getLogger().warn(
                        "Skipping {} API compatibility check because there is no jar to compare against",
                        capitalized
                    );
                    project.getLogger().info("API compatibility exception details: ", e);
                    return Collections.emptySet();
                }
                throw e;
            }
        });
    }

    private NamedDomainObjectProvider<ResolvableConfiguration> registerClasspathConfigurations(
        String capitalized, String side, DependencyCollector collector
    ) {
        ConfigurationContainer configurations = project.getConfigurations();
        String prefix = "crankcaseJapicmp" + capitalized + side + "Classpath";
        NamedDomainObjectProvider<DependencyScopeConfiguration> scope =
            configurations.dependencyScope(
                prefix + "Scope",
                s -> s.fromDependencyCollector(collector)
            );
        return configurations.resolvable(prefix, c -> c.extendsFrom(scope.get()));
    }
}
