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

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class EpicProviderSlotBooking {
	@Nullable
	private UUID epicProviderSlotBookingId;
	@Nullable
	private UUID providerId;
	@Nullable
	private String contactId;
	@Nullable
	private String contactIdType;
	@Nullable
	private String departmentId;
	@Nullable
	private String departmentIdType;
	@Nullable
	private String visitTypeId;
	@Nullable
	private String visitTypeIdType;
	@Nullable
	private LocalDateTime startDateTime;
	@Nullable
	private LocalDateTime endDateTime;
	@Nullable
	private Instant created;
	@Nullable
	private Instant lastUpdated;

	@Nullable
	public UUID getEpicProviderSlotBookingId() {
		return this.epicProviderSlotBookingId;
	}

	public void setEpicProviderSlotBookingId(@Nullable UUID epicProviderSlotBookingId) {
		this.epicProviderSlotBookingId = epicProviderSlotBookingId;
	}

	@Nullable
	public UUID getProviderId() {
		return this.providerId;
	}

	public void setProviderId(@Nullable UUID providerId) {
		this.providerId = providerId;
	}

	@Nullable
	public String getContactId() {
		return this.contactId;
	}

	public void setContactId(@Nullable String contactId) {
		this.contactId = contactId;
	}

	@Nullable
	public String getContactIdType() {
		return this.contactIdType;
	}

	public void setContactIdType(@Nullable String contactIdType) {
		this.contactIdType = contactIdType;
	}

	@Nullable
	public String getDepartmentId() {
		return this.departmentId;
	}

	public void setDepartmentId(@Nullable String departmentId) {
		this.departmentId = departmentId;
	}

	@Nullable
	public String getDepartmentIdType() {
		return this.departmentIdType;
	}

	public void setDepartmentIdType(@Nullable String departmentIdType) {
		this.departmentIdType = departmentIdType;
	}

	@Nullable
	public String getVisitTypeId() {
		return this.visitTypeId;
	}

	public void setVisitTypeId(@Nullable String visitTypeId) {
		this.visitTypeId = visitTypeId;
	}

	@Nullable
	public String getVisitTypeIdType() {
		return this.visitTypeIdType;
	}

	public void setVisitTypeIdType(@Nullable String visitTypeIdType) {
		this.visitTypeIdType = visitTypeIdType;
	}

	@Nullable
	public LocalDateTime getStartDateTime() {
		return this.startDateTime;
	}

	public void setStartDateTime(@Nullable LocalDateTime startDateTime) {
		this.startDateTime = startDateTime;
	}

	@Nullable
	public LocalDateTime getEndDateTime() {
		return this.endDateTime;
	}

	public void setEndDateTime(@Nullable LocalDateTime endDateTime) {
		this.endDateTime = endDateTime;
	}

	@Nullable
	public Instant getCreated() {
		return this.created;
	}

	public void setCreated(@Nullable Instant created) {
		this.created = created;
	}

	@Nullable
	public Instant getLastUpdated() {
		return this.lastUpdated;
	}

	public void setLastUpdated(@Nullable Instant lastUpdated) {
		this.lastUpdated = lastUpdated;
	}
}