package org.enginehub.crankcase

import org.enginehub.crankcase.checkstyle.CasedCheckstyleExtension
import org.enginehub.crankcase.coverage.CasedCoverageExtension
import org.enginehub.crankcase.java.CasedJavaExtension
import org.enginehub.crankcase.license.CasedLicenseExtension
import org.enginehub.crankcase.publish.CasedPublishExtension
import org.gradle.api.Project

abstract class CrankcaseExtension(private val project: Project) {
    abstract val license: CasedLicenseExtension
    abstract val java: CasedJavaExtension
    abstract val checkstyle: CasedCheckstyleExtension
    abstract val coverage: CasedCoverageExtension
    abstract val publish: CasedPublishExtension

    /**
     * Inherits the [Project.getGroup] and [Project.getVersion] from [from],
     * which is the root project by default.
     */
    fun inheritGroupAndVersion(from: Project = project.rootProject) {
        project.group = from.group
        project.version = from.version
    }
}
