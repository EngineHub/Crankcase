package org.enginehub.crankcase

import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType

internal val Project.crankcase: CrankcaseExtension
    get() = extensions.getByType()
