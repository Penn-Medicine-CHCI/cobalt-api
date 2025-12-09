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

package com.cobaltplatform.api.integration.enterprise;

import com.cobaltplatform.api.Configuration;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.Content;
import com.cobaltplatform.api.model.db.CronJob;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.InstitutionFeatureInstitutionReferrer;
import com.cobaltplatform.api.model.db.ScreeningAnswerContentHint.ScreeningAnswerContentHintId;
import com.cobaltplatform.api.model.db.ScreeningQuestion;
import com.cobaltplatform.api.model.db.Tag;
import com.cobaltplatform.api.service.ContentService;
import com.cobaltplatform.api.service.InstitutionService;
import com.cobaltplatform.api.util.AwsSecretManagerClient;
import com.cobaltplatform.api.util.GsonUtility;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Singleton
@ThreadSafe
public class CobaltEnterprisePlugin extends DefaultEnterprisePlugin {
	@Nonnull
	private final ContentService contentService;
	@Nonnull
	private final Gson gson;

	@Inject
	public CobaltEnterprisePlugin(@Nonnull InstitutionService institutionService,
																@Nonnull AwsSecretManagerClient awsSecretManagerClient,
																@Nonnull ContentService contentService,
																@Nonnull Configuration configuration) {
		super(institutionService, awsSecretManagerClient, configuration);
		this.contentService = contentService;

		GsonBuilder gsonBuilder = new GsonBuilder()
				.setPrettyPrinting()
				.disableHtmlEscaping();

		GsonUtility.applyDefaultTypeAdapters(gsonBuilder);

		this.gson = gsonBuilder.create();
	}

	@Nonnull
	@Override
	public InstitutionId getInstitutionId() {
		return InstitutionId.COBALT;
	}

	@Nonnull
	@Override
	public List<Content> recommendedContentForAccountId(@Nullable UUID accountId) {
		if (accountId == null)
			return Collections.emptyList();

		// Naive implementation for our COBALT institution - return all the content
		return getContentService().findVisibleContentByAccountId(accountId);
	}

	@Nonnull
	protected ContentService getContentService() {
		return this.contentService;
	}

	@Override
	public Tag applyCustomizationsToTag(@Nonnull Tag tag) {
		requireNonNull(tag);

		// Show how we might override tag display
		if ("Physical Health".equals(tag.getName())) {
			tag.setName("Example Tag Override");
			tag.setDescription("Example Tag description override");
		}

		return tag;
	}

	@Override
	public List<InstitutionFeatureInstitutionReferrer> applyCustomizationsToInstitutionFeatureInstitutionReferrers(@Nonnull List<InstitutionFeatureInstitutionReferrer> institutionFeatureInstitutionReferrers,
																																																								 @Nonnull Account account) {
		requireNonNull(institutionFeatureInstitutionReferrers);
		requireNonNull(account);

		// Just for testing, if we don't know your location, we don't show referrers
		if (account.getInstitutionLocationId() == null)
			return List.of();

		return institutionFeatureInstitutionReferrers;
	}

	@Nonnull
	@Override
	public Map<String, Object> customizeScreeningQuestionMetadata(@Nullable Account account,
																																@Nonnull ScreeningQuestion screeningQuestion,
																																@Nonnull Map<String, Object> metadata) {
		requireNonNull(screeningQuestion);
		requireNonNull(metadata);

		if (account == null)
			return metadata;

		Map<String, Object> mutableMetadata = new HashMap<>(metadata);

		if (screeningQuestion.getScreeningAnswerContentHintId() == ScreeningAnswerContentHintId.EMAIL_ADDRESS)
			mutableMetadata.put("prepopulatedEmailAddress", account.getEmailAddress());

		return mutableMetadata;
	}

	@Override
	public void runCronJob(@Nonnull CronJob cronJob) {
		requireNonNull(cronJob);

		if ("MOCK_SEND_REPORT_EMAIL".equals(cronJob.getCallbackType())) {
			MockSendReportEmailCronPayload payload = getGson().fromJson(cronJob.getCallbackPayload(), MockSendReportEmailCronPayload.class);
			getLogger().info("Cron job run for {} in {}: pretending to send a report email to these addresses: {}",
					cronJob.getCallbackType(), getInstitutionId(), payload.getEmailAddresses());
		} else if ("MOCK_MATERIALIZED_VIEW_REFRESH".equals(cronJob.getCallbackType())) {
			MockMaterializedViewRefreshCronPayload payload = getGson().fromJson(cronJob.getCallbackPayload(), MockMaterializedViewRefreshCronPayload.class);
			getLogger().info("Cron job run for {} in {}: pretending to refresh materialized view '{}'",
					cronJob.getCallbackType(), getInstitutionId(), payload.getMaterializedViewName());

			// Exercise error workflow if special data is configured
			if (payload.getMaterializedViewName().equals("v_example_error"))
				throw new RuntimeException("Example of throwing an exception during cron execution");
		} else {
			throw new IllegalArgumentException(format("Unexpected cron job callback type '%s'", cronJob.getCallbackType()));
		}
	}

	@NotThreadSafe
	protected static class MockMaterializedViewRefreshCronPayload {
		@Nullable
		private String materializedViewName;

		@Nullable
		public String getMaterializedViewName() {
			return this.materializedViewName;
		}

		public void setMaterializedViewName(@Nullable String materializedViewName) {
			this.materializedViewName = materializedViewName;
		}
	}

	@NotThreadSafe
	protected static class MockSendReportEmailCronPayload {
		@Nullable
		private List<String> emailAddresses;

		@Nullable
		public List<String> getEmailAddresses() {
			return this.emailAddresses;
		}

		public void setEmailAddresses(@Nullable List<String> emailAddresses) {
			this.emailAddresses = emailAddresses;
		}
	}

	@Nonnull
	protected Gson getGson() {
		return this.gson;
	}
}
