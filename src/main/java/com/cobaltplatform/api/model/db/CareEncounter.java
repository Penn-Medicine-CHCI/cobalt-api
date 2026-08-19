/*
 * Copyright 2021 The University of Pennsylvania and Penn Medicine
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

import com.cobaltplatform.api.model.db.CareEncounterStatus.CareEncounterStatusId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.util.UUID;

/**
 * Administrative data attached to a Care Navigator appointment.
 *
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class CareEncounter {
	@Nullable
	private UUID careEncounterId;
	@Nullable
	private UUID appointmentId;
	@Nullable
	private UUID accountId;
	@Nullable
	private CareEncounterStatusId careEncounterStatusId;
	@Nullable
	private String notes;
	@Nullable
	private Instant closedAt;
	@Nullable
	private UUID canceledByAccountId;
	@Nullable
	private Boolean deleted;
	@Nullable
	private UUID createdByAccountId;
	@Nullable
	private UUID lastUpdatedByAccountId;
	@Nullable
	private Instant created;
	@Nullable
	private Instant lastUpdated;
	@Nullable
	private Integer totalCount;

	@Nullable
	public UUID getCareEncounterId() {
		return this.careEncounterId;
	}

	public void setCareEncounterId(@Nullable UUID careEncounterId) {
		this.careEncounterId = careEncounterId;
	}

	@Nullable
	public UUID getAppointmentId() {
		return this.appointmentId;
	}

	public void setAppointmentId(@Nullable UUID appointmentId) {
		this.appointmentId = appointmentId;
	}

	@Nullable
	public UUID getAccountId() {
		return this.accountId;
	}

	public void setAccountId(@Nullable UUID accountId) {
		this.accountId = accountId;
	}

	@Nullable
	public CareEncounterStatusId getCareEncounterStatusId() {
		return this.careEncounterStatusId;
	}

	public void setCareEncounterStatusId(@Nullable CareEncounterStatusId careEncounterStatusId) {
		this.careEncounterStatusId = careEncounterStatusId;
	}

	@Nullable
	public String getNotes() {
		return this.notes;
	}

	public void setNotes(@Nullable String notes) {
		this.notes = notes;
	}

	@Nullable
	public Instant getClosedAt() {
		return this.closedAt;
	}

	public void setClosedAt(@Nullable Instant closedAt) {
		this.closedAt = closedAt;
	}

	@Nullable
	public UUID getCanceledByAccountId() {
		return this.canceledByAccountId;
	}

	public void setCanceledByAccountId(@Nullable UUID canceledByAccountId) {
		this.canceledByAccountId = canceledByAccountId;
	}

	@Nullable
	public Boolean getDeleted() {
		return this.deleted;
	}

	public void setDeleted(@Nullable Boolean deleted) {
		this.deleted = deleted;
	}

	@Nullable
	public UUID getCreatedByAccountId() {
		return this.createdByAccountId;
	}

	public void setCreatedByAccountId(@Nullable UUID createdByAccountId) {
		this.createdByAccountId = createdByAccountId;
	}

	@Nullable
	public UUID getLastUpdatedByAccountId() {
		return this.lastUpdatedByAccountId;
	}

	public void setLastUpdatedByAccountId(@Nullable UUID lastUpdatedByAccountId) {
		this.lastUpdatedByAccountId = lastUpdatedByAccountId;
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

	@Nullable
	public Integer getTotalCount() {
		return this.totalCount;
	}

	public void setTotalCount(@Nullable Integer totalCount) {
		this.totalCount = totalCount;
	}
}
