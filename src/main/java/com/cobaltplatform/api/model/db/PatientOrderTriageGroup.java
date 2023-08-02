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

import com.cobaltplatform.api.model.db.PatientOrderTriageOverrideReason.PatientOrderTriageOverrideReasonId;
import com.cobaltplatform.api.model.db.PatientOrderTriageSource.PatientOrderTriageSourceId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class PatientOrderTriageGroup {
	@Nullable
	private UUID patientOrderTriageGroupId;
	@Nullable
	private UUID patientOrderId;
	@Nullable
	private PatientOrderTriageOverrideReasonId patientOrderTriageOverrideReasonId;
	@Nullable
	private PatientOrderTriageSourceId patientOrderTriageSourceId;
	@Nullable
	private UUID accountId;
	@Nullable
	private UUID screeningSessionId;
	@Nullable
	private Boolean active;
	@Nullable
	private Instant created;
	@Nullable
	private Instant lastUpdated;

	@Nullable
	public UUID getPatientOrderTriageGroupId() {
		return this.patientOrderTriageGroupId;
	}

	public void setPatientOrderTriageGroupId(@Nullable UUID patientOrderTriageGroupId) {
		this.patientOrderTriageGroupId = patientOrderTriageGroupId;
	}

	@Nullable
	public UUID getPatientOrderId() {
		return this.patientOrderId;
	}

	public void setPatientOrderId(@Nullable UUID patientOrderId) {
		this.patientOrderId = patientOrderId;
	}

	@Nullable
	public PatientOrderTriageOverrideReasonId getPatientOrderTriageOverrideReasonId() {
		return this.patientOrderTriageOverrideReasonId;
	}

	public void setPatientOrderTriageOverrideReasonId(@Nullable PatientOrderTriageOverrideReasonId patientOrderTriageOverrideReasonId) {
		this.patientOrderTriageOverrideReasonId = patientOrderTriageOverrideReasonId;
	}

	@Nullable
	public PatientOrderTriageSourceId getPatientOrderTriageSourceId() {
		return this.patientOrderTriageSourceId;
	}

	public void setPatientOrderTriageSourceId(@Nullable PatientOrderTriageSourceId patientOrderTriageSourceId) {
		this.patientOrderTriageSourceId = patientOrderTriageSourceId;
	}

	@Nullable
	public UUID getAccountId() {
		return this.accountId;
	}

	public void setAccountId(@Nullable UUID accountId) {
		this.accountId = accountId;
	}

	@Nullable
	public UUID getScreeningSessionId() {
		return this.screeningSessionId;
	}

	public void setScreeningSessionId(@Nullable UUID screeningSessionId) {
		this.screeningSessionId = screeningSessionId;
	}

	@Nullable
	public Boolean getActive() {
		return this.active;
	}

	public void setActive(@Nullable Boolean active) {
		this.active = active;
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
