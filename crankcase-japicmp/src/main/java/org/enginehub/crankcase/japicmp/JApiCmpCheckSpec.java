/*
 * SPDX-FileCopyrightText: EngineHub <https://www.enginehub.org/>
 * SPDX-License-Identifier: MPL-2.0
 */

package org.enginehub.crankcase.japicmp;

import org.gradle.api.Action;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

import javax.inject.Inject;

public abstract class JApiCmpCheckSpec {
    private final JApiCmpCheckDependencies dependencies;

    @Inject
    public JApiCmpCheckSpec(ObjectFactory objects) {
        this.dependencies = objects.newInstance(JApiCmpCheckDependencies.class);
    }

    public JApiCmpCheckDependencies getDependencies() {
        return dependencies;
    }

    public void dependencies(Action<? super JApiCmpCheckDependencies> action) {
        action.execute(dependencies);
    }

    public abstract ListProperty<String> getPackageIncludes();

    public abstract ListProperty<String> getPackageExcludes();

    public abstract ListProperty<String> getClassIncludes();

    public abstract ListProperty<String> getClassExcludes();

    /**
     * Whether to skip the check when the old classpath has no jar to compare against (for example,
     * the first release of a module). Defaults to {@code false}.
     */
    public abstract Property<Boolean> getSkipWhenOldClasspathMissing();
}
