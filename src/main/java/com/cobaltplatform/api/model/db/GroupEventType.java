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

import com.cobaltplatform.api.model.db.Institution.InstitutionId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class GroupEventType {
	@Nullable
	private UUID groupEventTypeId;
	@Nullable
	private Long acuityCalendarId;
	@Nullable
	private Long acuityAppointmentTypeId;
	@Nullable
	private InstitutionId institutionId;
	@Nullable
	private String urlName;
	@Nullable
	private String imageUrl;
	@Nullable
	private String videoconferenceUrl;
	@Nullable
	private Instant created;
	@Nullable
	private Instant lastUpdated;

	@Nullable
	public UUID getGroupEventTypeId() {
		return groupEventTypeId;
	}

	public void setGroupEventTypeId(@Nullable UUID groupEventTypeId) {
		this.groupEventTypeId = groupEventTypeId;
	}

	@Nullable
	public Long getAcuityCalendarId() {
		return acuityCalendarId;
	}

	public void setAcuityCalendarId(@Nullable Long acuityCalendarId) {
		this.acuityCalendarId = acuityCalendarId;
	}

	@Nullable
	public Long getAcuityAppointmentTypeId() {
		return acuityAppointmentTypeId;
	}

	public void setAcuityAppointmentTypeId(@Nullable Long acuityAppointmentTypeId) {
		this.acuityAppointmentTypeId = acuityAppointmentTypeId;
	}

	@Nullable
	public InstitutionId getInstitutionId() {
		return institutionId;
	}

	public void setInstitutionId(@Nullable InstitutionId institutionId) {
		this.institutionId = institutionId;
	}

	@Nullable
	public String getUrlName() {
		return urlName;
	}

	public void setUrlName(@Nullable String urlName) {
		this.urlName = urlName;
	}

	@Nullable
	public String getImageUrl() {
		return imageUrl;
	}

	public void setImageUrl(@Nullable String imageUrl) {
		this.imageUrl = imageUrl;
	}

	@Nullable
	public String getVideoconferenceUrl() {
		return videoconferenceUrl;
	}

	public void setVideoconferenceUrl(@Nullable String videoconferenceUrl) {
		this.videoconferenceUrl = videoconferenceUrl;
	}

	@Nullable
	public Instant getCreated() {
		return created;
	}

	public void setCreated(@Nullable Instant created) {
		this.created = created;
	}

	@Nullable
	public Instant getLastUpdated() {
		return lastUpdated;
	}

	public void setLastUpdated(@Nullable Instant lastUpdated) {
		this.lastUpdated = lastUpdated;
	}
}
