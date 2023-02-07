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

import com.cobaltplatform.api.model.db.PatientOrderTrackingType.PatientOrderTrackingTypeId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.Map;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class CreatePatientOrderTrackingRequest {
	@Nullable
	private PatientOrderTrackingTypeId patientOrderTrackingTypeId;
	@Nullable
	private UUID patientOrderId;
	@Nullable
	private UUID accountId;
	@Nullable
	private String message;
	@Nullable
	private Map<String, Object> metadata;

	@Nullable
	public PatientOrderTrackingTypeId getPatientOrderTrackingTypeId() {
		return this.patientOrderTrackingTypeId;
	}

	public void setPatientOrderTrackingTypeId(@Nullable PatientOrderTrackingTypeId patientOrderTrackingTypeId) {
		this.patientOrderTrackingTypeId = patientOrderTrackingTypeId;
	}

	@Nullable
	public UUID getPatientOrderId() {
		return this.patientOrderId;
	}

	public void setPatientOrderId(@Nullable UUID patientOrderId) {
		this.patientOrderId = patientOrderId;
	}

	@Nullable
	public UUID getAccountId() {
		return this.accountId;
	}

	public void setAccountId(@Nullable UUID accountId) {
		this.accountId = accountId;
	}

	@Nullable
	public String getMessage() {
		return this.message;
	}

	public void setMessage(@Nullable String message) {
		this.message = message;
	}

	@Nullable
	public Map<String, Object> getMetadata() {
		return this.metadata;
	}

	public void setMetadata(@Nullable Map<String, Object> metadata) {
		this.metadata = metadata;
	}
}
