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

package com.cobaltplatform.api.model.api.response;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lokalized.Strings;
import com.cobaltplatform.api.model.db.Institution;
import com.cobaltplatform.api.util.Formatter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class InstitutionApiResponse {
	@Nullable
	private Institution.InstitutionId institutionId;
	@Nullable
	private String description;
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
	private Boolean ssoEnabled;
	@Nullable
	private Boolean emailEnabled;
	@Nullable
	private Boolean anonymousEnabled;


	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface InstitutionApiResponseFactory {
		@Nonnull
		InstitutionApiResponse create(@Nonnull Institution institution);
	}

	@AssistedInject
	public InstitutionApiResponse(@Nonnull Formatter formatter,
																@Nonnull Strings strings,
																@Assisted @Nonnull Institution institution) {
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(institution);

		this.institutionId = institution.getInstitutionId();
		this.description = institution.getDescription();
		this.crisisContent = institution.getCrisisContent();
		this.privacyContent = institution.getPrivacyContent();
		this.covidContent = institution.getCovidContent();
		this.requireConsentForm = institution.getRequireConsentForm();
		this.consentFormContent = institution.getConsentFormContent();
		this.calendarDescription = institution.getCalendarDescription();
		this.supportEnabled = institution.getSupportEnabled();
		this.wellBeingContent = institution.getWellBeingContent();
		this.ssoEnabled = institution.getSsoEnabled();
		this.anonymousEnabled = institution.getAnonymousEnabled();
		this.emailEnabled = institution.getEmailEnabled();
	}

	@Nullable
	public Institution.InstitutionId getInstitutionId() {
		return institutionId;
	}

	@Nullable
	public String getDescription() {
		return description;
	}

	@Nullable
	public String getCrisisContent() {
		return crisisContent;
	}

	@Nullable
	public String getPrivacyContent() {
		return privacyContent;
	}

	@Nullable
	public String getCovidContent() {
		return covidContent;
	}

	@Nullable
	public Boolean getRequireConsentForm() {
		return requireConsentForm;
	}

	@Nullable
	public String getConsentFormContent() {
		return consentFormContent;
	}

	@Nullable
	public String getCalendarDescription() {
		return calendarDescription;
	}

	@Nullable
	public Boolean getSupportEnabled() {
		return supportEnabled;
	}

	@Nullable
	public String getWellBeingContent() {
		return wellBeingContent;
	}

	@Nullable
	public Boolean getSsoEnabled() {
		return ssoEnabled;
	}

	@Nullable
	public Boolean getEmailEnabled() {
		return emailEnabled;
	}

	@Nullable
	public Boolean getAnonymousEnabled() {
		return anonymousEnabled;
	}
}