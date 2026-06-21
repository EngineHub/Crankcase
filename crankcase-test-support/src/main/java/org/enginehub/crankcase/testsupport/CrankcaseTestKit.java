/*
 * SPDX-FileCopyrightText: EngineHub <https://www.enginehub.org/>
 * SPDX-License-Identifier: MPL-2.0
 */

package org.enginehub.crankcase.testsupport;

import com.google.common.collect.ImmutableList;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;

import java.nio.file.Path;
import java.util.List;

import static org.enginehub.crankcase.testsupport.BuildResultSubject.assertThat;

public final class CrankcaseTestKit {
    public static final String GRADLE_VERSION = "9.6.0";

    private CrankcaseTestKit() {
    }

    public static GradleRunner runner(Path projectDir, String... args) {
        return runner(projectDir, ImmutableList.copyOf(args));
    }

    public static GradleRunner runner(Path projectDir, List<String> args) {
        List<String> allArgs = ImmutableList.<String>builder()
            .addAll(args)
            .add("--stacktrace")
            .build();
        return GradleRunner.create()
            .withGradleVersion(GRADLE_VERSION)
            .withProjectDir(projectDir.toFile())
            .withPluginClasspath()
            .withArguments(allArgs);
    }

    public static void assertConfigCacheStoredThenReused(GradleRunner runner) {
        BuildResult first = runner.build();
        assertThat(first).output().contains("Configuration cache entry stored");
        BuildResult second = runner.build();
        assertThat(second).output().contains("Configuration cache entry reused");
    }
}
