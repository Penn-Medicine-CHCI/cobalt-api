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

import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lokalized.Strings;
import com.cobaltplatform.api.model.db.Institution;
import com.cobaltplatform.api.util.Formatter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class InstitutionApiResponse {
	@Nonnull
	private final InstitutionId institutionId;
	@Nullable
	private final UUID providerTriageScreeningFlowId;
	@Nonnull
	private final String description;
	@Nullable
	private final String crisisContent;
	@Nullable
	private final String privacyContent;
	@Nullable
	private final String covidContent;
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
		this.providerTriageScreeningFlowId = institution.getProviderTriageScreeningFlowId();
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

	@Nonnull
	public InstitutionId getInstitutionId() {
		return this.institutionId;
	}

	@Nullable
	public UUID getProviderTriageScreeningFlowId() {
		return this.providerTriageScreeningFlowId;
	}

	@Nonnull
	public String getDescription() {
		return this.description;
	}

	@Nullable
	public String getCrisisContent() {
		return this.crisisContent;
	}

	@Nullable
	public String getPrivacyContent() {
		return this.privacyContent;
	}

	@Nullable
	public String getCovidContent() {
		return this.covidContent;
	}

	@Nullable
	public Boolean getRequireConsentForm() {
		return this.requireConsentForm;
	}

	@Nullable
	public String getConsentFormContent() {
		return this.consentFormContent;
	}

	@Nullable
	public String getCalendarDescription() {
		return this.calendarDescription;
	}

	@Nullable
	public Boolean getSupportEnabled() {
		return this.supportEnabled;
	}

	@Nullable
	public String getWellBeingContent() {
		return this.wellBeingContent;
	}

	@Nullable
	public Boolean getSsoEnabled() {
		return this.ssoEnabled;
	}

	@Nullable
	public Boolean getEmailEnabled() {
		return this.emailEnabled;
	}

	@Nullable
	public Boolean getAnonymousEnabled() {
		return this.anonymousEnabled;
	}
}