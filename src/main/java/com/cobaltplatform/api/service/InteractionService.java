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

package com.cobaltplatform.api.service;

import com.cobaltplatform.api.Configuration;
import com.cobaltplatform.api.messaging.Message;
import com.cobaltplatform.api.messaging.email.EmailMessage;
import com.cobaltplatform.api.messaging.email.EmailMessageTemplate;
import com.cobaltplatform.api.model.api.request.CreateInteractionInstanceRequest;
import com.cobaltplatform.api.model.api.request.CreateInteractionOptionActionRequest;
import com.cobaltplatform.api.model.api.request.CreateScheduledMessageRequest;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.Institution;
import com.cobaltplatform.api.model.db.Interaction;
import com.cobaltplatform.api.model.db.InteractionInstance;
import com.cobaltplatform.api.model.db.InteractionOption;
import com.cobaltplatform.api.model.db.InteractionOptionAction;
import com.cobaltplatform.api.model.db.ScheduledMessage;
import com.cobaltplatform.api.model.db.ScheduledMessageStatus;
import com.cobaltplatform.api.util.Formatter;
import com.cobaltplatform.api.util.JsonMapper;
import com.cobaltplatform.api.util.ValidationException;
import com.cobaltplatform.api.util.ValidationException.FieldError;
import com.lokalized.Strings;
import com.pyranid.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify LLC.
 */
@Singleton
public class InteractionService {
	@Nonnull
	private final Strings strings;
	@Nonnull
	private final Database database;
	@Nonnull
	private final Logger logger;
	@Nonnull
	private final AccountService accountService;
	@Nonnull
	private final MessageService messageService;
	@Nonnull
	private final InstitutionService institutionService;
	@Nonnull
	private final Formatter formatter;
	@Nonnull
	private final Configuration configuration;
	@Nonnull
	private final JsonMapper jsonMapper;

	@Inject
	public InteractionService(@Nonnull Database database,
														@Nonnull AccountService accountService,
														@Nonnull Strings strings,
														@Nonnull MessageService messageService,
														@Nonnull InstitutionService institutionService,
														@Nonnull Formatter formatter,
														@Nonnull Configuration configuration,
														@Nonnull JsonMapper jsonMapper) {
		requireNonNull(database);
		requireNonNull(accountService);
		requireNonNull(strings);
		requireNonNull(messageService);
		requireNonNull(institutionService);
		requireNonNull(formatter);
		requireNonNull(configuration);
		requireNonNull(jsonMapper);

		this.logger = LoggerFactory.getLogger(getClass());
		this.database = database;
		this.accountService = accountService;
		this.strings = strings;
		this.messageService = messageService;
		this.institutionService = institutionService;
		this.formatter = formatter;
		this.configuration = configuration;
		this.jsonMapper = jsonMapper;
	}

	@Nonnull
	public Optional<InteractionInstance> findInteractionInstanceById(@Nullable UUID interactionInstanceId) {
		if (interactionInstanceId == null)
			return Optional.empty();

		return getDatabase().queryForObject("SELECT * FROM interaction_instance WHERE interaction_instance_id = ?",
				InteractionInstance.class, interactionInstanceId);
	}

	@Nonnull
	public Optional<Interaction> findInteractionById(@Nonnull UUID interactionId) {
		requireNonNull(interactionId);

		return getDatabase().queryForObject("SELECT * FROM interaction WHERE interaction_id = ?",
				Interaction.class, interactionId);
	}

	@Nonnull
	public List<InteractionOption> findInteractionOptionsByInteractionId(@Nonnull UUID interactionId) {
		requireNonNull(interactionId);

		return getDatabase().queryForList("SELECT * FROM interaction_option WHERE interaction_id = ?",
				InteractionOption.class, interactionId);
	}

	@Nonnull
	public Optional<InteractionOption> findInteractionOptionById(@Nullable UUID interactionOptionId) {
		if (interactionOptionId == null)
			return Optional.empty();

		return getDatabase().queryForObject("SELECT * FROM interaction_option WHERE interaction_option_id = ?",
				InteractionOption.class, interactionOptionId);
	}

	@Nonnull
	private void cancelPendingMessagesForInteractionInstance(@Nonnull UUID interactionInstanceId) {
		requireNonNull(interactionInstanceId);

		List<ScheduledMessage> scheduledMessages = messageService.findScheduledMessagesMatchingMetadata(new HashMap<>() {{
			put("interactionInstanceId", interactionInstanceId);
		}});

		for (ScheduledMessage scheduledMessage : scheduledMessages) {
			if (scheduledMessage.getScheduledMessageStatusId().equals(ScheduledMessageStatus.ScheduledMessageStatusId.PENDING))
				messageService.cancelScheduledMessage(scheduledMessage.getScheduledMessageId());
		}
	}

	@Nonnull
	public void markInteractionInstanceComplete(@Nonnull UUID interactionInstanceId) {
		requireNonNull(interactionInstanceId);

		getDatabase().execute("UPDATE interaction_instance SET completed_flag = true, completed_date=now() WHERE interaction_instance_id=?", interactionInstanceId);

		cancelPendingMessagesForInteractionInstance(interactionInstanceId);
	}

	@Nonnull
	public UUID createInteractionInstance(@Nonnull CreateInteractionInstanceRequest request) {
		requireNonNull(request);

		ValidationException validationException = new ValidationException();
		UUID interactionInstanceId = UUID.randomUUID();
		UUID accountId = request.getAccountId();
		UUID interactionId = request.getInteractionId();
		LocalDateTime startDateTime = request.getStartDateTime();
		Map<String, Object> metadata = request.getMetadata() == null ? Collections.emptyMap() : request.getMetadata();
		Map<String, Object> hipaaCompliantMetadata = request.getMetadata() == null ? Collections.emptyMap() : request.getHipaaCompliantMetadata();
		ZoneId timeZone = request.getTimeZone();
		Interaction interaction;

		if (timeZone == null)
			validationException.add(new FieldError("timeZone", getStrings().get("Time zone is required.")));

		if (interactionId == null) {
			validationException.add(new FieldError("interactionId", getStrings().get("Interaction ID is required.")));
		} else {
			interaction = findInteractionById(interactionId).orElse(null);

			if (interaction == null)
				validationException.add(new FieldError("interactionId", getStrings().get("Interaction ID is invalid.")));
		}

		if (startDateTime == null)
			validationException.add(new FieldError("startDateTime", getStrings().get("Start date and time is required.")));

		if (validationException.hasErrors())
			throw validationException;

		getDatabase().execute("INSERT INTO interaction_instance (interaction_instance_id, interaction_id, account_id, start_date_time, "
						+ "time_zone, metadata, hipaa_compliant_metadata) VALUES (?,?,?,?,?,CAST (? AS JSONB),CAST (? AS JSONB))", interactionInstanceId,
				interactionId, accountId, startDateTime, timeZone, getJsonMapper().toJson(metadata), getJsonMapper().toJson(hipaaCompliantMetadata));

		createInteractionInstanceMessages(interactionInstanceId, startDateTime, timeZone);

		return interactionInstanceId;
	}

	@Nonnull
	protected void createInteractionInstanceMessages(@Nonnull UUID interactionInstanceId,
																									 @Nonnull LocalDateTime startDateTime,
																									 @Nonnull ZoneId timeZone) {
		requireNonNull(interactionInstanceId);
		requireNonNull(startDateTime);
		requireNonNull(timeZone);

		InteractionInstance interactionInstance = findInteractionInstanceById(interactionInstanceId).get();
		Interaction interaction = findInteractionById(interactionInstance.getInteractionId()).get();
		Institution institution = getInstitutionService().findInstitutionById(interaction.getInstitutionId()).get();

		Integer frequencyInMinutes = interaction.getFrequencyInMinutes();
		LocalDateTime scheduledAt = startDateTime;
		Integer optionActionCount = findOptionActionCount(interactionInstanceId);

		for (int i = optionActionCount; i < interaction.getMaxInteractionCount(); i++) {
			if (i > optionActionCount)
				scheduledAt = scheduledAt.plus(frequencyInMinutes, ChronoUnit.MINUTES);

			LocalDateTime finalScheduledAt = scheduledAt;

			Account.StandardMetadata standardMetadata = new Account.StandardMetadata();
			standardMetadata.setInteractionIds(Set.of(interaction.getInteractionId()));

			List<String> accountsToEmail = getAccountService().findAccountsMatchingMetadata(standardMetadata).stream()
					.map(e -> e.getEmailAddress()).filter(e -> e != null)
					.collect(Collectors.toList());

			if (accountsToEmail.size() == 0) {
				getLogger().warn("Did not find any accounts to email for interaction ID {}", interaction.getInteractionId());
				continue;
			}

			Message message = new EmailMessage.Builder(Enum.valueOf(EmailMessageTemplate.class, interaction.getMessageTemplate()), institution.getLocale())
					.toAddresses(accountsToEmail)
					.fromAddress(getConfiguration().getEmailDefaultFromAddress())
					.messageContext(new HashMap<String, Object>() {{
						// Pull out a magic "endUserHtmlRepresentation" key from metadata if possible and expose it to the message template.
						// Use HIPAA-compliant metadata here because this message is going out via email, which should not contain PII
						String metadata = trimToNull(interactionInstance.getHipaaCompliantMetadata());
						String endUserHtmlRepresentation = null;

						if (metadata != null) {
							try {
								Map<String, Object> metadataAsMap = getJsonMapper().fromJson(metadata, Map.class);
								endUserHtmlRepresentation = trimToNull((String) metadataAsMap.get("endUserHtmlRepresentation"));
							} catch (Exception ignored) {
								// Don't worry if this fails, it's just best-effort to get data out
							}
						}

						if (endUserHtmlRepresentation == null)
							endUserHtmlRepresentation = metadata == null ? getStrings().get("[none]", institution.getLocale()) : metadata;

						put("caseNumber", interactionInstance.getCaseNumber());
						put("metadata", metadata);
						put("endUserHtmlRepresentation", endUserHtmlRepresentation);
						put("interactionInstanceUrl", format("%s/interaction-instances/%s",
								getConfiguration().getWebappBaseUrl(institution.getInstitutionId()), interactionInstanceId));
					}})
					.build();

			Map<String, Object> metadata = new HashMap<>() {{
				put("interactionInstanceId", interactionInstanceId);
			}};

			getMessageService().createScheduledMessage(new CreateScheduledMessageRequest<>() {{
				setMessage(message);
				setScheduledAt(finalScheduledAt);
				setTimeZone(timeZone);
				setMetadata(metadata);
			}});
		}
	}

	@Nonnull
	public String formatInteractionOptionResponseMessage(@Nonnull InteractionInstance interactionInstance,
																											 @Nonnull String message) {
		requireNonNull(interactionInstance);
		requireNonNull(message);

		Interaction interaction = findInteractionById(interactionInstance.getInteractionId()).get();
		Institution institution = getInstitutionService().findInstitutionById(interaction.getInstitutionId()).get();
		Locale locale = institution.getLocale();

		Long completionDurationInSeconds = interactionInstance.getCompletedFlag() ? ChronoUnit.SECONDS.between(
				interactionInstance.getStartDateTime(), interactionInstance.getCompletedDate().atZone(interactionInstance.getTimeZone())) : null;

		String completionTimeHoursAndMinutes = interactionInstance.getCompletedFlag() ? getFormatter().formatDuration(completionDurationInSeconds, locale) : getStrings().get("[not completed]", locale);

		return message.replace("[maxInteractionCount]", interaction.getMaxInteractionCount().toString())
				.replace("[frequencyHoursAndMinutes]", getFormatter().formatDuration(interaction.getFrequencyInMinutes() * 60))
				.replace("[completionTimeHoursAndMinutes]", completionTimeHoursAndMinutes);
	}

	@Nonnull
	private Integer findOptionActionCount(@Nonnull UUID interactionInstanceId) {
		requireNonNull(interactionInstanceId);

		return getDatabase().queryForObject("SELECT COUNT(*) FROM interaction_option_action WHERE interaction_instance_id = ?", Integer.class, interactionInstanceId).get();
	}

	@Nonnull
	public UUID createInteractionOptionAction(@Nonnull CreateInteractionOptionActionRequest request) {
		requireNonNull(request);

		UUID interactionInstanceId = request.getInteractionInstanceId();
		UUID interactionOptionId = request.getInteractionOptionId();
		UUID accountId = request.getAccountId();
		ValidationException validationException = new ValidationException();
		InteractionInstance interactionInstance = null;
		Interaction interaction = null;
		InteractionOption interactionOption = null;

		if (interactionInstanceId == null) {
			validationException.add(new FieldError("interactionInstanceId", getStrings().get("Interaction instance ID is required.")));
		} else {
			interactionInstance = findInteractionInstanceById(interactionInstanceId).orElse(null);

			if (interactionInstance == null) {
				validationException.add(new FieldError("interactionInstanceId", getStrings().get("Interaction instance was not found.")));
			} else {
				//If this interaction instance is complete and a new interaction option action is being created thrown an exception
				if (interactionInstance.getCompletedFlag())
					validationException.add(new FieldError("interactionInstanceId", getStrings().get(interaction.getInteractionCompleteMessage())));

				interaction = findInteractionById(interactionInstance.getInteractionId()).orElse(null);

				if (interaction == null)
					validationException.add(new FieldError("interactionInstanceId", getStrings().get("No interaction was found for the given interaction instance.")));
			}
		}

		if (interactionOptionId == null) {
			validationException.add(new FieldError("interactionOptionId", getStrings().get("Interaction option ID is required.")));
		} else {
			interactionOption = findInteractionOptionById(interactionOptionId).orElse(null);

			if (interactionOption == null) {
				validationException.add(new FieldError("interactionOptionId", getStrings().get("Interaction option was not found.")));
			} else {
				if (interaction != null && !interaction.getInteractionId().equals(interactionOption.getInteractionId()))
					validationException.add(new FieldError("interactionOptionId", getStrings().get("Interaction option is not valid for the specified interaction instance.")));
			}
		}

		if (validationException.hasErrors())
			throw validationException;

		UUID interactionOptionActionId = UUID.randomUUID();

		getDatabase().execute("INSERT INTO interaction_option_action (interaction_option_action_id, interaction_instance_id, interaction_option_id, account_id) " +
				"VALUES (?,?,?,?)", interactionOptionActionId, interactionInstanceId, interactionOptionId, accountId);

		Integer optionActionCount = findOptionActionCount(interactionInstanceId);

		if (interactionOption.getFinalFlag() || optionActionCount >= interaction.getMaxInteractionCount())
			markInteractionInstanceComplete(interactionInstanceId);
		else {
			cancelPendingMessagesForInteractionInstance(interactionInstanceId);
			createInteractionInstanceMessages(interactionInstanceId, LocalDateTime.now(interactionInstance.getTimeZone())
					.plus(interaction.getFrequencyInMinutes(), ChronoUnit.MINUTES), interactionInstance.getTimeZone());
		}

		return interactionOptionActionId;
	}

	@Nonnull
	public Optional<InteractionOptionAction> findInteractionOptionActionById(@Nullable UUID interactionOptionActionId) {
		if (interactionOptionActionId == null)
			return Optional.empty();

		return getDatabase().queryForObject("SELECT * FROM interaction_option_action WHERE interaction_option_action_id=?",
				InteractionOptionAction.class, interactionOptionActionId);
	}

	@Nonnull
	public List<InteractionOptionAction> findInteractionOptionActionsByInteractionInstanceId(@Nullable UUID interactionInstanceId) {
		if (interactionInstanceId == null)
			return Collections.emptyList();

		return getDatabase().queryForList("SELECT * FROM interaction_option_action WHERE interaction_instance_id=? " +
				"ORDER BY created DESC", InteractionOptionAction.class, interactionInstanceId);
	}

	@Nonnull
	protected Database getDatabase() {
		return database;
	}

	@Nonnull
	protected AccountService getAccountService() {
		return accountService;
	}

	@Nonnull
	protected Strings getStrings() {
		return strings;
	}

	@Nonnull
	protected MessageService getMessageService() {
		return messageService;
	}

	@Nonnull
	protected InstitutionService getInstitutionService() {
		return institutionService;
	}

	@Nonnull
	protected Configuration getConfiguration() {
		return configuration;
	}

	@Nonnull
	protected Logger getLogger() {
		return logger;
	}

	@Nonnull
	protected Formatter getFormatter() {
		return formatter;
	}

	@Nonnull
	protected JsonMapper getJsonMapper() {
		return jsonMapper;
	}
}
