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

package com.cobaltplatform.api.model.db;

import com.cobaltplatform.api.model.db.LogicalAvailabilityType.LogicalAvailabilityTypeId;
import com.cobaltplatform.api.model.db.RecurrenceType.RecurrenceTypeId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import static java.lang.String.format;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class LogicalAvailability {
	@Nullable
	private UUID logicalAvailabilityId;
	@Nullable
	private UUID providerId;
	@Nullable
	private LogicalAvailabilityTypeId logicalAvailabilityTypeId;
	@Nullable
	private RecurrenceTypeId recurrenceTypeId;
	@Nullable
	private LocalDateTime startDateTime;
	@Nullable
	private LocalDateTime endDateTime;
	@Nullable
	private Boolean recurSunday;
	@Nullable
	private Boolean recurMonday;
	@Nullable
	private Boolean recurTuesday;
	@Nullable
	private Boolean recurWednesday;
	@Nullable
	private Boolean recurThursday;
	@Nullable
	private Boolean recurFriday;
	@Nullable
	private Boolean recurSaturday;
	@Nullable
	private Instant created;
	@Nullable
	private UUID createdByAccountId;
	@Nullable
	private Instant lastUpdated;
	@Nullable
	private UUID lastUpdatedByAccountId;

	@Override
	public String toString() {
		return format("%s{%s to %s, availability type %s, recurrence type %s}", getClass().getSimpleName(), getStartDateTime(),
				getEndDateTime(), getLogicalAvailabilityTypeId(), getRecurrenceTypeId());
	}

	@Nullable
	public UUID getLogicalAvailabilityId() {
		return logicalAvailabilityId;
	}

	public void setLogicalAvailabilityId(@Nullable UUID logicalAvailabilityId) {
		this.logicalAvailabilityId = logicalAvailabilityId;
	}

	@Nullable
	public UUID getProviderId() {
		return providerId;
	}

	public void setProviderId(@Nullable UUID providerId) {
		this.providerId = providerId;
	}

	@Nullable
	public LogicalAvailabilityTypeId getLogicalAvailabilityTypeId() {
		return logicalAvailabilityTypeId;
	}

	public void setLogicalAvailabilityTypeId(@Nullable LogicalAvailabilityTypeId logicalAvailabilityTypeId) {
		this.logicalAvailabilityTypeId = logicalAvailabilityTypeId;
	}

	@Nullable
	public RecurrenceTypeId getRecurrenceTypeId() {
		return recurrenceTypeId;
	}

	public void setRecurrenceTypeId(@Nullable RecurrenceTypeId recurrenceTypeId) {
		this.recurrenceTypeId = recurrenceTypeId;
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

	@Nullable
	public Boolean getRecurSunday() {
		return recurSunday;
	}

	public void setRecurSunday(@Nullable Boolean recurSunday) {
		this.recurSunday = recurSunday;
	}

	@Nullable
	public Boolean getRecurMonday() {
		return recurMonday;
	}

	public void setRecurMonday(@Nullable Boolean recurMonday) {
		this.recurMonday = recurMonday;
	}

	@Nullable
	public Boolean getRecurTuesday() {
		return recurTuesday;
	}

	public void setRecurTuesday(@Nullable Boolean recurTuesday) {
		this.recurTuesday = recurTuesday;
	}

	@Nullable
	public Boolean getRecurWednesday() {
		return recurWednesday;
	}

	public void setRecurWednesday(@Nullable Boolean recurWednesday) {
		this.recurWednesday = recurWednesday;
	}

	@Nullable
	public Boolean getRecurThursday() {
		return recurThursday;
	}

	public void setRecurThursday(@Nullable Boolean recurThursday) {
		this.recurThursday = recurThursday;
	}

	@Nullable
	public Boolean getRecurFriday() {
		return recurFriday;
	}

	public void setRecurFriday(@Nullable Boolean recurFriday) {
		this.recurFriday = recurFriday;
	}

	@Nullable
	public Boolean getRecurSaturday() {
		return recurSaturday;
	}

	public void setRecurSaturday(@Nullable Boolean recurSaturday) {
		this.recurSaturday = recurSaturday;
	}

	@Nullable
	public Instant getCreated() {
		return created;
	}

	public void setCreated(@Nullable Instant created) {
		this.created = created;
	}

	@Nullable
	public UUID getCreatedByAccountId() {
		return createdByAccountId;
	}

	public void setCreatedByAccountId(@Nullable UUID createdByAccountId) {
		this.createdByAccountId = createdByAccountId;
	}

	@Nullable
	public Instant getLastUpdated() {
		return lastUpdated;
	}

	public void setLastUpdated(@Nullable Instant lastUpdated) {
		this.lastUpdated = lastUpdated;
	}

	@Nullable
	public UUID getLastUpdatedByAccountId() {
		return lastUpdatedByAccountId;
	}

	public void setLastUpdatedByAccountId(@Nullable UUID lastUpdatedByAccountId) {
		this.lastUpdatedByAccountId = lastUpdatedByAccountId;
	}
}
