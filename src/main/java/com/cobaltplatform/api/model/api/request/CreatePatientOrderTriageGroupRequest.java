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

import com.cobaltplatform.api.model.db.PatientOrderCareType.PatientOrderCareTypeId;
import com.cobaltplatform.api.model.db.PatientOrderFocusType.PatientOrderFocusTypeId;
import com.cobaltplatform.api.model.db.PatientOrderSafetyPlanningStatus.PatientOrderSafetyPlanningStatusId;
import com.cobaltplatform.api.model.db.PatientOrderTriageOverrideReason.PatientOrderTriageOverrideReasonId;
import com.cobaltplatform.api.model.db.PatientOrderTriageSource.PatientOrderTriageSourceId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.List;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class CreatePatientOrderTriageGroupRequest {
	@Nullable
	private UUID accountId;
	@Nullable
	private UUID patientOrderId;
	@Nullable
	private PatientOrderTriageSourceId patientOrderTriageSourceId;
	@Nullable
	private UUID screeningSessionId;
	@Nullable
	private PatientOrderCareTypeId patientOrderCareTypeId; // The "winning" overall care type that is calculated and stored
	@Nullable
	private PatientOrderSafetyPlanningStatusId patientOrderSafetyPlanningStatusId;
	@Nullable
	private PatientOrderTriageOverrideReasonId patientOrderTriageOverrideReasonId;
	@Nullable
	private List<CreatePatientOrderTriageRequest> patientOrderTriages;

	@NotThreadSafe
	public static class CreatePatientOrderTriageRequest {
		@Nullable
		private PatientOrderFocusTypeId patientOrderFocusTypeId;
		@Nullable
		private PatientOrderCareTypeId patientOrderCareTypeId;
		@Nullable
		private String reason;

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
		public String getReason() {
			return this.reason;
		}

		public void setReason(@Nullable String reason) {
			this.reason = reason;
		}
	}

	@Nullable
	public UUID getAccountId() {
		return this.accountId;
	}

	public void setAccountId(@Nullable UUID accountId) {
		this.accountId = accountId;
	}

	@Nullable
	public UUID getPatientOrderId() {
		return this.patientOrderId;
	}

	public void setPatientOrderId(@Nullable UUID patientOrderId) {
		this.patientOrderId = patientOrderId;
	}

	@Nullable
	public PatientOrderTriageSourceId getPatientOrderTriageSourceId() {
		return this.patientOrderTriageSourceId;
	}

	public void setPatientOrderTriageSourceId(@Nullable PatientOrderTriageSourceId patientOrderTriageSourceId) {
		this.patientOrderTriageSourceId = patientOrderTriageSourceId;
	}

	@Nullable
	public UUID getScreeningSessionId() {
		return this.screeningSessionId;
	}

	public void setScreeningSessionId(@Nullable UUID screeningSessionId) {
		this.screeningSessionId = screeningSessionId;
	}

	@Nullable
	public PatientOrderTriageOverrideReasonId getPatientOrderTriageOverrideReasonId() {
		return this.patientOrderTriageOverrideReasonId;
	}

	public void setPatientOrderTriageOverrideReasonId(@Nullable PatientOrderTriageOverrideReasonId patientOrderTriageOverrideReasonId) {
		this.patientOrderTriageOverrideReasonId = patientOrderTriageOverrideReasonId;
	}

	@Nullable
	public PatientOrderSafetyPlanningStatusId getPatientOrderSafetyPlanningStatusId() {
		return this.patientOrderSafetyPlanningStatusId;
	}

	public void setPatientOrderSafetyPlanningStatusId(@Nullable PatientOrderSafetyPlanningStatusId patientOrderSafetyPlanningStatusId) {
		this.patientOrderSafetyPlanningStatusId = patientOrderSafetyPlanningStatusId;
	}

	@Nullable
	public PatientOrderCareTypeId getPatientOrderCareTypeId() {
		return this.patientOrderCareTypeId;
	}

	public void setPatientOrderCareTypeId(@Nullable PatientOrderCareTypeId patientOrderCareTypeId) {
		this.patientOrderCareTypeId = patientOrderCareTypeId;
	}

	@Nullable
	public List<CreatePatientOrderTriageRequest> getPatientOrderTriages() {
		return this.patientOrderTriages;
	}

	public void setPatientOrderTriages(@Nullable List<CreatePatientOrderTriageRequest> patientOrderTriages) {
		this.patientOrderTriages = patientOrderTriages;
	}
}
