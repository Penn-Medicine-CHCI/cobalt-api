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

import com.cobaltplatform.api.model.db.PatientOrderCareType.PatientOrderCareTypeId;
import com.cobaltplatform.api.model.db.PatientOrderFocusType.PatientOrderFocusTypeId;
import com.cobaltplatform.api.model.db.PatientOrderTriageSource.PatientOrderTriageSourceId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class PatientOrderTriage {
	@Nullable
	private UUID patientOrderId;
	@Nullable
	private PatientOrderFocusTypeId patientOrderFocusTypeId;
	@Nullable
	private PatientOrderCareTypeId patientOrderCareTypeId;
	@Nullable
	private PatientOrderTriageSourceId patientOrderTriageSourceId;
	@Nullable
	private String reason;
	@Nullable
	private Boolean current;
	@Nullable
	private Instant created;
	@Nullable
	private Instant lastUpdated;

	@Nullable
	public UUID getPatientOrderId() {
		return this.patientOrderId;
	}

	public void setPatientOrderId(@Nullable UUID patientOrderId) {
		this.patientOrderId = patientOrderId;
	}

	@Nullable
	public PatientOrderFocusTypeId getPatientOrderFocusTypeId() {
		return this.patientOrderFocusTypeId;
	}

	public void setPatientOrderFocusTypeId(@Nullable PatientOrderFocusTypeId patientOrderFocusTypeId) {
		this.patientOrderFocusTypeId = patientOrderFocusTypeId;
	}

	@Nullable
	public PatientOrderCareTypeId getPatientOrderCareTypeId() {
		return this.patientOrderCareTypeId;
	}

	public void setPatientOrderCareTypeId(@Nullable PatientOrderCareTypeId patientOrderCareTypeId) {
		this.patientOrderCareTypeId = patientOrderCareTypeId;
	}

	@Nullable
	public PatientOrderTriageSourceId getPatientOrderTriageSourceId() {
		return this.patientOrderTriageSourceId;
	}

	public void setPatientOrderTriageSourceId(@Nullable PatientOrderTriageSourceId patientOrderTriageSourceId) {
		this.patientOrderTriageSourceId = patientOrderTriageSourceId;
	}

	@Nullable
	public String getReason() {
		return this.reason;
	}

	public void setReason(@Nullable String reason) {
		this.reason = reason;
	}

	@Nullable
	public Boolean getCurrent() {
		return this.current;
	}

	public void setCurrent(@Nullable Boolean current) {
		this.current = current;
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
