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
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.UUID;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class Block implements Comparable<Block> {
	@Nonnull
	private static final Comparator<Block> DEFAULT_COMPARATOR;

	@Nullable
	private UUID logicalAvailabilityId;
	@Nullable
	private LocalDateTime startDateTime;
	@Nullable
	private LocalDateTime endDateTime;

	static {
		DEFAULT_COMPARATOR = Comparator.comparing(Block::getStartDateTime)
				.thenComparing(Block::getEndDateTime);
	}

	@Override
	public int compareTo(@Nonnull Block otherBlock) {
		requireNonNull(otherBlock);
		return getDefaultComparator().compare(this, otherBlock);
	}

	@Override
	public String toString() {
		return format("%s{%s to %s}", getClass().getSimpleName(), getStartDateTime(),
				getEndDateTime());
	}

	@Nonnull
	public Comparator<Block> getDefaultComparator() {
		return DEFAULT_COMPARATOR;
	}

	@Nullable
	public UUID getLogicalAvailabilityId() {
		return logicalAvailabilityId;
	}

	public void setLogicalAvailabilityId(@Nullable UUID logicalAvailabilityId) {
		this.logicalAvailabilityId = logicalAvailabilityId;
	}

	@Nullable
	public LocalDateTime getStartDateTime() {
		return startDateTime;
	}

	public void setStartDateTime(@Nullable LocalDateTime startDateTime) {
		this.startDateTime = startDateTime;
	}

	@Nullable
	public LocalDateTime getEndDateTime() {
		return endDateTime;
	}

	public void setEndDateTime(@Nullable LocalDateTime endDateTime) {
		this.endDateTime = endDateTime;
	}
}
