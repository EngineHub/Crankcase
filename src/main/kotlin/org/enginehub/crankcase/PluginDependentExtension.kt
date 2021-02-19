package org.enginehub.crankcase

import org.gradle.api.Action
import org.gradle.api.Project

/**
 * "Extensions" that apply an "owning" plugin when invoked as a config block.
 */
abstract class PluginDependentExtension<E : PluginDependentExtension<E>> internal constructor(
    protected val project: Project
) {
    /**
     * Apply the plugin without extra configuration.
     */
    operator fun invoke() {
        invoke {}
    }

    /**
     * Apply the plugin and then configure it via [config]
     */
    operator fun invoke(config: Action<E>) {
        applyPlugin()
        @Suppress("UNCHECKED_CAST")
        config.execute(this as E)
    }

    protected abstract fun applyPlugin()
}
