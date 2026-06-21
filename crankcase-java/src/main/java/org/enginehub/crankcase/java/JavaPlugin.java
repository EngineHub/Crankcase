/*
 * SPDX-FileCopyrightText: EngineHub <https://www.enginehub.org/>
 * SPDX-License-Identifier: MPL-2.0
 */

package org.enginehub.crankcase.java;

import net.ltgt.gradle.errorprone.ErrorProneOptions;
import net.ltgt.gradle.errorprone.ErrorPronePlugin;
import org.enginehub.crankcase.common.CommonPlugin;
import org.enginehub.crankcase.common.CrankcaseVersions;
import org.enginehub.crankcase.java.internal.DisabledLintsArgumentProvider;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.gradle.api.tasks.testing.Test;
import org.gradle.external.javadoc.StandardJavadocDocletOptions;
import org.gradle.jvm.toolchain.JavaLanguageVersion;

import java.util.List;

public abstract class JavaPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.getPluginManager().apply(CommonPlugin.class);
        project.getPluginManager().apply("java");
        project.getPluginManager().apply(ErrorPronePlugin.class);

        var ext = project.getExtensions().create("crankcaseJava", JavaExtension.class);
        ext.getErrorproneVersion().convention(CrankcaseVersions.ERROR_PRONE);
        ext.getJunitVersion().convention(CrankcaseVersions.JUNIT);

        project.getExtensions().getByType(JavaPluginExtension.class).getToolchain().getLanguageVersion()
            .set(ext.getJavaRelease().map(JavaLanguageVersion::of));

        project.getTasks().withType(JavaCompile.class)
            .matching(t -> t.getName().equals("compileJava") || t.getName().equals("compileTestJava"))
            .configureEach(t -> {
                t.getOptions().getRelease().set(ext.getJavaRelease());
                t.getOptions().getCompilerArgs().addAll(List.of("-Xlint:all", "-parameters", "-Werror"));
                t.getOptions().getCompilerArgumentProviders()
                    .add(new DisabledLintsArgumentProvider(ext.getDisabledLints()));
                t.getOptions().setDeprecation(true);
                t.getOptions().setEncoding("UTF-8");
                var ep = ((ExtensionAware) t.getOptions()).getExtensions().getByType(ErrorProneOptions.class);
                ep.getAllErrorsAsWarnings().set(true);
                ep.getDisableWarningsInGeneratedCode().set(true);
                for (String name : ext.getDisabledErrorprone().get()) {
                    ep.disable(name);
                }
            });

        project.getTasks().withType(Test.class).configureEach(Test::useJUnitPlatform);

        DependencyHandler deps = project.getDependencies();
        deps.addProvider(
            "compileOnly",
            ext.getErrorproneVersion().map(v -> "com.google.errorprone:error_prone_annotations:" + v)
        );
        deps.addProvider(
            "errorprone",
            ext.getErrorproneVersion().map(v -> "com.google.errorprone:error_prone_core:" + v)
        );
        deps.addProvider(
            "testImplementation",
            ext.getJunitVersion().map(v -> deps.platform("org.junit:junit-bom:" + v))
        );
        deps.add("testImplementation", "org.junit.jupiter:junit-jupiter-api");
        deps.add("testImplementation", "org.junit.jupiter:junit-jupiter-params");
        deps.add("testRuntimeOnly", "org.junit.jupiter:junit-jupiter-engine");
        deps.add("testRuntimeOnly", "org.junit.platform:junit-platform-launcher");

        project.getTasks().withType(Javadoc.class).configureEach(t -> {
            t.getOptions().setEncoding("UTF-8");
            var opts = (StandardJavadocDocletOptions) t.getOptions();
            opts.addBooleanOption("Werror", true);
            opts.addBooleanOption("Xdoclint:all", true);
            opts.addBooleanOption("Xdoclint:-missing", true);
            opts.tags(List.of(
                "apiNote:a:API Note:",
                "implSpec:a:Implementation Requirements:",
                "implNote:a:Implementation Note:"
            ));
        });
    }
}
