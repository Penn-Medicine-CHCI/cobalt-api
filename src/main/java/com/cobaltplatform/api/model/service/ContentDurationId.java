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

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
public enum ContentDurationId {
	UNDER_FIVE_MINUTES(0, 4),
	BETWEEN_FIVE_AND_TEN_MINUTES(5, 10),
	BETWEEN_TEN_AND_THIRTY_MINUTES(10, 30),
	OVER_THIRTY_MINUTES(31, Integer.MAX_VALUE);

	@Nonnull
	private final Integer lowerBoundInclusive;
	@Nonnull
	private final Integer upperBoundInclusive;

	private ContentDurationId(@Nonnull Integer lowerBoundInclusive,
														@Nonnull Integer upperBoundInclusive) {
		requireNonNull(lowerBoundInclusive);
		requireNonNull(upperBoundInclusive);

		this.lowerBoundInclusive = lowerBoundInclusive;
		this.upperBoundInclusive = upperBoundInclusive;
	}

	@Nonnull
	public Integer getLowerBoundInclusive() {
		return this.lowerBoundInclusive;
	}

	@Nonnull
	public Integer getUpperBoundInclusive() {
		return this.upperBoundInclusive;
	}
}
