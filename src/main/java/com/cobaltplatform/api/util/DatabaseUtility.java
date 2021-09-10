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

import com.pyranid.StatementMetadata;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Collection;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify LLC.
 */
@ThreadSafe
public final class DatabaseUtility {
	@Nonnull
	public static final StatementMetadata SUPPRESS_LOGGING_STATEMENT_METADATA;

	static {
		SUPPRESS_LOGGING_STATEMENT_METADATA = new StatementMetadata();
	}

	private DatabaseUtility() {
		// Non-instantiable
	}

	@Nonnull
	public static String sqlInListPlaceholders(@Nonnull Collection<?> collection) {
		requireNonNull(collection);

		if (collection.size() == 0)
			throw new IllegalArgumentException("Attempted to create an empty IN list");

		// Turns a list of anything into a string like "(?,?.?)" for use as a parameterized "IN" list in SQL
		return format("(%s)", collection.stream().map(element -> "?").collect(Collectors.joining(",")));
	}

	@Nonnull
	public static Object[] sqlVaragsParameters(@Nonnull Collection<?> collection) {
		requireNonNull(collection);
		return collection.toArray(new Object[collection.size()]);
	}
}