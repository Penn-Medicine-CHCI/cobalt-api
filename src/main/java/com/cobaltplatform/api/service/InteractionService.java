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
import com.cobaltplatform.api.error.ErrorReporter;
import com.cobaltplatform.api.messaging.Message;
import com.cobaltplatform.api.messaging.email.EmailMessage;
import com.cobaltplatform.api.messaging.email.EmailMessageTemplate;
import com.cobaltplatform.api.model.api.request.CreateInteractionInstanceRequest;
import com.cobaltplatform.api.model.api.request.CreateInteractionOptionActionRequest;
import com.cobaltplatform.api.model.api.request.CreateScheduledMessageRequest;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.Institution;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.Interaction;
import com.cobaltplatform.api.model.db.InteractionInstance;
import com.cobaltplatform.api.model.db.InteractionOption;
import com.cobaltplatform.api.model.db.InteractionOptionAction;
import com.cobaltplatform.api.model.db.InteractionSendMethod.InteractionSendMethodId;
import com.cobaltplatform.api.model.db.PatientOrder;
import com.cobaltplatform.api.model.db.ScheduledMessage;
import com.cobaltplatform.api.model.db.ScheduledMessageStatus;
import com.cobaltplatform.api.model.db.ScreeningAnswer;
import com.cobaltplatform.api.model.db.ScreeningAnswerFormat.ScreeningAnswerFormatId;
import com.cobaltplatform.api.model.db.ScreeningAnswerOption;
import com.cobaltplatform.api.model.db.ScreeningQuestion;
import com.cobaltplatform.api.model.db.ScreeningSession;
import com.cobaltplatform.api.model.db.ScreeningSessionScreening;
import com.cobaltplatform.api.model.db.ScreeningType;
import com.cobaltplatform.api.model.db.ScreeningVersion;
import com.cobaltplatform.api.model.db.UserExperienceType.UserExperienceTypeId;
import com.cobaltplatform.api.model.service.ScreeningQuestionWithAnswerOptions;
import com.cobaltplatform.api.util.Formatter;
import com.cobaltplatform.api.util.JsonMapper;
import com.cobaltplatform.api.util.Normalizer;
import com.cobaltplatform.api.util.ValidationException;
import com.cobaltplatform.api.util.ValidationException.FieldError;
import com.cobaltplatform.api.util.db.DatabaseProvider;
import com.lokalized.Strings;
import com.pyranid.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
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
import static org.apache.commons.text.StringEscapeUtils.escapeHtml4;

/**
 * @author Transmogrify LLC.
 */
@Singleton
public class InteractionService {
	@Nonnull
	private final Strings strings;
	@Nonnull
	private final DatabaseProvider databaseProvider;
	@Nonnull
	private final Logger logger;
	@Nonnull
	private final javax.inject.Provider<AccountService> accountServiceProvider;
	@Nonnull
	private final javax.inject.Provider<MessageService> messageServiceProvider;
	@Nonnull
	private final javax.inject.Provider<InstitutionService> institutionServiceProvider;
	@Nonnull
	private final javax.inject.Provider<ScreeningService> screeningServiceProvider;
	@Nonnull
	private final javax.inject.Provider<PatientOrderService> patientOrderServiceProvider;
	@Nonnull
	private final Formatter formatter;
	@Nonnull
	private final ErrorReporter errorReporter;
	@Nonnull
	private final Configuration configuration;
	@Nonnull
	private final JsonMapper jsonMapper;

	@Inject
	public InteractionService(@Nonnull DatabaseProvider databaseProvider,
														@Nonnull javax.inject.Provider<AccountService> accountServiceProvider,
														@Nonnull Strings strings,
														@Nonnull javax.inject.Provider<MessageService> messageServiceProvider,
														@Nonnull javax.inject.Provider<InstitutionService> institutionServiceProvider,
														@Nonnull javax.inject.Provider<ScreeningService> screeningServiceProvider,
														@Nonnull javax.inject.Provider<PatientOrderService> patientOrderServiceProvider,
														@Nonnull Formatter formatter,
														@Nonnull ErrorReporter errorReporter,
														@Nonnull Configuration configuration,
														@Nonnull JsonMapper jsonMapper) {
		requireNonNull(databaseProvider);
		requireNonNull(accountServiceProvider);
		requireNonNull(strings);
		requireNonNull(messageServiceProvider);
		requireNonNull(institutionServiceProvider);
		requireNonNull(screeningServiceProvider);
		requireNonNull(patientOrderServiceProvider);
		requireNonNull(formatter);
		requireNonNull(errorReporter);
		requireNonNull(configuration);
		requireNonNull(jsonMapper);

		this.logger = LoggerFactory.getLogger(getClass());
		this.databaseProvider = databaseProvider;
		this.accountServiceProvider = accountServiceProvider;
		this.strings = strings;
		this.messageServiceProvider = messageServiceProvider;
		this.institutionServiceProvider = institutionServiceProvider;
		this.screeningServiceProvider = screeningServiceProvider;
		this.patientOrderServiceProvider = patientOrderServiceProvider;
		this.formatter = formatter;
		this.errorReporter = errorReporter;
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

		List<ScheduledMessage> scheduledMessages = getMessageService().findScheduledMessagesMatchingMetadata(new HashMap<>() {{
			put("interactionInstanceId", interactionInstanceId);
		}});

		for (ScheduledMessage scheduledMessage : scheduledMessages) {
			if (scheduledMessage.getScheduledMessageStatusId().equals(ScheduledMessageStatus.ScheduledMessageStatusId.PENDING))
				getMessageService().cancelScheduledMessage(scheduledMessage.getScheduledMessageId());
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

		if (request.getAdditionalAccountsToNotify() != null && request.getAdditionalAccountsToNotify().size() > 0)
			for (UUID accountIdToUpdate : request.getAdditionalAccountsToNotify())
				addInteractionInstanceIdToAccount(interactionInstanceId, accountIdToUpdate);

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

		// Determine the initial send time for this interaction
		if (interaction.getInteractionSendMethodId().equals(InteractionSendMethodId.MINUTE_OFFSET))
			startDateTime = startDateTime.plusMinutes(interaction.getSendOffsetInMinutes());
		else {
			LocalDate interactionDate = startDateTime.toLocalDate().plusDays(interaction.getSendDayOffset());
			LocalTime interactionTimeOfDay = interaction.getSendTimeOfDay();
			startDateTime = LocalDateTime.of(interactionDate, interactionTimeOfDay);
		}

		LocalDateTime scheduledAt = startDateTime;
		Integer optionActionCount = findOptionActionCount(interactionInstanceId);

		for (int i = optionActionCount; i < interaction.getMaxInteractionCount(); i++) {
			if (i > optionActionCount) {
				// Determine the send time for the next follow up
				if (interaction.getInteractionSendMethodId().equals(InteractionSendMethodId.MINUTE_OFFSET))
					scheduledAt = scheduledAt.plus(interaction.getFrequencyInMinutes(), ChronoUnit.MINUTES);
				else {
					LocalDate nextInteractionDate = scheduledAt.toLocalDate().plusDays(interaction.getSendDayOffset());
					LocalTime nextInteractionTimeOfDay = interaction.getSendTimeOfDay();
					scheduledAt = LocalDateTime.of(nextInteractionDate, nextInteractionTimeOfDay);
				}
			}

			LocalDateTime finalScheduledAt = scheduledAt;

			Account.StandardMetadata standardMetadata = new Account.StandardMetadata();
			standardMetadata.setInteractionIds(Set.of(interaction.getInteractionId()));

			List<String> accountsToEmail = getAccountService().findAccountsMatchingMetadata(standardMetadata).stream()
					.map(e -> e.getEmailAddress()).filter(e -> e != null)
					.collect(Collectors.toList());

			standardMetadata = new Account.StandardMetadata();
			standardMetadata.setInteractionInstanceIds(Set.of(interactionInstanceId));

			accountsToEmail.addAll(getAccountService().findAccountsMatchingMetadata(standardMetadata).stream()
					.map(e -> e.getEmailAddress()).filter(e -> e != null)
					.collect(Collectors.toList()));

			if (accountsToEmail.size() == 0) {
				getLogger().warn("Did not find any accounts to email for interaction ID {}", interaction.getInteractionId());
				continue;
			}

			Message message = new EmailMessage.Builder(institution.getInstitutionId(), Enum.valueOf(EmailMessageTemplate.class, interaction.getMessageTemplate()), institution.getLocale())
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
								getInstitutionService().findWebappBaseUrlByInstitutionIdAndUserExperienceTypeId(institution.getInstitutionId(), UserExperienceTypeId.STAFF).get(), interactionInstanceId));
						put("messageTemplateBodyHtml", interaction.getMessageTemplateBody());
						put("subject", interaction.getEmailSubject());
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
				interaction = findInteractionById(interactionInstance.getInteractionId()).orElse(null);

				if (interaction == null) {
					validationException.add(new FieldError("interactionInstanceId", getStrings().get("No interaction was found for the given interaction instance.")));
				} else {
					// If this interaction instance is complete and a new interaction option action is being created, throw an exception
					if (interactionInstance.getCompletedFlag())
						validationException.add(getStrings().get(interaction.getInteractionCompleteMessage()));
				}
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
	private void addInteractionInstanceIdToAccount(@Nonnull UUID interactionInstanceId,
																								 @Nonnull UUID accountId) {
		requireNonNull(interactionInstanceId);
		requireNonNull(accountId);

		Boolean accountHasInteractionInstances = getDatabase().queryForObject("SELECT COUNT(*) > 0 FROM account WHERE metadata ?? 'interactionInstanceIds' AND account_id=? ",
				Boolean.class, accountId).get();

		if (accountHasInteractionInstances)
			getDatabase().execute(format("UPDATE account SET metadata = jsonb_set(" +
					"  metadata::jsonb, array['interactionInstanceIds'], " +
					"  (metadata->'interactionInstanceIds')::jsonb || '[\"%s\"]'::jsonb) " +
					"WHERE account_id = ?", interactionInstanceId), accountId);
		else
			getDatabase().execute(format("UPDATE account SET metadata = coalesce(metadata,'{}')::jsonb || '{\"interactionInstanceIds\": [\"%s\"]}'::JSONB WHERE account_id = ?", interactionInstanceId), accountId);

	}

	@Nonnull
	public UUID linkInteractionInstanceToAppointment(@Nonnull UUID interactionInstanceId,
																									 @Nonnull UUID appointmentId) {
		requireNonNull(interactionInstanceId);
		requireNonNull(appointmentId);

		UUID appointmentInteractionInstanceId = UUID.randomUUID();

		getDatabase().execute("INSERT INTO appointment_interaction_instance (appointment_interaction_instance_id, appointment_id, interaction_instance_id) " +
				"VALUES (?,?,?)", appointmentInteractionInstanceId, appointmentId, interactionInstanceId);

		return appointmentInteractionInstanceId;
	}

	@Nonnull
	public void cancelInteractionInstancesForAppointment(@Nonnull UUID appointmentId) {
		requireNonNull(appointmentId);

		List<UUID> interactionInstanceIds = getDatabase().queryForList("SELECT interaction_instance_id FROM appointment_interaction_instance " +
				"WHERE appointment_id = ?", UUID.class, appointmentId);

		for (UUID interactionInstanceId : interactionInstanceIds)
			cancelPendingMessagesForInteractionInstance(interactionInstanceId);

	}

	public void createCrisisInteraction(@Nonnull UUID screeningSessionId) {
		requireNonNull(screeningSessionId);

		ScreeningSession screeningSession = getScreeningService().findScreeningSessionById(screeningSessionId).get();
		CrisisDataProvider crisisDataProvider;

		if (screeningSession.getPatientOrderId() != null) {
			PatientOrder patientOrder = getPatientOrderService().findPatientOrderById(screeningSession.getPatientOrderId()).get();
			crisisDataProvider = new PatientOrderCrisisDataProvider(patientOrder);
		} else {
			Account targetAccount = getAccountService().findAccountById(screeningSession.getTargetAccountId()).get();
			crisisDataProvider = new AccountCrisisDataProvider(targetAccount);
		}

		// Find the crisis interaction ID for this screening's institution
		Institution institution = getInstitutionService().findInstitutionById(crisisDataProvider.getInstitutionId()).get();
		UUID defaultCrisisInteractionId = institution.getStandardMetadata().getDefaultCrisisInteractionId();

		if (defaultCrisisInteractionId == null) {
			getLogger().warn("Not creating crisis interaction because no default crisis interaction ID is available for institution {}", institution.getInstitutionId());
			return;
		}

		// Gather information to put into the interaction
		ZoneId timeZone = institution.getTimeZone();
		LocalDateTime now = LocalDateTime.now(timeZone);
		Locale locale = institution.getLocale();

		List<ScreeningSessionScreening> screeningSessionScreenings = getScreeningService().findCurrentScreeningSessionScreeningsByScreeningSessionId(screeningSessionId);
		List<ScreeningSessionScreeningResult> screeningSessionScreeningResults = new ArrayList<>(screeningSessionScreenings.size());

		// We could do this as a single query, but the dataset is small, and this is a little clearer and fast enough
		for (ScreeningSessionScreening screeningSessionScreening : screeningSessionScreenings) {
			ScreeningVersion screeningVersion = getScreeningService().findScreeningVersionById(screeningSessionScreening.getScreeningVersionId()).get();
			List<ScreeningQuestionWithAnswerOptions> screeningQuestionsWithAnswerOptions = getScreeningService().findScreeningQuestionsWithAnswerOptionsByScreeningSessionScreeningId(screeningSessionScreening.getScreeningSessionScreeningId());
			List<ScreeningAnswer> screeningAnswers = getScreeningService().findScreeningAnswersAcrossAllQuestionsByScreeningSessionScreeningId(screeningSessionScreening.getScreeningSessionScreeningId());
			List<ScreeningQuestionAndAnswers> screeningQuestionsAndAnswers = new ArrayList<>();

			Map<UUID, ScreeningAnswer> screeningAnswersByAnswerOptionId = new HashMap<>(screeningAnswers.size());

			for (ScreeningAnswer screeningAnswer : screeningAnswers)
				screeningAnswersByAnswerOptionId.put(screeningAnswer.getScreeningAnswerOptionId(), screeningAnswer);

			for (ScreeningQuestionWithAnswerOptions screeningQuestionsWithAnswerOption : screeningQuestionsWithAnswerOptions) {
				List<ScreeningAnswerOption> screeningAnswerOptions = screeningQuestionsWithAnswerOption.getScreeningAnswerOptions();
				List<ScreeningAnswerOption> answeredScreeningAnswerOptions = new ArrayList<>(screeningAnswerOptions.size());
				List<ScreeningAnswer> currentScreeningAnswers = new ArrayList<>(screeningAnswerOptions.size());

				for (ScreeningAnswerOption screeningAnswerOption : screeningAnswerOptions) {
					ScreeningAnswer screeningAnswer = screeningAnswersByAnswerOptionId.get(screeningAnswerOption.getScreeningAnswerOptionId());

					if (screeningAnswer != null) {
						currentScreeningAnswers.add(screeningAnswer);
						answeredScreeningAnswerOptions.add(screeningAnswerOption);
					}
				}

				ScreeningQuestionAndAnswers screeningQuestionAndAnswers = new ScreeningQuestionAndAnswers();
				screeningQuestionAndAnswers.setScreeningQuestion(screeningQuestionsWithAnswerOption.getScreeningQuestion());
				screeningQuestionAndAnswers.setAnsweredScreeningAnswerOptions(answeredScreeningAnswerOptions);
				screeningQuestionAndAnswers.setScreeningAnswers(currentScreeningAnswers);
				screeningQuestionsAndAnswers.add(screeningQuestionAndAnswers);
			}

			ScreeningSessionScreeningResult screeningSessionScreeningResult = new ScreeningSessionScreeningResult();
			screeningSessionScreeningResult.setScreeningSessionScreening(screeningSessionScreening);
			screeningSessionScreeningResult.setScreeningQuestionsAndAnswers(screeningQuestionsAndAnswers);
			screeningSessionScreeningResult.setScreeningVersion(screeningVersion);
			screeningSessionScreeningResults.add(screeningSessionScreeningResult);
		}

		Map<String, Object> metadata = createCrisisInteractionMetadata(screeningSessionScreeningResults, crisisDataProvider, locale);
		Map<String, Object> hipaaCompliantMetadata = createCrisisInteractionHipaaCompliantMetadata(screeningSessionScreeningResults, crisisDataProvider, locale);

		// Record an interaction for this incident, which might send off some email messages (for example)
		createInteractionInstance(new CreateInteractionInstanceRequest() {{
			setMetadata(metadata);
			setHipaaCompliantMetadata(hipaaCompliantMetadata);
			setStartDateTime(now);
			setTimeZone(timeZone);
			setInteractionId(defaultCrisisInteractionId);
		}});
	}

	@Nonnull
	protected Map<String, Object> createCrisisInteractionHipaaCompliantMetadata(@Nonnull List<ScreeningSessionScreeningResult> screeningSessionScreeningResults,
																																							@Nonnull CrisisDataProvider crisisDataProvider,
																																							@Nonnull Locale locale) {
		requireNonNull(screeningSessionScreeningResults);
		requireNonNull(crisisDataProvider);
		requireNonNull(locale);

		List<String> htmlListItems = new ArrayList<>(2);

		if (crisisDataProvider.getFirstName() != null)
			htmlListItems.add(createHtmlListItem(getStrings().get("First Name", locale), crisisDataProvider.getFirstName()));

		if (crisisDataProvider.getPhoneNumber() != null)
			htmlListItems.add(createHtmlListItem(getStrings().get("Phone Number", locale), format("<a href='tel:%s'>%s</a>", crisisDataProvider.getPhoneNumber(), getFormatter().formatPhoneNumber(crisisDataProvider.getPhoneNumber(), locale)), false));
		else if (crisisDataProvider.getEmailAddress() != null)
			htmlListItems.add(createHtmlListItem(getStrings().get("Email Address", locale), format("<a href='mailto:%s'>%s</a>", crisisDataProvider.getEmailAddress(), crisisDataProvider.getEmailAddress()), false));
		else
			htmlListItems.add(createHtmlListItem(getStrings().get("Contact Information", locale), getStrings().get("None Available", locale)));

		String endUserHtmlRepresentation = format("<ul>%s</ul>", htmlListItems.stream().collect(Collectors.joining("")));

		return new HashMap<String, Object>() {{
			if (crisisDataProvider.getFirstName() != null)
				put("firstName", crisisDataProvider.getFirstName());

			if (crisisDataProvider.getPhoneNumber() != null) {
				put("phoneNumber", crisisDataProvider.getPhoneNumber());
				put("phoneNumberForDisplay", getFormatter().formatPhoneNumber(crisisDataProvider.getPhoneNumber(), locale));
			} else if (crisisDataProvider.getEmailAddress() != null) {
				put("emailAddress", crisisDataProvider.getEmailAddress());
			}

			put("endUserHtmlRepresentation", endUserHtmlRepresentation);
		}};
	}

	@Nonnull
	protected Map<String, Object> createCrisisInteractionMetadata(@Nonnull List<ScreeningSessionScreeningResult> screeningSessionScreeningResults,
																																@Nonnull CrisisDataProvider crisisDataProvider,
																																@Nonnull Locale locale) {
		requireNonNull(screeningSessionScreeningResults);
		requireNonNull(crisisDataProvider);
		requireNonNull(locale);

		List<String> htmlListItems = new ArrayList<>();

		if (crisisDataProvider.getDisplayName() != null)
			htmlListItems.add(createHtmlListItem(getStrings().get("Name", locale), crisisDataProvider.getDisplayName()));
		if (crisisDataProvider.getPhoneNumber() != null)
			htmlListItems.add(createHtmlListItem(getStrings().get("Phone Number", locale), format("<a href='tel:%s'>%s</a>", crisisDataProvider.getPhoneNumber(), getFormatter().formatPhoneNumber(crisisDataProvider.getPhoneNumber(), locale)), false));
		if (crisisDataProvider.getEmailAddress() != null)
			htmlListItems.add(createHtmlListItem(getStrings().get("Email Address", locale), format("<a href='mailto:%s'>%s</a>", crisisDataProvider.getEmailAddress(), crisisDataProvider.getEmailAddress()), false));

		for (ScreeningSessionScreeningResult screeningSessionScreeningResult : screeningSessionScreeningResults) {
			ScreeningVersion screeningVersion = screeningSessionScreeningResult.getScreeningVersion();
			ScreeningType screeningType = getScreeningService().findScreeningTypeById(screeningVersion.getScreeningTypeId()).get();

			htmlListItems.add("<li>");
			htmlListItems.add(escapeHtml4(screeningType.getDescription()));
			htmlListItems.add("<ul>");

			for (ScreeningQuestionAndAnswers screeningQuestionAndAnswers : screeningSessionScreeningResult.getScreeningQuestionsAndAnswers()) {
				ScreeningQuestion screeningQuestion = screeningQuestionAndAnswers.getScreeningQuestion();
				List<String> answers = new ArrayList<>();

				for (int i = 0; i < screeningQuestionAndAnswers.getScreeningAnswers().size(); ++i) {
					ScreeningAnswer screeningAnswer = screeningQuestionAndAnswers.getScreeningAnswers().get(i);
					ScreeningAnswerOption screeningAnswerOption = screeningQuestionAndAnswers.getAnsweredScreeningAnswerOptions().get(i);

					if (screeningQuestion.getScreeningAnswerFormatId() == ScreeningAnswerFormatId.FREEFORM_TEXT)
						answers.add(escapeHtml4(screeningAnswer.getText()));
					else
						answers.add(escapeHtml4(screeningAnswerOption.getAnswerOptionText()));
				}

				htmlListItems.add(createHtmlListItem(getStrings().get(screeningQuestion.getQuestionText(), locale),
						answers.stream().collect(Collectors.joining(", "))));
			}

			htmlListItems.add("</ul>");
			htmlListItems.add("</li>");
		}

		String endUserHtmlRepresentation = format("<ul>%s</ul>", htmlListItems.stream().collect(Collectors.joining("")));

		return new HashMap<String, Object>() {{
			if (crisisDataProvider.getDisplayName() != null)
				put("name", crisisDataProvider.getDisplayName());

			if (crisisDataProvider.getPhoneNumber() != null) {
				put("phoneNumber", crisisDataProvider.getPhoneNumber());
				put("phoneNumberForDisplay", getFormatter().formatPhoneNumber(crisisDataProvider.getPhoneNumber(), locale));
			}

			if (crisisDataProvider.getEmailAddress() != null)
				put("emailAddress", crisisDataProvider.getEmailAddress());

			put("endUserHtmlRepresentation", endUserHtmlRepresentation);
		}};
	}

	@Nonnull
	protected String createHtmlListItem(@Nonnull String fieldName,
																			@Nullable String fieldValue) {
		requireNonNull(fieldName);
		return createHtmlListItem(fieldName, fieldValue, true);
	}

	@Nonnull
	protected String createHtmlListItem(@Nonnull String fieldName,
																			@Nullable String fieldValue,
																			@Nonnull Boolean escapeHtml) {
		requireNonNull(fieldName);
		requireNonNull(escapeHtml);

		if (trimToNull(fieldValue) == null)
			fieldValue = getStrings().get("[unspecified]");

		return format("<li><strong>%s</strong> %s</li>", escapeHtml ? escapeHtml4(fieldName) : fieldName,
				escapeHtml ? escapeHtml4(fieldValue) : fieldValue);
	}

	protected interface CrisisDataProvider {
		@Nonnull
		InstitutionId getInstitutionId();

		@Nullable
		String getEmailAddress();

		@Nullable
		String getPhoneNumber();

		@Nullable
		String getDisplayName();

		@Nullable
		String getFirstName();
	}

	@ThreadSafe
	protected static class AccountCrisisDataProvider implements CrisisDataProvider {
		@Nonnull
		private final Account account;

		public AccountCrisisDataProvider(@Nonnull Account account) {
			requireNonNull(account);
			this.account = account;
		}

		@Nonnull
		@Override
		public InstitutionId getInstitutionId() {
			return getAccount().getInstitutionId();
		}

		@Nullable
		@Override
		public String getEmailAddress() {
			return getAccount().getEmailAddress();
		}

		@Nullable
		@Override
		public String getPhoneNumber() {
			return getAccount().getPhoneNumber();
		}

		@Nullable
		@Override
		public String getDisplayName() {
			return getAccount().getDisplayName();
		}

		@Nullable
		@Override
		public String getFirstName() {
			return getAccount().getFirstName();
		}

		@Nonnull
		protected Account getAccount() {
			return this.account;
		}
	}

	@ThreadSafe
	protected static class PatientOrderCrisisDataProvider implements CrisisDataProvider {
		@Nonnull
		private final PatientOrder patientOrder;

		public PatientOrderCrisisDataProvider(@Nonnull PatientOrder patientOrder) {
			requireNonNull(patientOrder);
			this.patientOrder = patientOrder;
		}

		@Nonnull
		@Override
		public InstitutionId getInstitutionId() {
			return getPatientOrder().getInstitutionId();
		}

		@Nullable
		@Override
		public String getEmailAddress() {
			return getPatientOrder().getPatientEmailAddress();
		}

		@Nullable
		@Override
		public String getPhoneNumber() {
			return getPatientOrder().getPatientPhoneNumber();
		}

		@Nullable
		@Override
		public String getDisplayName() {
			return Normalizer.normalizeName(getPatientOrder().getPatientFirstName(), getPatientOrder().getPatientLastName()).orElse(null);
		}

		@Nullable
		@Override
		public String getFirstName() {
			return getPatientOrder().getPatientFirstName();
		}

		@Nonnull
		protected PatientOrder getPatientOrder() {
			return this.patientOrder;
		}
	}

	@NotThreadSafe
	protected static class ScreeningSessionScreeningResult {
		@Nullable
		private ScreeningSessionScreening screeningSessionScreening;
		@Nullable
		private ScreeningVersion screeningVersion;
		@Nullable
		private List<ScreeningQuestionAndAnswers> screeningQuestionsAndAnswers;

		@Nullable
		public ScreeningSessionScreening getScreeningSessionScreening() {
			return this.screeningSessionScreening;
		}

		public void setScreeningSessionScreening(@Nullable ScreeningSessionScreening screeningSessionScreening) {
			this.screeningSessionScreening = screeningSessionScreening;
		}

		@Nullable
		public ScreeningVersion getScreeningVersion() {
			return this.screeningVersion;
		}

		public void setScreeningVersion(@Nullable ScreeningVersion screeningVersion) {
			this.screeningVersion = screeningVersion;
		}

		@Nullable
		public List<ScreeningQuestionAndAnswers> getScreeningQuestionsAndAnswers() {
			return this.screeningQuestionsAndAnswers;
		}

		public void setScreeningQuestionsAndAnswers(@Nullable List<ScreeningQuestionAndAnswers> screeningQuestionsAndAnswers) {
			this.screeningQuestionsAndAnswers = screeningQuestionsAndAnswers;
		}
	}

	@NotThreadSafe
	protected static class ScreeningQuestionAndAnswers {
		@Nullable
		private ScreeningQuestion screeningQuestion;
		@Nullable
		private List<ScreeningAnswerOption> answeredScreeningAnswerOptions;
		@Nullable
		private List<ScreeningAnswer> screeningAnswers;

		@Nullable
		public ScreeningQuestion getScreeningQuestion() {
			return this.screeningQuestion;
		}

		public void setScreeningQuestion(@Nullable ScreeningQuestion screeningQuestion) {
			this.screeningQuestion = screeningQuestion;
		}

		@Nullable
		public List<ScreeningAnswerOption> getAnsweredScreeningAnswerOptions() {
			return this.answeredScreeningAnswerOptions;
		}

		public void setAnsweredScreeningAnswerOptions(@Nullable List<ScreeningAnswerOption> answeredScreeningAnswerOptions) {
			this.answeredScreeningAnswerOptions = answeredScreeningAnswerOptions;
		}

		@Nullable
		public List<ScreeningAnswer> getScreeningAnswers() {
			return this.screeningAnswers;
		}

		public void setScreeningAnswers(@Nullable List<ScreeningAnswer> screeningAnswers) {
			this.screeningAnswers = screeningAnswers;
		}
	}

	@Nonnull
	protected Database getDatabase() {
		return this.databaseProvider.get();
	}

	@Nonnull
	protected AccountService getAccountService() {
		return this.accountServiceProvider.get();
	}

	@Nonnull
	protected Strings getStrings() {
		return this.strings;
	}

	@Nonnull
	protected MessageService getMessageService() {
		return this.messageServiceProvider.get();
	}

	@Nonnull
	protected InstitutionService getInstitutionService() {
		return this.institutionServiceProvider.get();
	}

	@Nonnull
	protected ScreeningService getScreeningService() {
		return this.screeningServiceProvider.get();
	}

	@Nonnull
	protected PatientOrderService getPatientOrderService() {
		return this.patientOrderServiceProvider.get();
	}

	@Nonnull
	protected Configuration getConfiguration() {
		return this.configuration;
	}

	@Nonnull
	protected Logger getLogger() {
		return this.logger;
	}

	@Nonnull
	protected Formatter getFormatter() {
		return this.formatter;
	}

	@Nonnull
	protected ErrorReporter getErrorReporter() {
		return this.errorReporter;
	}

	@Nonnull
	protected JsonMapper getJsonMapper() {
		return this.jsonMapper;
	}
}
