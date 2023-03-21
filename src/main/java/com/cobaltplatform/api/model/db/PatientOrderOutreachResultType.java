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

import com.cobaltplatform.api.model.db.PatientOrderOutreachResultStatus.PatientOrderOutreachResultStatusId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import static java.lang.String.format;

/**
 * @author Transmogrify LLC.
 */
@NotThreadSafe
public class PatientOrderOutreachResultType {
	@Nullable
	private PatientOrderOutreachResultTypeId patientOrderOutreachResultTypeId;
	@Nullable
	private PatientOrderOutreachResultStatusId patientOrderOutreachResultStatusId;
	@Nullable
	private String description;

	public enum PatientOrderOutreachResultTypeId {
		NO_ANSWER,
		BUSY,
		LEFT_VOICEMAIL,
		LEFT_MESSAGE,
		DISCONNECTED,
		WRONG_NUMBER,
		DISCUSSED_APPOINTMENT,
		DISCUSSED_DIGITAL_SCREENING,
		DISCUSSED_RESOURCES,
		DISCUSSED_OTHER,
		SENT_RESOURCES,
		SENT_OTHER
	}

	@Override
	public String toString() {
		return format("%s{patientOrderOutreachResultTypeId=%s, description=%s}", getClass().getSimpleName(), getPatientOrderOutreachResultTypeId(), getDescription());
	}

	@Nullable
	public PatientOrderOutreachResultTypeId getPatientOrderOutreachResultTypeId() {
		return this.patientOrderOutreachResultTypeId;
	}

	public void setPatientOrderOutreachResultTypeId(@Nullable PatientOrderOutreachResultTypeId patientOrderOutreachResultTypeId) {
		this.patientOrderOutreachResultTypeId = patientOrderOutreachResultTypeId;
	}

	@Nullable
	public PatientOrderOutreachResultStatusId getPatientOrderOutreachResultStatusId() {
		return this.patientOrderOutreachResultStatusId;
	}

	public void setPatientOrderOutreachResultStatusId(@Nullable PatientOrderOutreachResultStatusId patientOrderOutreachResultStatusId) {
		this.patientOrderOutreachResultStatusId = patientOrderOutreachResultStatusId;
	}

	@Nullable
	public String getDescription() {
		return this.description;
	}

	public void setDescription(@Nullable String description) {
		this.description = description;
	}
}