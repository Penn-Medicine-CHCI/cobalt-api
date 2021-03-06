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

package com.cobaltplatform.api.model.api.request;

import com.cobaltplatform.api.model.db.LogicalAvailabilityType.LogicalAvailabilityTypeId;
import com.cobaltplatform.api.model.db.RecurrenceType.RecurrenceTypeId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class CreateLogicalAvailabilityRequest {
	@Nullable
	private UUID providerId;
	@Nullable
	private UUID accountId;
	@Nullable
	private LogicalAvailabilityTypeId logicalAvailabilityTypeId;
	@Nullable
	private RecurrenceTypeId recurrenceTypeId;
	@Nullable
	private LocalDateTime startDateTime;
	@Nullable
	private LocalDate endDate;
	@Nullable
	private LocalTime endTime;
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
	private List<UUID> appointmentTypeIds;

	@Nullable
	public UUID getProviderId() {
		return providerId;
	}

	public void setProviderId(@Nullable UUID providerId) {
		this.providerId = providerId;
	}

	@Nullable
	public UUID getAccountId() {
		return accountId;
	}

	public void setAccountId(@Nullable UUID accountId) {
		this.accountId = accountId;
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
	public LocalDate getEndDate() {
		return endDate;
	}

	public void setEndDate(@Nullable LocalDate endDate) {
		this.endDate = endDate;
	}

	@Nullable
	public LocalTime getEndTime() {
		return endTime;
	}

	public void setEndTime(@Nullable LocalTime endTime) {
		this.endTime = endTime;
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
	public List<UUID> getAppointmentTypeIds() {
		return appointmentTypeIds;
	}

	public void setAppointmentTypeIds(@Nullable List<UUID> appointmentTypeIds) {
		this.appointmentTypeIds = appointmentTypeIds;
	}
}