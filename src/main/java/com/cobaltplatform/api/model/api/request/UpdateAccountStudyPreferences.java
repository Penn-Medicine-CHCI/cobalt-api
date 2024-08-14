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

import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.RecordingPreference.RecordingPreferenceId;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class UpdateAccountStudyPreferences {

	@Nonnull
	private String username;

	@Nonnull
	private RecordingPreferenceId recordingPreferenceId;

	@Nonnull
	private InstitutionId institutionId;

	@Nonnull
	private UUID studyId;

	@Nonnull
	public String getUsername() {
		return username;
	}

	public void setUsername(@Nonnull String username) {
		this.username = username;
	}

	@Nonnull
	public RecordingPreferenceId getRecordingPreferenceId() {
		return recordingPreferenceId;
	}

	public void setRecordingPreferenceId(@Nonnull RecordingPreferenceId recordingPreferenceId) {
		this.recordingPreferenceId = recordingPreferenceId;
	}

	@Nonnull
	public InstitutionId getInstitutionId() {
		return institutionId;
	}

	public void setInstitutionId(@Nonnull InstitutionId institutionId) {
		this.institutionId = institutionId;
	}

	@Nonnull
	public UUID getStudyId() {
		return studyId;
	}

	public void setStudyId(@Nonnull UUID studyId) {
		this.studyId = studyId;
	}
}
