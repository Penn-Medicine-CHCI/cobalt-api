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

import com.cobaltplatform.api.model.db.GroupSessionSystem.GroupSessionSystemId;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.ZoneId;
import java.util.Locale;

import static java.lang.String.format;

/**
 * @author Transmogrify LLC.
 */
@NotThreadSafe
public class Institution {
	@Nullable
	private InstitutionId institutionId;
	@Nullable
	private GroupSessionSystemId groupSessionSystemId;
	@Nullable
	private String description;
	@Nullable
	private ZoneId timeZone;
	@Nullable
	private Locale locale;
	@Nullable
	private String crisisContent;
	@Nullable
	private String privacyContent;
	@Nullable
	private String covidContent;
	@Nullable
	private Boolean requireConsentForm;
	@Nullable
	private String consentFormContent;
	@Nullable
	private String calendarDescription;
	@Nullable
	private Boolean supportEnabled;
	@Nullable
	private String wellBeingContent;
	@Nullable
	private String name;
	@Nullable
	private Boolean ssoEnabled;
	@Nullable
	private Boolean emailEnabled;
	@Nullable
	private Boolean anonymousEnabled;
	@Nonnull
	private Long accessTokenExpirationInMinutes;
	@Nonnull
	private Long accessTokenShortExpirationInMinutes;

	public enum InstitutionId {
		COBALT
	}

	@Override
	public String toString() {
		return format("%s{institutionId=%s, description=%s}", getClass().getSimpleName(), getInstitutionId(), getDescription());
	}

	@Nullable
	public InstitutionId getInstitutionId() {
		return institutionId;
	}

	public void setInstitutionId(@Nullable InstitutionId institutionId) {
		this.institutionId = institutionId;
	}

	@Nullable
	public String getDescription() {
		return description;
	}

	public void setDescription(@Nullable String description) {
		this.description = description;
	}

	@Nullable
	public GroupSessionSystemId getGroupSessionSystemId() {
		return groupSessionSystemId;
	}

	public void setGroupSessionSystemId(@Nullable GroupSessionSystemId groupSessionSystemId) {
		this.groupSessionSystemId = groupSessionSystemId;
	}

	@Nullable
	public ZoneId getTimeZone() {
		return timeZone;
	}

	public void setTimeZone(@Nullable ZoneId timeZone) {
		this.timeZone = timeZone;
	}

	@Nullable
	public Locale getLocale() {
		return locale;
	}

	public void setLocale(@Nullable Locale locale) {
		this.locale = locale;
	}

	@Nullable
	public String getCrisisContent() {
		return crisisContent;
	}

	public void setCrisisContent(@Nullable String crisisContent) {
		this.crisisContent = crisisContent;
	}

	@Nullable
	public String getPrivacyContent() {
		return privacyContent;
	}

	public void setPrivacyContent(@Nullable String privacyContent) {
		this.privacyContent = privacyContent;
	}

	@Nullable
	public String getCovidContent() {
		return covidContent;
	}

	public void setCovidContent(@Nullable String covidContent) {
		this.covidContent = covidContent;
	}

	@Nullable
	public Boolean getRequireConsentForm() {
		return requireConsentForm;
	}

	public void setRequireConsentForm(@Nullable Boolean requireConsentForm) {
		this.requireConsentForm = requireConsentForm;
	}

	@Nullable
	public String getConsentFormContent() {
		return consentFormContent;
	}

	public void setConsentFormContent(@Nullable String consentFormContent) {
		this.consentFormContent = consentFormContent;
	}

	@Nullable
	public String getCalendarDescription() {
		return calendarDescription;
	}

	public void setCalendarDescription(@Nullable String calendarDescription) {
		this.calendarDescription = calendarDescription;
	}

	@Nullable
	public Boolean getSupportEnabled() {
		return supportEnabled;
	}

	public void setSupportEnabled(@Nullable Boolean supportEnabled) {
		this.supportEnabled = supportEnabled;
	}

	@Nullable
	public String getWellBeingContent() {
		return wellBeingContent;
	}

	public void setWellBeingContent(@Nullable String wellBeingContent) {
		this.wellBeingContent = wellBeingContent;
	}

	@Nullable
	public String getName() {
		return name;
	}

	public void setName(@Nullable String name) {
		this.name = name;
	}

	@Nullable
	public Boolean getSsoEnabled() {
		return ssoEnabled;
	}

	public void setSsoEnabled(@Nullable Boolean ssoEnabled) {
		this.ssoEnabled = ssoEnabled;
	}

	@Nullable
	public Boolean getEmailEnabled() {
		return emailEnabled;
	}

	public void setEmailEnabled(@Nullable Boolean emailEnabled) {
		this.emailEnabled = emailEnabled;
	}

	@Nullable
	public Boolean getAnonymousEnabled() {
		return anonymousEnabled;
	}

	public void setAnonymousEnabled(@Nullable Boolean anonymousEnabled) {
		this.anonymousEnabled = anonymousEnabled;
	}

	@Nonnull
	public Long getAccessTokenExpirationInMinutes() {
		return accessTokenExpirationInMinutes;
	}

	public void setAccessTokenExpirationInMinutes(@Nonnull Long accessTokenExpirationInMinutes) {
		this.accessTokenExpirationInMinutes = accessTokenExpirationInMinutes;
	}

	@Nonnull
	public Long getAccessTokenShortExpirationInMinutes() {
		return accessTokenShortExpirationInMinutes;
	}

	public void setAccessTokenShortExpirationInMinutes(@Nonnull Long accessTokenShortExpirationInMinutes) {
		this.accessTokenShortExpirationInMinutes = accessTokenShortExpirationInMinutes;
	}
}