/*
 * SPDX-FileCopyrightText: EngineHub <https://www.enginehub.org/>
 * SPDX-License-Identifier: MPL-2.0
 */

package org.enginehub.crankcase.testsupport;

import com.google.common.truth.FailureMetadata;
import com.google.common.truth.Subject;
import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.TaskOutcome;

public final class BuildTaskSubject extends Subject {
    static Factory<BuildTaskSubject, BuildTask> buildTasks() {
        return BuildTaskSubject::new;
    }

    private final BuildTask actual;

    private BuildTaskSubject(FailureMetadata metadata, BuildTask actual) {
        super(metadata, actual);
        this.actual = actual;
    }

    public void hasOutcome(TaskOutcome expected) {
        isNotNull();
        check("getOutcome()").that(actual.getOutcome()).isEqualTo(expected);
    }

    public void succeeded() {
        hasOutcome(TaskOutcome.SUCCESS);
    }

    public void failed() {
        hasOutcome(TaskOutcome.FAILED);
    }

    public void skipped() {
        hasOutcome(TaskOutcome.SKIPPED);
    }

    public void upToDate() {
        hasOutcome(TaskOutcome.UP_TO_DATE);
    }
}
