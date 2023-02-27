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

import static java.lang.String.format;

/**
 * @author Transmogrify LLC.
 */
@NotThreadSafe
public class PatientOrderEventType {
	@Nullable
	private PatientOrderEventTypeId patientOrderEventTypeId;
	@Nullable
	private String description;

	public enum PatientOrderEventTypeId {
		IMPORTED,
		STATUS_CHANGED,
		SCREENING_STATUS_CHANGED,
		PANEL_ACCOUNT_CHANGED,
		PATIENT_ACCOUNT_ASSIGNED,
		NOTE_CREATED,
		NOTE_UPDATED,
		NOTE_DELETED,
		OUTREACH_CREATED,
		OUTREACH_UPDATED,
		OUTREACH_DELETED,
	}

	@Override
	public String toString() {
		return format("%s{patientOrderEventTypeId=%s, description=%s}", getClass().getSimpleName(), getPatientOrderEventTypeId(), getDescription());
	}

	@Nullable
	public PatientOrderEventTypeId getPatientOrderEventTypeId() {
		return this.patientOrderEventTypeId;
	}

	public void setPatientOrderEventTypeId(@Nullable PatientOrderEventTypeId patientOrderEventTypeId) {
		this.patientOrderEventTypeId = patientOrderEventTypeId;
	}

	@Nullable
	public String getDescription() {
		return this.description;
	}

	public void setDescription(@Nullable String description) {
		this.description = description;
	}
}