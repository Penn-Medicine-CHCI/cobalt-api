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
import com.cobaltplatform.api.model.api.request.CreateInteractionInstance;
import com.cobaltplatform.api.model.api.request.CreateScheduledMessageRequest;
import com.cobaltplatform.api.model.api.response.InteractionOptionApiResponse.InteractionOptionApiResponseFactory;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.Interaction;
import com.cobaltplatform.api.model.db.InteractionInstance;
import com.cobaltplatform.api.model.db.InteractionOption;
import com.cobaltplatform.api.model.db.ScheduledMessage;
import com.cobaltplatform.api.model.db.ScheduledMessageStatus;
import com.cobaltplatform.api.util.Formatter;
import com.cobaltplatform.api.util.ValidationException;
import com.lokalized.Strings;
import com.pyranid.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

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
	private final InteractionOptionApiResponseFactory interactionOptionApiResponseFactory;
	@Nonnull
	private final Formatter formatter;
	@Nonnull
	private final Configuration configuration;

	@Inject
	public InteractionService(@Nonnull Database database,
														@Nonnull AccountService accountService,
														@Nonnull Strings strings,
														@Nonnull MessageService messageService,
														@Nonnull InteractionOptionApiResponseFactory interactionOptionApiResponseFactory,
														@Nonnull Formatter formatter,
														@Nonnull Configuration configuration) {
		requireNonNull(database);
		requireNonNull(accountService);
		requireNonNull(strings);
		requireNonNull(messageService);
		requireNonNull(interactionOptionApiResponseFactory);
		requireNonNull(formatter);
		requireNonNull(configuration);

		this.logger = LoggerFactory.getLogger(getClass());
		this.database = database;
		this.accountService = accountService;
		this.strings = strings;
		this.messageService = messageService;
		this.interactionOptionApiResponseFactory = interactionOptionApiResponseFactory;
		this.formatter = formatter;
		this.configuration = configuration;
	}

	@Nonnull
	public InteractionInstance findRequiredInteractionInstanceById(@Nonnull UUID interactionInstanceId) {
		requireNonNull(interactionInstanceId);

		return getDatabase().queryForObject("SELECT * FROM interaction_instance WHERE interaction_instance_id = ?",
				InteractionInstance.class, interactionInstanceId).get();
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

		return getDatabase().queryForList("SELECT * FROM interaction_option WHERE interaction_id = ?", InteractionOption.class, interactionId);
	}

	@Nonnull
	public InteractionOption findRequiredInteractionOptionsById(@Nonnull UUID interactionOptionId) {
		requireNonNull(interactionOptionId);

		return getDatabase().queryForObject("SELECT * FROM interaction_option WHERE interaction_option_id = ?", InteractionOption.class, interactionOptionId).get();
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
	public UUID createInteractionInstance(@Nonnull CreateInteractionInstance request) {
		requireNonNull(request);

		ValidationException validationException = new ValidationException();
		UUID interactionInstanceId = UUID.randomUUID();
		Account account = null;
		UUID accountId = request.getAccountId();
		UUID interactionId = request.getInteractionId();
		LocalDateTime startDateTime = request.getStartDateTime();
		String metaData = request.getmetadata();
		Interaction interaction = null;

		if (accountId == null) {
			validationException.add(new ValidationException.FieldError("accountId", getStrings().get("Account ID is required.")));
		} else {
			account = getAccountService().findAccountById(accountId).orElse(null);

			if (account == null)
				validationException.add(new ValidationException.FieldError("accountId", getStrings().get("Account ID is invalid.")));
		}

		if (interactionId == null) {
			validationException.add(new ValidationException.FieldError("interactionId", getStrings().get("Interaction ID is required.")));
		} else {
			interaction = findInteractionById(interactionId).orElse(null);

			if (interaction == null)
				validationException.add(new ValidationException.FieldError("interactionId", getStrings().get("Interaction ID is invalid.")));
		}

		if (startDateTime == null)
			validationException.add(new ValidationException.FieldError("startDateTime", getStrings().get("Start date and time is required.")));

		if (validationException.hasErrors())
			throw validationException;

		getDatabase().execute("INSERT INTO interaction_instance (interaction_instance_id, interaction_id, account_id, start_date_time, "
						+ "time_zone, metadata) VALUES (?,?,?,?,?,CAST (? AS JSONB))", interactionInstanceId, interactionId, accountId,
				startDateTime, account.getTimeZone(), metaData);

		createInteractionInstanceMessages(interactionInstanceId, startDateTime);

		return interactionInstanceId;
	}

	@Nonnull
	private void createInteractionInstanceMessages (@Nonnull UUID interactionInstanceId,
																									@Nonnull LocalDateTime startDateTime) {
		requireNonNull(interactionInstanceId);
		requireNonNull(startDateTime);

		InteractionInstance interactionInstance = findRequiredInteractionInstanceById(interactionInstanceId);
		Interaction interaction = findInteractionById(interactionInstance.getInteractionId()).get();
		Account account = getAccountService().findAccountById(interactionInstance.getAccountId()).get();

		Integer frequencyInMinutes = interaction.getFrequencyInMinutes();
		ZoneId timeZone = account.getTimeZone();
		LocalDateTime scheduledAt = startDateTime;
		Integer optionActionCount = findOptionActionCount(interactionInstanceId);

		if (optionActionCount > 0)
			scheduledAt = scheduledAt.plus(frequencyInMinutes, ChronoUnit.MINUTES).atZone(timeZone)
					.toLocalDateTime();

		for (int i = optionActionCount; i < interaction.getMaxInteractionCount(); i++) {

			LocalDateTime finalScheduledAt = scheduledAt;
			List<String> accountsToEmail = getAccountService().findAccountsMatchingMetadata(new HashMap<>() {{
				put("interactionId", interactionInstance.getInteractionId());
			}}).stream().map( e -> e.getEmailAddress()).filter(e -> e != null).collect(Collectors.toList());

			Message message = new EmailMessage.Builder(EmailMessageTemplate.INTERACTION_REMINDER, Locale.US)
					.toAddresses(accountsToEmail)
					.fromAddress(getConfiguration().getEmailDefaultFromAddress())
					.messageContext(new HashMap<String, Object>() {{
						put("interactionInstanceId", interactionInstanceId);
						put("metaData", interactionInstance.getmetadata());
						put("interactionOptions", findInteractionOptionsByInteractionId(interaction.getInteractionId()).stream().map((interactionOption) ->
								getInteractionOptionApiResponseFactory().create(interactionOption, interactionInstance)).collect(Collectors.toList()));
					}})
					.build();

			Map<String, Object> metadata = new HashMap<>() {{
				put("interactionInstanceId", interactionInstanceId);
			}};

			UUID scheduledMessageId = messageService.createScheduledMessage(new CreateScheduledMessageRequest<>() {{
				setMessage(message);
				setScheduledAt(finalScheduledAt);
				setTimeZone(timeZone);
				setMetadata(metadata);
			}});

			ScheduledMessage scheduledMessage = messageService.findScheduledMessageById(scheduledMessageId).get();

			scheduledAt = scheduledAt
					.plus(frequencyInMinutes, ChronoUnit.MINUTES)
					.atZone(timeZone)
					.toLocalDateTime();

		}
	}

	@Nonnull
	public String formatInteractionMessage(@Nonnull InteractionInstance interactionInstance,
																				 @Nonnull String message) {
		requireNonNull(interactionInstance);
		requireNonNull(message);

		Interaction interaction = findInteractionById(interactionInstance.getInteractionId()).get();


		return message.replace("[maxInteractionCount]", interaction.getMaxInteractionCount().toString())
				.replace("[frequencyHoursAndMinutes]", formatter.formatDuration(interaction.getFrequencyInMinutes() * 60))
				.replace("[completionTimeHoursAndMinutes]", interactionInstance.getCompletedFlag() ? formatter.formatDuration(ChronoUnit.SECONDS.between(interactionInstance.getCompletedDate(),
						interactionInstance.getStartDateTime())) : "[not completed]");
	}

	@Nonnull
	private Integer findOptionActionCount(@Nonnull UUID interactionInstanceId) {
		requireNonNull(interactionInstanceId);

		return  getDatabase().queryForObject("SELECT COUNT(*) FROM interaction_option_action WHERE interaction_instance_id = ?", Integer.class, interactionInstanceId).get();
	}

	@Nonnull
	public UUID createInteractionOptionAction(@Nonnull UUID accountId, @Nonnull UUID interactionInstanceId, @Nonnull UUID interactionOptionId) {
		requireNonNull(accountId);
		requireNonNull(interactionInstanceId);
		requireNonNull(interactionOptionId);

		ValidationException validationException = new ValidationException();
		InteractionInstance interactionInstance = findRequiredInteractionInstanceById(interactionInstanceId);
		Optional<Interaction> interaction = findInteractionById(interactionInstance.getInteractionId());
		Account account = getAccountService().findAccountById(accountId).get();

		//If this interaction instance is complete and a new interaction option action is being created thrown an exception
		if (interactionInstance.getCompletedFlag())
			validationException.add(new ValidationException.FieldError("interactionInstance", getStrings().get(interaction.get().getInteractionCompleteMessage())));

		if (validationException.hasErrors())
			throw validationException;

		UUID interactionOptionActionId = UUID.randomUUID();

		getDatabase().execute("INSERT INTO interaction_option_action (interaction_option_action_id, interaction_instance_id, interaction_option_id, account_id) " +
				"VALUES (?,?,?,?)", interactionOptionActionId, interactionInstanceId, interactionOptionId, accountId);

		InteractionOption interactionOption = findRequiredInteractionOptionsById(interactionOptionId);
		Integer optionActionCount = findOptionActionCount(interactionInstanceId);

		if (interactionOption.getFinalFlag() || optionActionCount >= interaction.get().getMaxInteractionCount())
			markInteractionInstanceComplete(interactionInstanceId);
		else {
			cancelPendingMessagesForInteractionInstance(interactionInstanceId);
			createInteractionInstanceMessages(interactionInstanceId, Instant.now().atZone(account.getTimeZone()).toLocalDateTime());
		}

		return interactionOptionActionId;
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
	protected InteractionOptionApiResponseFactory getInteractionOptionApiResponseFactory() {
		return interactionOptionApiResponseFactory;
	}

	@Nonnull
	protected Configuration getConfiguration() {return  configuration; }
}
