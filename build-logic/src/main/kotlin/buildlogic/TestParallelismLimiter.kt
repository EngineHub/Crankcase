// SPDX-FileCopyrightText: EngineHub <https://www.enginehub.org/>
// SPDX-License-Identifier: MPL-2.0

package buildlogic

import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

abstract class TestParallelismLimiter: BuildService<BuildServiceParameters.None>
