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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public final class ExecutorServiceUtility {
	@Nonnull
	private static final Long DEFAULT_INITIAL_TERMINATION_TIMEOUT;
	@Nonnull
	private static final TimeUnit DEFAULT_INITIAL_TERMINATION_TIME_UNIT;
	@Nonnull
	private static final Long DEFAULT_FINAL_TERMINATION_TIMEOUT;
	@Nonnull
	private static final TimeUnit DEFAULT_FINAL_TERMINATION_TIME_UNIT;
	@Nonnull
	private static final Logger LOGGER;

	static {
		DEFAULT_INITIAL_TERMINATION_TIMEOUT = 5L;
		DEFAULT_INITIAL_TERMINATION_TIME_UNIT = TimeUnit.SECONDS;
		DEFAULT_FINAL_TERMINATION_TIMEOUT = 5L;
		DEFAULT_FINAL_TERMINATION_TIME_UNIT = TimeUnit.SECONDS;
		LOGGER = LoggerFactory.getLogger(ExecutorServiceUtility.class);
	}

	private ExecutorServiceUtility() {
		// Non-instantiable
	}

	public static void shutdownAndAwaitTermination(@Nonnull ExecutorService executorService) {
		requireNonNull(executorService);
		shutdownAndAwaitTermination(executorService,
				DEFAULT_INITIAL_TERMINATION_TIMEOUT,
				DEFAULT_INITIAL_TERMINATION_TIME_UNIT,
				DEFAULT_FINAL_TERMINATION_TIMEOUT,
				DEFAULT_FINAL_TERMINATION_TIME_UNIT);
	}

	public static void shutdownAndAwaitTermination(@Nonnull ExecutorService executorService,
																								 long initialTerminationTimeout,
																								 @Nonnull TimeUnit initialTerminationTimeUnit,
																								 long finalTerminationTimeout,
																								 @Nonnull TimeUnit finalTerminationTimeUnit) {
		requireNonNull(executorService);
		requireNonNull(initialTerminationTimeUnit);
		requireNonNull(finalTerminationTimeUnit);

		// Disable new tasks from being submitted
		executorService.shutdown();

		try {
			// Wait a while for existing tasks to terminate
			if (!executorService.awaitTermination(initialTerminationTimeout, initialTerminationTimeUnit)) {
				// Cancel currently executing tasks
				executorService.shutdownNow();
				// Wait a while for tasks to respond to being cancelled
				if (!executorService.awaitTermination(finalTerminationTimeout, finalTerminationTimeUnit))
					getLogger().warn("{} did not terminate its tasks cleanly.", executorService);
			}
		} catch (InterruptedException ie) {
			// (Re-)Cancel if current thread also interrupted
			executorService.shutdownNow();
			// Preserve interrupt status
			Thread.currentThread().interrupt();
		}
	}

	@Nonnull
	private static Logger getLogger() {
		return LOGGER;
	}
}