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
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.ScreeningAnswerOption;
import com.cobaltplatform.api.model.db.ScreeningConfirmationPrompt;
import com.cobaltplatform.api.model.db.ScreeningConfirmationPrompt.ScreeningConfirmationPromptCallout;
import com.cobaltplatform.api.model.db.ScreeningFlowVersion;
import com.cobaltplatform.api.model.db.ScreeningQuestion;
import com.cobaltplatform.api.model.db.ScreeningSession;
import com.cobaltplatform.api.service.AlertService;
import com.cobaltplatform.api.service.InstitutionService;
import com.cobaltplatform.api.util.AwsSecretManagerClient;
import com.cobaltplatform.api.util.JsonMapper;
import com.cobaltplatform.api.util.db.DatabaseProvider;
import com.pyranid.Database;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@Singleton
@ThreadSafe
public class CobaltCoursesEnterprisePlugin extends DefaultEnterprisePlugin {
	@Nonnull
	protected static final String BEHAVIOR_BRIDGE_METADATA_KEY = "behaviorBridge";
	@Nonnull
	protected static final String BEHAVIOR_BRIDGE_PROVIDER_QUESTION_METADATA_KEY = "behaviorBridgeProviderQuestion";
	@Nonnull
	protected static final String BEHAVIOR_BRIDGE_PROVIDER_ANSWER_METADATA_KEY = "behaviorBridgeProvider";
	@Nonnull
	protected static final String BEHAVIOR_BRIDGE_IS_PROVIDER_METADATA_KEY = "isProvider";
	@Nonnull
	protected static final String BEHAVIOR_BRIDGE_CONTINUING_EDUCATION_ALERT_ID_METADATA_KEY = "continuingEducationAlertId";
	@Nonnull
	protected static final String BEHAVIOR_BRIDGE_CONTINUING_EDUCATION_SCREENING_CONFIRMATION_PROMPT_ID_METADATA_KEY = "continuingEducationScreeningConfirmationPromptId";
	@Nonnull
	protected static final String BEHAVIOR_BRIDGE_CONTINUING_EDUCATION_CALLOUT_TITLE = "Continuing Education";
	@Nonnull
	protected static final String BEHAVIOR_BRIDGE_CONTINUING_EDUCATION_CALLOUT_DESCRIPTION = "You can find more information about Continuing Education credits by clicking the link in the Behavior Bridge footer.";

	@Nonnull
	private final Provider<AlertService> alertServiceProvider;
	@Nonnull
	private final DatabaseProvider databaseProvider;
	@Nonnull
	private final JsonMapper jsonMapper;

	@Inject
	public CobaltCoursesEnterprisePlugin(@Nonnull InstitutionService institutionService,
																			 @Nonnull AwsSecretManagerClient awsSecretManagerClient,
																			 @Nonnull Configuration configuration,
																			 @Nonnull Provider<AlertService> alertServiceProvider,
																			 @Nonnull DatabaseProvider databaseProvider,
																			 @Nonnull JsonMapper jsonMapper) {
		super(institutionService, awsSecretManagerClient, configuration);
		requireNonNull(alertServiceProvider);
		requireNonNull(databaseProvider);
		requireNonNull(jsonMapper);

		this.alertServiceProvider = alertServiceProvider;
		this.databaseProvider = databaseProvider;
		this.jsonMapper = jsonMapper;
	}

	@Nonnull
	@Override
	public InstitutionId getInstitutionId() {
		return InstitutionId.COBALT_COURSES;
	}

	@Override
	public void postProcessScreeningAnswers(@Nonnull Account createdByAccount,
																					@Nullable Account targetAccount,
																					@Nonnull ScreeningSession screeningSession,
																					@Nonnull ScreeningFlowVersion screeningFlowVersion,
																					@Nonnull ScreeningQuestion screeningQuestion,
																					@Nonnull List<ScreeningAnswerOption> selectedScreeningAnswerOptions) {
		requireNonNull(createdByAccount);
		requireNonNull(screeningSession);
		requireNonNull(screeningFlowVersion);
		requireNonNull(screeningQuestion);
		requireNonNull(selectedScreeningAnswerOptions);

		if (targetAccount == null)
			return;

		UUID targetAccountId = targetAccount.getAccountId();

		if (targetAccountId == null)
			return;

		boolean providerQuestion = metadataBoolean(screeningQuestion.getMetadata(), BEHAVIOR_BRIDGE_PROVIDER_QUESTION_METADATA_KEY).orElse(false)
				|| selectedScreeningAnswerOptions.stream()
				.anyMatch(screeningAnswerOption -> metadataBoolean(screeningAnswerOption.getMetadata(), BEHAVIOR_BRIDGE_PROVIDER_ANSWER_METADATA_KEY).isPresent());

		if (!providerQuestion)
			return;

		boolean provider = selectedScreeningAnswerOptions.stream()
				.anyMatch(screeningAnswerOption -> metadataBoolean(screeningAnswerOption.getMetadata(), BEHAVIOR_BRIDGE_PROVIDER_ANSWER_METADATA_KEY).orElse(false));

		Map<String, Object> behaviorBridgeMetadata = new HashMap<>();
		behaviorBridgeMetadata.put(BEHAVIOR_BRIDGE_IS_PROVIDER_METADATA_KEY, provider);
		if (screeningFlowVersion.getPreCompletionScreeningConfirmationPromptId() != null)
			behaviorBridgeMetadata.put(BEHAVIOR_BRIDGE_CONTINUING_EDUCATION_SCREENING_CONFIRMATION_PROMPT_ID_METADATA_KEY,
					screeningFlowVersion.getPreCompletionScreeningConfirmationPromptId().toString());

		Optional<UUID> continuingEducationAlertId = metadataUuid(screeningQuestion, selectedScreeningAnswerOptions, BEHAVIOR_BRIDGE_CONTINUING_EDUCATION_ALERT_ID_METADATA_KEY);
		continuingEducationAlertId.ifPresent(alertId -> behaviorBridgeMetadata.put(BEHAVIOR_BRIDGE_CONTINUING_EDUCATION_ALERT_ID_METADATA_KEY, alertId.toString()));

		mergeBehaviorBridgeMetadata(targetAccountId, behaviorBridgeMetadata);

		continuingEducationAlertId.ifPresent(alertId -> {
			if (provider)
				getAlertService().createAccountAlert(targetAccountId, alertId);
			else
				getAlertService().deactivateAccountAlert(targetAccountId, alertId);
		});
	}

	@Nonnull
	@Override
	public ScreeningConfirmationPrompt customizeScreeningConfirmationPrompt(@Nonnull Account createdByAccount,
																																				 @Nullable Account targetAccount,
																																				 @Nonnull ScreeningSession screeningSession,
																																				 @Nonnull ScreeningFlowVersion screeningFlowVersion,
																																				 @Nonnull ScreeningConfirmationPrompt screeningConfirmationPrompt) {
		requireNonNull(createdByAccount);
		requireNonNull(screeningSession);
		requireNonNull(screeningFlowVersion);
		requireNonNull(screeningConfirmationPrompt);

		if (targetAccount == null)
			return screeningConfirmationPrompt;

		Map<String, Object> behaviorBridgeMetadata = metadataObject(targetAccount.getMetadataAsMap(), BEHAVIOR_BRIDGE_METADATA_KEY);
		boolean provider = metadataBoolean(behaviorBridgeMetadata, BEHAVIOR_BRIDGE_IS_PROVIDER_METADATA_KEY).orElse(false);

		if (!provider)
			return screeningConfirmationPrompt;

		UUID continuingEducationScreeningConfirmationPromptId = metadataUuid(behaviorBridgeMetadata,
				BEHAVIOR_BRIDGE_CONTINUING_EDUCATION_SCREENING_CONFIRMATION_PROMPT_ID_METADATA_KEY).orElse(null);

		if (continuingEducationScreeningConfirmationPromptId == null
				|| !continuingEducationScreeningConfirmationPromptId.equals(screeningConfirmationPrompt.getScreeningConfirmationPromptId()))
			return screeningConfirmationPrompt;

		List<ScreeningConfirmationPromptCallout> callouts = new ArrayList<>(screeningConfirmationPrompt.getCallouts());
		callouts.add(new ScreeningConfirmationPromptCallout(BEHAVIOR_BRIDGE_CONTINUING_EDUCATION_CALLOUT_TITLE, BEHAVIOR_BRIDGE_CONTINUING_EDUCATION_CALLOUT_DESCRIPTION, null, null));
		screeningConfirmationPrompt.setCallouts(callouts);

		return screeningConfirmationPrompt;
	}

	protected void mergeBehaviorBridgeMetadata(@Nonnull UUID accountId,
																						 @Nonnull Map<String, Object> behaviorBridgeMetadata) {
		requireNonNull(accountId);
		requireNonNull(behaviorBridgeMetadata);

		getDatabase().execute("""
				UPDATE account
				SET metadata = jsonb_set(
					coalesce(metadata,'{}')::jsonb,
					'{behaviorBridge}',
					(
						CASE
							WHEN jsonb_typeof(coalesce(metadata,'{}')::jsonb->'behaviorBridge') = 'object'
								THEN coalesce(metadata,'{}')::jsonb->'behaviorBridge'
							ELSE '{}'::jsonb
						END
					) || CAST(? AS JSONB),
					TRUE
				)
				WHERE account_id=?
				""", getJsonMapper().toJson(behaviorBridgeMetadata), accountId);
	}

	@Nonnull
	protected Optional<UUID> metadataUuid(@Nullable Map<String, Object> metadata,
																				@Nonnull String key) {
		requireNonNull(key);

		return metadataString(metadata, key)
				.flatMap(value -> {
					try {
						return Optional.of(UUID.fromString(value));
					} catch (IllegalArgumentException e) {
						return Optional.empty();
					}
				});
	}

	@Nonnull
	protected Optional<UUID> metadataUuid(@Nonnull ScreeningQuestion screeningQuestion,
																				@Nonnull List<ScreeningAnswerOption> screeningAnswerOptions,
																				@Nonnull String key) {
		requireNonNull(screeningQuestion);
		requireNonNull(screeningAnswerOptions);
		requireNonNull(key);

		Optional<UUID> selectedAnswerOptionMetadataUuid = screeningAnswerOptions.stream()
				.map(screeningAnswerOption -> metadataUuid(screeningAnswerOption.getMetadata(), key))
				.filter(Optional::isPresent)
				.map(Optional::get)
				.findFirst();

		if (selectedAnswerOptionMetadataUuid.isPresent())
			return selectedAnswerOptionMetadataUuid;

		return metadataUuid(screeningQuestion.getMetadata(), key);
	}

	@Nonnull
	protected Optional<String> metadataString(@Nullable Map<String, Object> metadata,
																						@Nonnull String key) {
		requireNonNull(key);

		if (metadata == null)
			return Optional.empty();

		Object value = metadata.get(key);

		if (value instanceof String stringValue)
			return Optional.ofNullable(trimToNull(stringValue));

		return Optional.ofNullable(value)
				.map(Object::toString)
				.map(stringValue -> trimToNull(stringValue));
	}

	@Nonnull
	protected Optional<Boolean> metadataBoolean(@Nullable Map<String, Object> metadata,
																						 @Nonnull String key) {
		requireNonNull(key);

		if (metadata == null)
			return Optional.empty();

		Object value = metadata.get(key);

		if (value instanceof Boolean booleanValue)
			return Optional.of(booleanValue);

		if (value instanceof String stringValue)
			return Optional.of(Boolean.parseBoolean(stringValue));

		return Optional.empty();
	}

	@Nonnull
	protected Map<String, Object> metadataObject(@Nullable Map<String, Object> metadata,
																						 @Nonnull String key) {
		requireNonNull(key);

		if (metadata == null)
			return Map.of();

		Object value = metadata.get(key);

		if (!(value instanceof Map<?, ?> valueAsMap))
			return Map.of();

		Map<String, Object> metadataObject = new HashMap<>();

		for (Map.Entry<?, ?> entry : valueAsMap.entrySet()) {
			if (entry.getKey() instanceof String entryKey)
				metadataObject.put(entryKey, entry.getValue());
		}

		return metadataObject;
	}

	@Nonnull
	protected AlertService getAlertService() {
		return this.alertServiceProvider.get();
	}

	@Nonnull
	protected Database getDatabase() {
		return this.databaseProvider.get();
	}

	@Nonnull
	protected JsonMapper getJsonMapper() {
		return this.jsonMapper;
	}
}
