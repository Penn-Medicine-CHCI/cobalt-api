/*
 * Copyright 2021 The University of Pennsylvania and Penn Medicine
 *
 * Originally created at the University of Pennsylvania and Penn Medicine by:
 * Dr. David Asch; Dr. Lisa Bellini; Dr. Cecilia Livesey; Kelley Kugler; and Dr. Matthew Press.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cobaltplatform.api.util;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import static java.lang.String.format;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public final class GitUtility {
	private GitUtility() {
		// Non-instantiable
	}

	@Nonnull
	public static String getHeadCommitHash() {
		ProcessUtility.ProcessResult processResult = ProcessUtility.run("git rev-parse HEAD");
		String output = processResult.getOutput().orElse(null);

		if (processResult.getExitCode() == 0 && output != null)
			return output;

		throw new RuntimeException(format("Unable to determine git HEAD commit hash. Process exit code was %d and output was '%s'", processResult.getExitCode(), output));
	}

	@Nonnull
	public static String getBranch() {
		// Will be HEAD if detached head
		ProcessUtility.ProcessResult processResult = ProcessUtility.run("git rev-parse --abbrev-ref HEAD");
		String output = processResult.getOutput().orElse(null);

		if (processResult.getExitCode() == 0 && output != null)
			return output;

		throw new RuntimeException(format("Unable to determine current branch in git. Process exit code was %d and output was '%s'", processResult.getExitCode(), output));
	}
}
