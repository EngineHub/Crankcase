/*
 * SPDX-FileCopyrightText: EngineHub <https://www.enginehub.org/>
 * SPDX-License-Identifier: MPL-2.0
 */

package org.enginehub.crankcase.git;

import com.google.common.base.Suppliers;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;
import org.gradle.process.ExecOutput;

import java.io.File;
import java.util.function.Supplier;
import javax.inject.Inject;

public abstract class GitBuildService implements BuildService<GitBuildService.Params> {
    public interface Params extends BuildServiceParameters {
        DirectoryProperty getWorkingDirectory();
    }

    @Inject
    public abstract ProviderFactory getProviders();

    @SuppressWarnings("this-escape")
    private final Supplier<Provider<String>> commitHash = Suppliers.memoize(() -> {
        File workingDir = getParameters().getWorkingDirectory().get().getAsFile();
        ExecOutput execOutput = getProviders().exec(spec -> {
            spec.setWorkingDir(workingDir);
            spec.commandLine("git", "rev-parse", "--short", "HEAD");
            spec.setIgnoreExitValue(true);
        });
        Provider<String> probed = execOutput.getResult().zip(
            execOutput.getStandardOutput().getAsText(),
            (result, output) -> {
                if (result.getExitValue() != 0) {
                    throw new GradleException(
                        "Could not determine the git commit hash: "
                            + "'git rev-parse --short HEAD' exited with "
                            + result.getExitValue() + " in " + workingDir + ". "
                            + "Ensure the build runs inside a git repository with at "
                            + "least one commit, or set -PgitCommitHash=<hash> to "
                            + "provide it explicitly."
                    );
                }
                return output.trim();
            }
        );
        return getProviders().gradleProperty("gitCommitHash").orElse(probed);
    });

    @SuppressWarnings("this-escape")
    private final Supplier<Provider<Boolean>> dirty = Suppliers.memoize(() -> {
        File workingDir = getParameters().getWorkingDirectory().get().getAsFile();
        ExecOutput execOutput = getProviders().exec(spec -> {
            spec.setWorkingDir(workingDir);
            spec.commandLine("git", "status", "--porcelain");
            spec.setIgnoreExitValue(true);
        });
        Provider<Boolean> probed = execOutput.getResult().zip(
            execOutput.getStandardOutput().getAsText(),
            (result, output) -> {
                if (result.getExitValue() != 0) {
                    throw new GradleException(
                        "Could not determine the git working tree state: "
                            + "'git status --porcelain' exited with "
                            + result.getExitValue() + " in " + workingDir + ". "
                            + "Ensure the build runs inside a git repository, or set "
                            + "-PgitDirty=<true|false> to provide it explicitly."
                    );
                }
                return !output.isBlank();
            }
        );
        return getProviders().gradleProperty("gitDirty").map(Boolean::parseBoolean).orElse(probed);
    });

    public Provider<String> getCommitHash() {
        return commitHash.get();
    }

    public Provider<Boolean> getDirty() {
        return dirty.get();
    }
}
