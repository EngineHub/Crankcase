/*
 * SPDX-FileCopyrightText: EngineHub <https://www.enginehub.org/>
 * SPDX-License-Identifier: MPL-2.0
 */

package org.enginehub.crankcase.git;

import org.gradle.api.provider.Provider;

import javax.inject.Inject;

public abstract class GitExtension {
    private final Provider<GitBuildService> service;

    @Inject
    public GitExtension(Provider<GitBuildService> service) {
        this.service = service;
    }

    public Provider<String> getCommitHash() {
        return service.flatMap(GitBuildService::getCommitHash);
    }

    public Provider<Boolean> getDirty() {
        return service.flatMap(GitBuildService::getDirty);
    }
}
