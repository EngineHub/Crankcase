/*
 * SPDX-FileCopyrightText: EngineHub <https://www.enginehub.org/>
 * SPDX-License-Identifier: MPL-2.0
 */

package org.enginehub.crankcase.testsupport;

import com.google.common.truth.FailureMetadata;
import com.google.common.truth.StringSubject;
import com.google.common.truth.Subject;
import org.gradle.testkit.runner.BuildResult;

import static com.google.common.truth.Truth.assertAbout;

public final class BuildResultSubject extends Subject {
    public static BuildResultSubject assertThat(BuildResult actual) {
        return assertAbout(buildResults()).that(actual);
    }

    public static Factory<BuildResultSubject, BuildResult> buildResults() {
        return BuildResultSubject::new;
    }

    private final BuildResult actual;

    private BuildResultSubject(FailureMetadata metadata, BuildResult actual) {
        super(metadata, actual);
        this.actual = actual;
    }

    public StringSubject output() {
        isNotNull();
        return check("getOutput()").that(actual.getOutput());
    }

    public BuildTaskSubject task(String path) {
        isNotNull();
        return check("task(%s)", path)
            .withMessage("build output:\n%s", actual.getOutput())
            .about(BuildTaskSubject.buildTasks())
            .that(actual.task(path));
    }
}
