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

package com.cobaltplatform.api.context;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Singleton;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify LLC.
 */
@Singleton
@ThreadSafe
public class DatabaseContextExecutor {
	@Nonnull
	private static final ThreadLocal<DatabaseContext> DATABASE_CONTEXT_HOLDER;

	static {
		DATABASE_CONTEXT_HOLDER = new ThreadLocal<>();
	}

	public void execute(@Nonnull DatabaseContext databaseContext, @Nonnull DatabaseContextOperation databaseContextOperation) throws Exception {
		requireNonNull(databaseContext);
		requireNonNull(databaseContextOperation);

		DatabaseContext previousDatabaseContext = DATABASE_CONTEXT_HOLDER.get();

		boolean shouldRestorePreviousContext = previousDatabaseContext != null;

		try {
			DATABASE_CONTEXT_HOLDER.set(databaseContext);
			databaseContextOperation.execute();
		} finally {
			DATABASE_CONTEXT_HOLDER.remove();
		}

		if (shouldRestorePreviousContext)
			DATABASE_CONTEXT_HOLDER.set(previousDatabaseContext);
	}

	@Nonnull
	public Optional<DatabaseContext> getDatabaseContext() {
		return Optional.ofNullable(DATABASE_CONTEXT_HOLDER.get());
	}

	@FunctionalInterface
	public interface DatabaseContextOperation {
		void execute() throws Exception;
	}
}
