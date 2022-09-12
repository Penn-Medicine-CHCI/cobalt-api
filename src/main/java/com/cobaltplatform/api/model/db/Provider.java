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

import com.cobaltplatform.api.model.db.EpicAppointmentFilter.EpicAppointmentFilterId;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.SchedulingSystem.SchedulingSystemId;
import com.cobaltplatform.api.model.db.VideoconferencePlatform.VideoconferencePlatformId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Locale;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class Provider {
	@Nullable
	private UUID providerId;
	@Nullable
	private InstitutionId institutionId;
	@Nullable
	private SchedulingSystemId schedulingSystemId;
	@Nullable
	private String name;
	@Nullable
	private String title;
	@Nullable
	private String entity;
	@Nullable
	private String clinic;
	@Nullable
	private String license;
	@Nullable
	private String specialty;
	@Nullable
	private String emailAddress;
	@Nullable
	private String imageUrl;
	@Nullable
	private String bioUrl;
	@Nullable
	private String bio;
	@Nullable
	private Locale locale;
	@Nullable
	private ZoneId timeZone;
	@Nullable
	private Long acuityCalendarId;
	@Nullable
	private UUID defaultAppointmentTypeId;
	@Nullable
	private Long bluejeansUserId;
	@Nullable
	private String epicProviderId;
	@Nullable
	private String epicProviderIdType;
	@Nullable
	private EpicAppointmentFilterId epicAppointmentFilterId;
	@Nullable
	private Boolean active;
	@Nullable
	private String tags; // e.g. ["Experienced Coach", "Calming Voice"]
	@Nullable
	private VideoconferencePlatformId videoconferencePlatformId;
	@Nullable
	private String videoconferenceUrl;
	@Nullable
	private Integer schedulingLeadTimeInHours;
	@Nullable
	private Instant created;
	@Nullable
	private Instant lastUpdated;

	@Nullable
	public UUID getProviderId() {
		return providerId;
	}

	public void setProviderId(@Nullable UUID providerId) {
		this.providerId = providerId;
	}

	@Nullable
	public InstitutionId getInstitutionId() {
		return institutionId;
	}

	public void setInstitutionId(@Nullable InstitutionId institutionId) {
		this.institutionId = institutionId;
	}

	@Nullable
	public SchedulingSystemId getSchedulingSystemId() {
		return schedulingSystemId;
	}

	public void setSchedulingSystemId(@Nullable SchedulingSystemId schedulingSystemId) {
		this.schedulingSystemId = schedulingSystemId;
	}

	@Nullable
	public String getName() {
		return name;
	}

	public void setName(@Nullable String name) {
		this.name = name;
	}

	@Nullable
	public String getTitle() {
		return title;
	}

	public void setTitle(@Nullable String title) {
		this.title = title;
	}

	@Nullable
	public String getEntity() {
		return entity;
	}

	public void setEntity(@Nullable String entity) {
		this.entity = entity;
	}

	@Nullable
	public String getClinic() {
		return clinic;
	}

	public void setClinic(@Nullable String clinic) {
		this.clinic = clinic;
	}

	@Nullable
	public String getLicense() {
		return license;
	}

	public void setLicense(@Nullable String license) {
		this.license = license;
	}

	@Nullable
	public String getSpecialty() {
		return specialty;
	}

	public void setSpecialty(@Nullable String specialty) {
		this.specialty = specialty;
	}

	@Nullable
	public String getEmailAddress() {
		return emailAddress;
	}

	public void setEmailAddress(@Nullable String emailAddress) {
		this.emailAddress = emailAddress;
	}

	@Nullable
	public String getImageUrl() {
		return imageUrl;
	}

	public void setImageUrl(@Nullable String imageUrl) {
		this.imageUrl = imageUrl;
	}

	@Nullable
	public String getBioUrl() {
		return bioUrl;
	}

	public void setBioUrl(@Nullable String bioUrl) {
		this.bioUrl = bioUrl;
	}

	@Nullable
	public Locale getLocale() {
		return locale;
	}

	public void setLocale(@Nullable Locale locale) {
		this.locale = locale;
	}

	@Nullable
	public ZoneId getTimeZone() {
		return timeZone;
	}

	public void setTimeZone(@Nullable ZoneId timeZone) {
		this.timeZone = timeZone;
	}

	@Nullable
	public Long getAcuityCalendarId() {
		return acuityCalendarId;
	}

	public void setAcuityCalendarId(@Nullable Long acuityCalendarId) {
		this.acuityCalendarId = acuityCalendarId;
	}

	@Nullable
	public UUID getDefaultAppointmentTypeId() {
		return defaultAppointmentTypeId;
	}

	public void setDefaultAppointmentTypeId(@Nullable UUID defaultAppointmentTypeId) {
		this.defaultAppointmentTypeId = defaultAppointmentTypeId;
	}

	@Nullable
	public Long getBluejeansUserId() {
		return bluejeansUserId;
	}

	public void setBluejeansUserId(@Nullable Long bluejeansUserId) {
		this.bluejeansUserId = bluejeansUserId;
	}

	@Nullable
	public Boolean getActive() {
		return active;
	}

	public void setActive(@Nullable Boolean active) {
		this.active = active;
	}

	@Nullable
	public String getTags() {
		return tags;
	}

	public void setTags(@Nullable String tags) {
		this.tags = tags;
	}

	@Nullable
	public String getEpicProviderId() {
		return epicProviderId;
	}

	public void setEpicProviderId(@Nullable String epicProviderId) {
		this.epicProviderId = epicProviderId;
	}

	@Nullable
	public String getEpicProviderIdType() {
		return epicProviderIdType;
	}

	public void setEpicProviderIdType(@Nullable String epicProviderIdType) {
		this.epicProviderIdType = epicProviderIdType;
	}

	@Nullable
	public EpicAppointmentFilterId getEpicAppointmentFilterId() {
		return epicAppointmentFilterId;
	}

	public void setEpicAppointmentFilterId(@Nullable EpicAppointmentFilterId epicAppointmentFilterId) {
		this.epicAppointmentFilterId = epicAppointmentFilterId;
	}

	@Nullable
	public VideoconferencePlatformId getVideoconferencePlatformId() {
		return videoconferencePlatformId;
	}

	public void setVideoconferencePlatformId(@Nullable VideoconferencePlatformId videoconferencePlatformId) {
		this.videoconferencePlatformId = videoconferencePlatformId;
	}

	@Nullable
	public String getVideoconferenceUrl() {
		return videoconferenceUrl;
	}

	public void setVideoconferenceUrl(@Nullable String videoconferenceUrl) {
		this.videoconferenceUrl = videoconferenceUrl;
	}

	@Nullable
	public Integer getSchedulingLeadTimeInHours() {
		return this.schedulingLeadTimeInHours;
	}

	public void setSchedulingLeadTimeInHours(@Nullable Integer schedulingLeadTimeInHours) {
		this.schedulingLeadTimeInHours = schedulingLeadTimeInHours;
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

	@Nullable
	public String getBio() {
		return bio;
	}

	public void setBio(@Nullable String bio) {
		this.bio = bio;
	}
}