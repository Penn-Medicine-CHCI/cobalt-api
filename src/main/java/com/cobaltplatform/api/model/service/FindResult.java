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

package com.cobaltplatform.api.model.service;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Immutable
public class FindResult<T> {
	@Nonnull
	private final List<T> results;
	@Nonnull
	private final Integer totalCount;

	@Nonnull
	private static final FindResult<?> EMPTY;

	static {
		EMPTY = new FindResult(Collections.emptyList(), 0);
	}

	@Nonnull
	public static FindResult<?> empty() {
		return EMPTY;
	}

	public FindResult(@Nonnull List<T> results, @Nonnull Integer totalCount) {
		requireNonNull(results);
		requireNonNull(totalCount);

		this.results = Collections.unmodifiableList(new ArrayList<>(results));
		this.totalCount = totalCount;
	}

	@Nonnull
	public List<T> getResults() {
		return results;
	}

	@Nonnull
	public Integer getTotalCount() {
		return totalCount;
	}
}
