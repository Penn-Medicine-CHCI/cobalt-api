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
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify LLC.
 */
@NotThreadSafe
public class Institution {
	@Nonnull
	private static final Gson GSON;

	static {
		GSON = new GsonBuilder()
				.setPrettyPrinting()
				.disableHtmlEscaping()
				.create();
	}

	@Nullable
	private InstitutionId institutionId;
	@Nullable
	private GroupSessionSystemId groupSessionSystemId;
	@Nullable
	private UUID providerTriageScreeningFlowId;
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
	@Nullable
	private String metadata;
	@Nonnull
	private Long accessTokenExpirationInMinutes;
	@Nonnull
	private Long accessTokenShortExpirationInMinutes;
	@Nonnull
	private Long anonAccessTokenExpirationInMinutes;
	@Nonnull
	private Long anonAccessTokenShortExpirationInMinutes;

	public enum InstitutionId {
		COBALT
	}

	@Override
	public String toString() {
		return format("%s{institutionId=%s, description=%s}", getClass().getSimpleName(), getInstitutionId(), getDescription());
	}

	@Nonnull
	public Map<String, Object> getMetadataAsMap() {
		String metadata = trimToNull(getMetadata());
		return metadata == null ? Collections.emptyMap() : getGson().fromJson(metadata, new TypeToken<Map<String, Object>>() {
		}.getType());
	}

	@Nonnull
	public StandardMetadata getStandardMetadata() {
		String metadata = trimToNull(getMetadata());
		return metadata == null ? StandardMetadata.emptyInstance() : getGson().fromJson(metadata, StandardMetadata.class);
	}

	@Nonnull
	protected Gson getGson() {
		return GSON;
	}

	@NotThreadSafe
	public static class StandardMetadata {
		@Nullable
		private UUID defaultCrisisInteractionId;
		@Nullable
		private UUID defaultRoleRequestInteractionId;
		@Nullable
		private List<Way2HealthIncidentTrackingConfig> way2HealthIncidentTrackingConfigs;

		@Nonnull
		public static StandardMetadata emptyInstance() {
			StandardMetadata standardMetadata = new StandardMetadata();
			standardMetadata.setWay2HealthIncidentTrackingConfigs(Collections.emptyList());
			return standardMetadata;
		}

		@NotThreadSafe
		public static class Way2HealthIncidentTrackingConfig {
			@Nullable
			private Long studyId;
			@Nullable
			private String type;
			@Nullable
			private UUID interactionId;
			@Nullable
			private Boolean enabled;

			@Nullable
			public Long getStudyId() {
				return studyId;
			}

			public void setStudyId(@Nullable Long studyId) {
				this.studyId = studyId;
			}

			@Nullable
			public String getType() {
				return type;
			}

			public void setType(@Nullable String type) {
				this.type = type;
			}

			@Nullable
			public UUID getInteractionId() {
				return interactionId;
			}

			public void setInteractionId(@Nullable UUID interactionId) {
				this.interactionId = interactionId;
			}

			@Nullable
			public Boolean getEnabled() {
				return enabled;
			}

			public void setEnabled(@Nullable Boolean enabled) {
				this.enabled = enabled;
			}
		}

		@Nullable
		public UUID getDefaultCrisisInteractionId() {
			return defaultCrisisInteractionId;
		}

		public void setDefaultCrisisInteractionId(@Nullable UUID defaultCrisisInteractionId) {
			this.defaultCrisisInteractionId = defaultCrisisInteractionId;
		}

		@Nullable
		public UUID getDefaultRoleRequestInteractionId() {
			return defaultRoleRequestInteractionId;
		}

		public void setDefaultRoleRequestInteractionId(@Nullable UUID defaultRoleRequestInteractionId) {
			this.defaultRoleRequestInteractionId = defaultRoleRequestInteractionId;
		}

		@Nullable
		public List<Way2HealthIncidentTrackingConfig> getWay2HealthIncidentTrackingConfigs() {
			return way2HealthIncidentTrackingConfigs;
		}

		public void setWay2HealthIncidentTrackingConfigs(@Nullable List<Way2HealthIncidentTrackingConfig> way2HealthIncidentTrackingConfigs) {
			this.way2HealthIncidentTrackingConfigs = way2HealthIncidentTrackingConfigs;
		}
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
	public UUID getProviderTriageScreeningFlowId() {
		return this.providerTriageScreeningFlowId;
	}

	public void setProviderTriageScreeningFlowId(@Nullable UUID providerTriageScreeningFlowId) {
		this.providerTriageScreeningFlowId = providerTriageScreeningFlowId;
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
	public String getMetadata() {
		return metadata;
	}

	public void setMetadata(@Nullable String metadata) {
		this.metadata = metadata;
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

	@Nonnull
	public Long getAnonAccessTokenExpirationInMinutes() {
		return anonAccessTokenExpirationInMinutes;
	}

	public void setAnonAccessTokenExpirationInMinutes(@Nonnull Long anonAccessTokenExpirationInMinutes) {
		this.anonAccessTokenExpirationInMinutes = anonAccessTokenExpirationInMinutes;
	}

	@Nonnull
	public Long getAnonAccessTokenShortExpirationInMinutes() {
		return anonAccessTokenShortExpirationInMinutes;
	}

	public void setAnonAccessTokenShortExpirationInMinutes(@Nonnull Long anonAccessTokenShortExpirationInMinutes) {
		this.anonAccessTokenShortExpirationInMinutes = anonAccessTokenShortExpirationInMinutes;
	}
}