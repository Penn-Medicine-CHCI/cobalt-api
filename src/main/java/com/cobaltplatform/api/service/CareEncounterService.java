/*
 * Copyright 2021 The University of Pennsylvania and Penn Medicine
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

import com.cobaltplatform.api.model.api.request.CancelAppointmentRequest;
import com.cobaltplatform.api.model.api.request.CancelCareEncounterAppointmentRequest;
import com.cobaltplatform.api.model.api.request.CancelCareEncounterRequest;
import com.cobaltplatform.api.model.api.request.ChangeAppointmentAttendanceStatusRequest;
import com.cobaltplatform.api.model.api.request.CreateCareEncounterNoteRequest;
import com.cobaltplatform.api.model.api.request.CreateCareEncounterRequest;
import com.cobaltplatform.api.model.api.request.CreateCareEncounterScheduledMessageRequest;
import com.cobaltplatform.api.model.api.request.CreateScheduledMessageRequest;
import com.cobaltplatform.api.model.api.request.FindCareEncountersRequest;
import com.cobaltplatform.api.model.api.request.FindCareEncountersRequest.CareEncounterAssignmentScopeId;
import com.cobaltplatform.api.model.api.request.FindCareEncountersRequest.CareEncounterSortColumnId;
import com.cobaltplatform.api.model.api.request.UpdateCareEncounterRequest;
import com.cobaltplatform.api.model.api.request.UpdateCareEncounterNoteRequest;
import com.cobaltplatform.api.model.api.request.PreviewCareEncounterScheduledMessageRequest;
import com.cobaltplatform.api.messaging.email.EmailMessage;
import com.cobaltplatform.api.messaging.email.EmailMessageTemplate;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.Appointment;
import com.cobaltplatform.api.model.db.AttendanceStatus;
import com.cobaltplatform.api.model.db.AttendanceStatus.AttendanceStatusId;
import com.cobaltplatform.api.model.db.CareEncounter;
import com.cobaltplatform.api.model.db.CareEncounterCancellationReason;
import com.cobaltplatform.api.model.db.CareEncounterCancellationReason.CareEncounterCancellationReasonId;
import com.cobaltplatform.api.model.db.CareEncounterNote;
import com.cobaltplatform.api.model.db.CareEncounterScheduledMessage;
import com.cobaltplatform.api.model.db.CareEncounterScheduledMessageType;
import com.cobaltplatform.api.model.db.CareEncounterScheduledMessageType.CareEncounterScheduledMessageTypeId;
import com.cobaltplatform.api.model.db.CareEncounterStatus.CareEncounterStatusId;
import com.cobaltplatform.api.model.db.Feature.FeatureId;
import com.cobaltplatform.api.model.db.Institution;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.ScheduledMessageSource.ScheduledMessageSourceId;
import com.cobaltplatform.api.model.db.ScheduledMessageStatus.ScheduledMessageStatusId;
import com.cobaltplatform.api.model.db.UserExperienceType.UserExperienceTypeId;
import com.cobaltplatform.api.model.service.FindResult;
import com.cobaltplatform.api.model.service.RenderedEmailMessage;
import com.cobaltplatform.api.model.service.SortDirectionId;
import com.cobaltplatform.api.util.Normalizer;
import com.cobaltplatform.api.util.Formatter;
import com.cobaltplatform.api.util.HandlebarsTemplater;
import com.cobaltplatform.api.util.ValidationException;
import com.cobaltplatform.api.util.ValidationException.FieldError;
import com.cobaltplatform.api.util.db.DatabaseProvider;
import com.lokalized.Strings;
import com.pyranid.Database;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.cobaltplatform.api.util.DatabaseUtility.sqlVaragsParameters;
import static com.cobaltplatform.api.util.ValidationUtility.isValidEmailAddress;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * Administrative operations for Care Navigator appointments.
 *
 * @author Transmogrify, LLC.
 */
@Singleton
@ThreadSafe
public class CareEncounterService {
	protected static final int DEFAULT_PAGE_SIZE = 25;
	protected static final int MAXIMUM_PAGE_SIZE = 100;
	protected static final int MAXIMUM_NOTE_LENGTH = 20_000;
	protected static final int MAXIMUM_CUSTOM_EMAIL_TEXT_LENGTH = 20_000;
	protected static final int MAXIMUM_CANCELLATION_REASON_OTHER_TEXT_LENGTH = 2_000;
	protected static final int MAXIMUM_APPOINTMENT_CANCELLATION_REASON_LENGTH = 2_000;

	@Nonnull
	private final DatabaseProvider databaseProvider;
	@Nonnull
	private final AppointmentService appointmentService;
	@Nonnull
	private final Normalizer normalizer;
	@Nonnull
	private final Strings strings;
	@Nonnull
	private final MessageService messageService;
	@Nonnull
	private final Provider<InstitutionService> institutionServiceProvider;
	@Nonnull
	private final Provider<AccountService> accountServiceProvider;
	@Nonnull
	private final Formatter formatter;
	@Nonnull
	private final HandlebarsTemplater emailHandlebarsTemplater;
	@Nonnull
	private final PolicyFactory customEmailTextSanitizingPolicy;

	@Inject
	public CareEncounterService(@Nonnull DatabaseProvider databaseProvider,
								 @Nonnull AppointmentService appointmentService,
								 @Nonnull Normalizer normalizer,
								 @Nonnull Strings strings,
								 @Nonnull MessageService messageService,
								 @Nonnull Provider<InstitutionService> institutionServiceProvider,
								 @Nonnull Provider<AccountService> accountServiceProvider,
								 @Nonnull Formatter formatter,
								 @Nonnull HandlebarsTemplater emailHandlebarsTemplater) {
		requireNonNull(databaseProvider);
		requireNonNull(appointmentService);
		requireNonNull(normalizer);
		requireNonNull(strings);
		requireNonNull(messageService);
		requireNonNull(institutionServiceProvider);
		requireNonNull(accountServiceProvider);
		requireNonNull(formatter);
		requireNonNull(emailHandlebarsTemplater);

		this.databaseProvider = databaseProvider;
		this.appointmentService = appointmentService;
		this.normalizer = normalizer;
		this.strings = strings;
		this.messageService = messageService;
		this.institutionServiceProvider = institutionServiceProvider;
		this.accountServiceProvider = accountServiceProvider;
		this.formatter = formatter;
		this.emailHandlebarsTemplater = emailHandlebarsTemplater;
		this.customEmailTextSanitizingPolicy = new HtmlPolicyBuilder()
				.allowElements("p", "strong", "b", "br", "em", "i", "u", "ol", "li", "ul", "a")
				.allowUrlProtocols("https")
				.allowAttributes("href", "rel", "target").onElements("a")
				.requireRelNofollowOnLinks()
				.toFactory();
	}

	@Nonnull
	public FindResult<CareEncounter> findCareEncounters(@Nonnull FindCareEncountersRequest request) {
		requireNonNull(request);

		InstitutionId institutionId = request.getInstitutionId();
		Integer pageNumber = request.getPageNumber();
		Integer pageSize = request.getPageSize();
		LocalDate startDate = request.getStartDate();
		LocalDate endDate = request.getEndDate();
		String searchQuery = trimToNull(request.getSearchQuery());
		CareEncounterStatusId careEncounterStatusId = request.getCareEncounterStatusId();
		CareEncounterAssignmentScopeId careEncounterAssignmentScopeId = request.getCareEncounterAssignmentScopeId();
		UUID careNavigatorAccountId = request.getCareNavigatorAccountId();
		CareEncounterSortColumnId careEncounterSortColumnId = request.getCareEncounterSortColumnId();
		SortDirectionId sortDirectionId = request.getSortDirectionId();

		if (careEncounterSortColumnId == null || sortDirectionId == null) {
			careEncounterSortColumnId = CareEncounterSortColumnId.APPOINTMENT_DATE;
			sortDirectionId = SortDirectionId.DESCENDING;
		}
		ValidationException validationException = new ValidationException();

		if (institutionId == null)
			validationException.add(new FieldError("institutionId", getStrings().get("Institution ID is required.")));

		if (careEncounterAssignmentScopeId == CareEncounterAssignmentScopeId.SELF && careNavigatorAccountId == null)
			validationException.add(new FieldError("careNavigatorAccountId",
					getStrings().get("Care Navigator Account ID is required for SELF assignment scope.")));

		if (startDate != null && endDate != null && startDate.isAfter(endDate))
			validationException.add(new FieldError("date", getStrings().get("Start date must be on or before end date.")));

		if (validationException.hasErrors())
			throw validationException;

		if (pageNumber == null || pageNumber < 0)
			pageNumber = 0;

		if (pageSize == null || pageSize <= 0)
			pageSize = DEFAULT_PAGE_SIZE;
		else if (pageSize > MAXIMUM_PAGE_SIZE)
			pageSize = MAXIMUM_PAGE_SIZE;

		StringBuilder query = new StringBuilder("""
				SELECT care_encounter.*, COUNT(*) OVER() AS total_count
				FROM care_encounter
				JOIN LATERAL (
					SELECT appointment.*
					FROM appointment
					WHERE appointment.care_encounter_id=care_encounter.care_encounter_id
					ORDER BY
						CASE WHEN appointment.canceled=FALSE
							AND appointment.canceled_for_reschedule=FALSE
							AND appointment.attendance_status_id='UNKNOWN' THEN 0 ELSE 1 END,
						appointment.start_time DESC,
						appointment.appointment_id
					LIMIT 1
				) appointment ON TRUE
				JOIN provider ON provider.provider_id=appointment.provider_id
				WHERE care_encounter.deleted=FALSE
				AND provider.institution_id=?
				""");
		List<Object> parameters = new ArrayList<>();
		parameters.add(institutionId);

		if (startDate != null) {
			query.append("AND appointment.start_time >= ? ");
			parameters.add(startDate.atStartOfDay());
		}

		if (endDate != null) {
			query.append("AND appointment.start_time <= ? ");
			parameters.add(endDate.atTime(LocalTime.MAX));
		}

		if (searchQuery != null) {
			query.append("""
					AND (
						EXISTS (
							SELECT 1
							FROM appointment search_appointment
							WHERE search_appointment.care_encounter_id=care_encounter.care_encounter_id
							AND (
								CONCAT_WS(' ', search_appointment.first_name, search_appointment.last_name) ILIKE ?
								OR CONCAT_WS(' ', search_appointment.last_name, search_appointment.first_name) ILIKE ?
								OR search_appointment.email_address ILIKE ?
								OR search_appointment.contact_phone_number ILIKE ?
								OR search_appointment.title ILIKE ?
							)
						)
						OR EXISTS (
							SELECT 1
							FROM care_encounter_note
							WHERE care_encounter_note.care_encounter_id=care_encounter.care_encounter_id
							AND care_encounter_note.deleted=FALSE
							AND care_encounter_note.note ILIKE ?
						)
					)
					""");
			String searchPattern = String.format("%%%s%%", searchQuery);
			parameters.add(searchPattern);
			parameters.add(searchPattern);
			parameters.add(searchPattern);
			parameters.add(searchPattern);
			parameters.add(searchPattern);
			parameters.add(searchPattern);
		}

		if (careEncounterStatusId != null) {
			if (careEncounterStatusId == CareEncounterStatusId.CLOSED) {
				query.append("AND care_encounter.care_encounter_status_id<>'OPEN' ");
			} else {
				query.append("AND care_encounter.care_encounter_status_id=? ");
				parameters.add(careEncounterStatusId);
			}
		}

		if (careEncounterAssignmentScopeId == CareEncounterAssignmentScopeId.SELF) {
			query.append("AND care_encounter.care_navigator_account_id=? ");
			parameters.add(careNavigatorAccountId);
		} else if (careEncounterAssignmentScopeId == CareEncounterAssignmentScopeId.UNASSIGNED) {
			query.append("AND care_encounter.care_navigator_account_id IS NULL ");
		}

		String sortDirection = sortDirectionId == SortDirectionId.ASCENDING ? "ASC" : "DESC";
		query.append("ORDER BY ");

		if (careEncounterSortColumnId == CareEncounterSortColumnId.APPOINTMENT_DATE)
			query.append("appointment.start_time ").append(sortDirection).append(" ");
		else if (careEncounterSortColumnId == CareEncounterSortColumnId.PATIENT_NAME)
			query.append("LOWER(appointment.last_name) ").append(sortDirection)
					.append(", LOWER(appointment.first_name) ").append(sortDirection).append(" ");
		else if (careEncounterSortColumnId == CareEncounterSortColumnId.STATUS)
			query.append("CASE WHEN care_encounter.care_encounter_status_id='OPEN' THEN 1 ELSE 0 END ")
					.append(sortDirection).append(", care_encounter.care_encounter_status_id ").append(sortDirection).append(" ");
		else if (careEncounterSortColumnId == CareEncounterSortColumnId.CREATED)
			query.append("care_encounter.created ").append(sortDirection).append(" ");
		else if (careEncounterSortColumnId == CareEncounterSortColumnId.LAST_UPDATED)
			query.append("care_encounter.last_updated ").append(sortDirection).append(" ");

		query.append(", care_encounter.care_encounter_id ASC ");
		query.append("LIMIT ? OFFSET ?");
		parameters.add(pageSize);
		parameters.add(pageNumber * pageSize);

		List<CareEncounter> careEncounters = getDatabase().queryForList(query.toString(), CareEncounter.class,
				sqlVaragsParameters(parameters));
		Integer totalCount = careEncounters.isEmpty() || careEncounters.get(0).getTotalCount() == null
				? 0
				: careEncounters.get(0).getTotalCount();

		return new FindResult<>(careEncounters, totalCount);
	}

	@Nonnull
	public Optional<CareEncounter> findCareEncounterByIdForInstitutionId(@Nullable UUID careEncounterId,
																							@Nullable InstitutionId institutionId) {
		if (careEncounterId == null || institutionId == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				SELECT care_encounter.*
				FROM care_encounter
				JOIN LATERAL (
					SELECT appointment.*
					FROM appointment
					WHERE appointment.care_encounter_id=care_encounter.care_encounter_id
					ORDER BY appointment.start_time DESC, appointment.appointment_id
					LIMIT 1
				) appointment ON TRUE
				JOIN provider ON provider.provider_id=appointment.provider_id
				WHERE care_encounter.care_encounter_id=?
				AND care_encounter.deleted=FALSE
				AND provider.institution_id=?
				""", CareEncounter.class, careEncounterId, institutionId);
	}

	@Nonnull
	public List<CareEncounter> findCareEncountersByAccountId(@Nullable UUID accountId,
																 @Nullable InstitutionId institutionId) {
		if (accountId == null || institutionId == null)
			return List.of();

		return getDatabase().queryForList("""
				SELECT care_encounter.*
				FROM care_encounter
				JOIN LATERAL (
					SELECT appointment.*
					FROM appointment
					WHERE appointment.care_encounter_id=care_encounter.care_encounter_id
					ORDER BY appointment.start_time DESC, appointment.appointment_id
					LIMIT 1
				) appointment ON TRUE
				JOIN provider ON provider.provider_id=appointment.provider_id
				WHERE care_encounter.account_id=?
				AND care_encounter.deleted=FALSE
				AND provider.institution_id=?
				ORDER BY appointment.start_time DESC, care_encounter.care_encounter_id ASC
				""", CareEncounter.class, accountId, institutionId);
	}

	@Nonnull
	public List<CareEncounterNote> findCareEncounterNotesByCareEncounterId(@Nullable UUID careEncounterId) {
		if (careEncounterId == null)
			return List.of();

		return getDatabase().queryForList("""
				SELECT care_encounter_note.*,
					COALESCE(NULLIF(BTRIM(created_by_account.display_name), ''),
						NULLIF(BTRIM(CONCAT_WS(' ', created_by_account.first_name, created_by_account.last_name)), ''))
						AS created_by_account_display_name,
					COALESCE(NULLIF(BTRIM(last_updated_by_account.display_name), ''),
						NULLIF(BTRIM(CONCAT_WS(' ', last_updated_by_account.first_name, last_updated_by_account.last_name)), ''))
						AS last_updated_by_account_display_name
				FROM care_encounter_note
				JOIN account created_by_account
					ON created_by_account.account_id=care_encounter_note.created_by_account_id
				JOIN account last_updated_by_account
					ON last_updated_by_account.account_id=care_encounter_note.last_updated_by_account_id
				WHERE care_encounter_note.care_encounter_id=?
				AND care_encounter_note.deleted=FALSE
				ORDER BY care_encounter_note.created DESC, care_encounter_note.care_encounter_note_id DESC
				""", CareEncounterNote.class, careEncounterId);
	}

	@Nonnull
	public Optional<CareEncounterNote> findCareEncounterNoteByIdAndCareEncounterId(
			@Nullable UUID careEncounterNoteId,
			@Nullable UUID careEncounterId) {
		if (careEncounterNoteId == null || careEncounterId == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				SELECT care_encounter_note.*,
					COALESCE(NULLIF(BTRIM(created_by_account.display_name), ''),
						NULLIF(BTRIM(CONCAT_WS(' ', created_by_account.first_name, created_by_account.last_name)), ''))
						AS created_by_account_display_name,
					COALESCE(NULLIF(BTRIM(last_updated_by_account.display_name), ''),
						NULLIF(BTRIM(CONCAT_WS(' ', last_updated_by_account.first_name, last_updated_by_account.last_name)), ''))
						AS last_updated_by_account_display_name
				FROM care_encounter_note
				JOIN account created_by_account
					ON created_by_account.account_id=care_encounter_note.created_by_account_id
				JOIN account last_updated_by_account
					ON last_updated_by_account.account_id=care_encounter_note.last_updated_by_account_id
				WHERE care_encounter_note.care_encounter_note_id=?
				AND care_encounter_note.care_encounter_id=?
				AND care_encounter_note.deleted=FALSE
				""", CareEncounterNote.class, careEncounterNoteId, careEncounterId);
	}

	@Nonnull
	public CareEncounterNote createCareEncounterNote(@Nullable UUID careEncounterId,
																		@Nullable InstitutionId institutionId,
																		@Nullable UUID accountId,
																		@Nonnull CreateCareEncounterNoteRequest request) {
		requireNonNull(request);

		String note = normalizeNote(request.getNote());
		ValidationException validationException = new ValidationException();

		validateCareEncounterForNote(careEncounterId, institutionId, accountId, validationException);
		validateNote(note, validationException);

		if (validationException.hasErrors())
			throw validationException;

		UUID careEncounterNoteId = UUID.randomUUID();
		getDatabase().execute("""
				INSERT INTO care_encounter_note (
					care_encounter_note_id,
					care_encounter_id,
					note,
					created_by_account_id,
					last_updated_by_account_id
				) VALUES (?,?,?,?,?)
				""", careEncounterNoteId, careEncounterId, note, accountId, accountId);

		touchCareEncounter(careEncounterId, accountId);
		return findCareEncounterNoteByIdAndCareEncounterId(careEncounterNoteId, careEncounterId).get();
	}

	public boolean deleteCareEncounterNote(@Nullable UUID careEncounterId,
															@Nullable UUID careEncounterNoteId,
															@Nullable InstitutionId institutionId,
															@Nullable UUID accountId) {
		ValidationException validationException = new ValidationException();
		validateCareEncounterForNote(careEncounterId, institutionId, accountId, validationException);

		if (careEncounterNoteId == null) {
			validationException.add(new FieldError("careEncounterNoteId",
					getStrings().get("Care Encounter Note ID is required.")));
		} else if (careEncounterId != null
				&& findCareEncounterNoteByIdAndCareEncounterId(careEncounterNoteId, careEncounterId).isEmpty()) {
			validationException.add(new FieldError("careEncounterNoteId",
					getStrings().get("Care Encounter Note ID is invalid.")));
		}

		if (validationException.hasErrors())
			throw validationException;

		boolean deleted = getDatabase().execute("""
				UPDATE care_encounter_note
				SET deleted=TRUE, deleted_at=NOW(), deleted_by_account_id=?, last_updated_by_account_id=?
				WHERE care_encounter_note_id=? AND care_encounter_id=? AND deleted=FALSE
				""", accountId, accountId, careEncounterNoteId, careEncounterId) > 0;
		if (deleted)
			touchCareEncounter(careEncounterId, accountId);
		return deleted;
	}

	@Nonnull
	public List<CareEncounterScheduledMessageType> findCareEncounterScheduledMessageTypes() {
		return getDatabase().queryForList("""
				SELECT *
				FROM care_encounter_scheduled_message_type
				ORDER BY display_order, description
				""", CareEncounterScheduledMessageType.class);
	}

	@Nonnull
	public List<CareEncounterScheduledMessage> findCareEncounterScheduledMessagesByCareEncounterId(
			@Nullable UUID careEncounterId) {
		if (careEncounterId == null)
			return List.of();

		return getDatabase().queryForList(careEncounterScheduledMessageSelectSql() + """
				WHERE cesm.care_encounter_id=?
				ORDER BY cesm.created DESC, cesm.care_encounter_scheduled_message_id DESC
				""", CareEncounterScheduledMessage.class, careEncounterId);
	}

	@Nonnull
	public Optional<CareEncounterScheduledMessage> findCareEncounterScheduledMessageById(
			@Nullable UUID careEncounterId,
			@Nullable UUID careEncounterScheduledMessageId) {
		if (careEncounterId == null || careEncounterScheduledMessageId == null)
			return Optional.empty();

		return getDatabase().queryForObject(careEncounterScheduledMessageSelectSql() + """
				WHERE cesm.care_encounter_id=?
				AND cesm.care_encounter_scheduled_message_id=?
				""", CareEncounterScheduledMessage.class, careEncounterId, careEncounterScheduledMessageId);
	}

	@Nonnull
	public RenderedEmailMessage previewCareEncounterScheduledMessage(@Nullable UUID careEncounterId,
																					 @Nullable InstitutionId institutionId,
																					 @Nonnull PreviewCareEncounterScheduledMessageRequest request) {
		requireNonNull(request);
		ValidationException validationException = new ValidationException();
		CareEncounter careEncounter = validateOpenCareEncounter(careEncounterId, institutionId, null,
				validationException, false);
		String customEmailText = validateAndSanitizeCustomEmailText(request.getCustomEmailText(), validationException);
		validateScheduledMessageType(request.getCareEncounterScheduledMessageTypeId(), validationException);
		validateRecipientEmailAddress(careEncounter, validationException);
		if (validationException.hasErrors())
			throw validationException;

		return renderCareEncounterFollowUp(careEncounter, institutionId, customEmailText, UUID.randomUUID(), null).renderedEmailMessage;
	}

	@Nonnull
	public CareEncounterScheduledMessage createCareEncounterScheduledMessage(@Nullable UUID careEncounterId,
																						 @Nullable InstitutionId institutionId,
																						 @Nullable UUID accountId,
																						 @Nonnull CreateCareEncounterScheduledMessageRequest request) {
		requireNonNull(request);
		ValidationException validationException = new ValidationException();
		CareEncounter careEncounter = validateOpenCareEncounter(careEncounterId, institutionId, accountId,
				validationException, true);
		String customEmailText = validateAndSanitizeCustomEmailText(request.getCustomEmailText(), validationException);
		validateScheduledMessageType(request.getCareEncounterScheduledMessageTypeId(), validationException);
		LocalDateTime scheduledAt = validateScheduledAt(request, validationException);
		validateRecipientEmailAddress(careEncounter, validationException);
		validateAttendedAppointmentForFollowUp(careEncounter, institutionId,
				request.getCareEncounterScheduledMessageTypeId(), validationException);
		if (careEncounter != null && hasPendingScheduledMessage(careEncounter.getCareEncounterId(),
				request.getCareEncounterScheduledMessageTypeId()))
			validationException.add(new FieldError("careEncounterScheduledMessageTypeId",
					getStrings().get("This Care Encounter already has a pending follow-up message.")));
		if (validationException.hasErrors())
			throw validationException;

		UUID messageId = UUID.randomUUID();
		UUID linkRevisionId = UUID.randomUUID();
		FollowUpEmailSnapshot snapshot = renderCareEncounterFollowUp(careEncounter, institutionId,
				customEmailText, messageId, linkRevisionId);
		Map<String, Object> metadata = scheduledMessageMetadata(careEncounterId,
				request.getCareEncounterScheduledMessageTypeId());
		CreateScheduledMessageRequest<EmailMessage> scheduledMessageRequest = new CreateScheduledMessageRequest<>();
		scheduledMessageRequest.setMessage(snapshot.freeformEmailMessage);
		scheduledMessageRequest.setScheduledAt(scheduledAt);
		scheduledMessageRequest.setTimeZone(snapshot.timeZone);
		scheduledMessageRequest.setMetadata(metadata);
		scheduledMessageRequest.setScheduledMessageSourceId(ScheduledMessageSourceId.MANUAL);
		scheduledMessageRequest.setScheduledByAccountId(accountId);
		UUID scheduledMessageId = getMessageService().createScheduledMessage(scheduledMessageRequest);
		UUID careEncounterScheduledMessageId = UUID.randomUUID();

		getDatabase().execute("""
				INSERT INTO care_encounter_scheduled_message (
					care_encounter_scheduled_message_id, care_encounter_id,
					care_encounter_scheduled_message_type_id, scheduled_message_id,
					recipient_email_address, custom_email_text, email_subject, email_body,
					created_by_account_id, last_updated_by_account_id
				) VALUES (?,?,?,?,?,?,?,?,?,?)
				""", careEncounterScheduledMessageId, careEncounterId,
				request.getCareEncounterScheduledMessageTypeId(), scheduledMessageId,
				careEncounter.getEmailAddress(), customEmailText,
				snapshot.renderedEmailMessage.getEmailSubject(), snapshot.renderedEmailMessage.getEmailBody(),
				accountId, accountId);
		touchCareEncounter(careEncounterId, accountId);
		return findCareEncounterScheduledMessageById(careEncounterId, careEncounterScheduledMessageId).get();
	}

	@Nonnull
	public CareEncounterScheduledMessage updateCareEncounterScheduledMessage(@Nullable UUID careEncounterId,
																						 @Nullable UUID careEncounterScheduledMessageId,
																						 @Nullable InstitutionId institutionId,
																						 @Nullable UUID accountId,
																						 @Nonnull CreateCareEncounterScheduledMessageRequest request) {
		requireNonNull(request);
		ValidationException validationException = new ValidationException();
		CareEncounter careEncounter = validateOpenCareEncounter(careEncounterId, institutionId, accountId,
				validationException, true);
		CareEncounterScheduledMessage existing = findCareEncounterScheduledMessageById(
				careEncounterId, careEncounterScheduledMessageId).orElse(null);
		if (existing == null)
			validationException.add(new FieldError("careEncounterScheduledMessageId",
					getStrings().get("Care Encounter Scheduled Message ID is invalid.")));
		else if (Boolean.TRUE.equals(existing.getDeleted())
				|| existing.getScheduledMessageStatusId() != ScheduledMessageStatusId.PENDING)
			validationException.add(new FieldError("careEncounterScheduledMessageId",
					getStrings().get("Only pending scheduled messages can be edited.")));
		String customEmailText = validateAndSanitizeCustomEmailText(request.getCustomEmailText(), validationException);
		validateScheduledMessageType(request.getCareEncounterScheduledMessageTypeId(), validationException);
		LocalDateTime scheduledAt = validateScheduledAt(request, validationException);
		validateRecipientEmailAddress(careEncounter, validationException);
		validateAttendedAppointmentForFollowUp(careEncounter, institutionId,
				request.getCareEncounterScheduledMessageTypeId(), validationException);
		if (validationException.hasErrors())
			throw validationException;

		UUID linkRevisionId = UUID.randomUUID();
		FollowUpEmailSnapshot snapshot = renderCareEncounterFollowUp(careEncounter, institutionId,
				customEmailText, existing.getMessageId(), linkRevisionId);
		boolean updated = getMessageService().updateScheduledMessage(existing.getScheduledMessageId(),
				snapshot.freeformEmailMessage, scheduledAt, snapshot.timeZone,
				scheduledMessageMetadata(careEncounterId, request.getCareEncounterScheduledMessageTypeId()));
		if (!updated) {
			getDatabase().execute("DELETE FROM care_encounter_follow_up_link WHERE message_id=? AND revision_id=?",
					existing.getMessageId(), linkRevisionId);
			throw pendingScheduledMessageValidationException("Only pending scheduled messages can be edited.");
		}
		getDatabase().execute("DELETE FROM care_encounter_follow_up_link WHERE message_id=? AND revision_id<>?",
				existing.getMessageId(), linkRevisionId);

		getDatabase().execute("""
				UPDATE care_encounter_scheduled_message
				SET care_encounter_scheduled_message_type_id=?, recipient_email_address=?,
					custom_email_text=?, email_subject=?, email_body=?, last_updated_by_account_id=?
				WHERE care_encounter_scheduled_message_id=? AND care_encounter_id=? AND deleted=FALSE
				""", request.getCareEncounterScheduledMessageTypeId(), careEncounter.getEmailAddress(), customEmailText,
				snapshot.renderedEmailMessage.getEmailSubject(), snapshot.renderedEmailMessage.getEmailBody(), accountId,
				careEncounterScheduledMessageId, careEncounterId);
		touchCareEncounter(careEncounterId, accountId);
		return findCareEncounterScheduledMessageById(careEncounterId, careEncounterScheduledMessageId).get();
	}

	@Nonnull
	public CareEncounterScheduledMessage deleteCareEncounterScheduledMessage(@Nullable UUID careEncounterId,
																						 @Nullable UUID careEncounterScheduledMessageId,
																						 @Nullable InstitutionId institutionId,
																						 @Nullable UUID accountId) {
		ValidationException validationException = new ValidationException();
		validateOpenCareEncounter(careEncounterId, institutionId, accountId, validationException, true);
		CareEncounterScheduledMessage existing = findCareEncounterScheduledMessageById(
				careEncounterId, careEncounterScheduledMessageId).orElse(null);
		if (existing == null)
			validationException.add(new FieldError("careEncounterScheduledMessageId",
					getStrings().get("Care Encounter Scheduled Message ID is invalid.")));
		else if (Boolean.TRUE.equals(existing.getDeleted())
				|| existing.getScheduledMessageStatusId() != ScheduledMessageStatusId.PENDING)
			validationException.add(new FieldError("careEncounterScheduledMessageId",
					getStrings().get("Only pending scheduled messages can be canceled.")));
		if (validationException.hasErrors())
			throw validationException;

		if (!getMessageService().cancelScheduledMessage(existing.getScheduledMessageId()))
			throw pendingScheduledMessageValidationException("Only pending scheduled messages can be canceled.");
		getDatabase().execute("""
				UPDATE care_encounter_scheduled_message
				SET deleted=TRUE, deleted_at=NOW(), deleted_by_account_id=?, last_updated_by_account_id=?
				WHERE care_encounter_scheduled_message_id=? AND care_encounter_id=? AND deleted=FALSE
				""", accountId, accountId, careEncounterScheduledMessageId, careEncounterId);
		touchCareEncounter(careEncounterId, accountId);
		return findCareEncounterScheduledMessageById(careEncounterId, careEncounterScheduledMessageId).get();
	}

	protected boolean hasPendingScheduledMessage(@Nullable UUID careEncounterId,
																		 @Nullable CareEncounterScheduledMessageTypeId typeId) {
		if (careEncounterId == null)
			return false;
		if (typeId == null)
			return getDatabase().queryForObject("""
					SELECT EXISTS (
						SELECT 1
						FROM care_encounter_scheduled_message cesm
						JOIN scheduled_message sm ON sm.scheduled_message_id=cesm.scheduled_message_id
						WHERE cesm.care_encounter_id=?
						AND sm.scheduled_message_status_id='PENDING'
					)
					""", Boolean.class, careEncounterId).orElse(false);
		return getDatabase().queryForObject("""
				SELECT EXISTS (
					SELECT 1
					FROM care_encounter_scheduled_message cesm
					JOIN scheduled_message sm ON sm.scheduled_message_id=cesm.scheduled_message_id
					WHERE cesm.care_encounter_id=?
					AND cesm.care_encounter_scheduled_message_type_id=?
					AND sm.scheduled_message_status_id='PENDING'
				)
				""", Boolean.class, careEncounterId, typeId).orElse(false);
	}

	@Nonnull
	public CareEncounterNote updateCareEncounterNote(@Nullable UUID careEncounterId,
																		@Nullable UUID careEncounterNoteId,
																		@Nullable InstitutionId institutionId,
																		@Nullable UUID accountId,
																		@Nonnull UpdateCareEncounterNoteRequest request) {
		requireNonNull(request);

		String note = normalizeNote(request.getNote());
		ValidationException validationException = new ValidationException();

		validateCareEncounterForNote(careEncounterId, institutionId, accountId, validationException);

		if (careEncounterNoteId == null) {
			validationException.add(new FieldError("careEncounterNoteId",
					getStrings().get("Care Encounter Note ID is required.")));
		} else if (careEncounterId != null
				&& findCareEncounterNoteByIdAndCareEncounterId(careEncounterNoteId, careEncounterId).isEmpty()) {
			validationException.add(new FieldError("careEncounterNoteId",
					getStrings().get("Care Encounter Note ID is invalid.")));
		}

		validateNote(note, validationException);

		if (validationException.hasErrors())
			throw validationException;

		getDatabase().execute("""
				UPDATE care_encounter_note
				SET note=?, last_updated_by_account_id=?
				WHERE care_encounter_note_id=?
				AND care_encounter_id=?
				AND deleted=FALSE
				""", note, accountId, careEncounterNoteId, careEncounterId);

		touchCareEncounter(careEncounterId, accountId);
		return findCareEncounterNoteByIdAndCareEncounterId(careEncounterNoteId, careEncounterId).get();
	}

	@Nonnull
	public CareEncounter createCareEncounter(@Nonnull CreateCareEncounterRequest request) {
		requireNonNull(request);

		UUID appointmentId = request.getAppointmentId();
		InstitutionId institutionId = request.getInstitutionId();
		UUID accountId = request.getAccountId();
		Appointment appointment = null;
		ValidationException validationException = new ValidationException();

		if (institutionId == null)
			validationException.add(new FieldError("institutionId", getStrings().get("Institution ID is required.")));

		if (accountId == null)
			validationException.add(new FieldError("accountId", getStrings().get("Account ID is required.")));

		if (appointmentId == null) {
			validationException.add(new FieldError("appointmentId", getStrings().get("Appointment ID is required.")));
		} else if (institutionId != null) {
			appointment = findCareNavigatorAppointmentByIdAndInstitutionId(appointmentId, institutionId).orElse(null);

			if (appointment == null)
				validationException.add(new FieldError("appointmentId", getStrings().get("Appointment ID is invalid.")));
		}

		if (validationException.hasErrors())
			throw validationException;

		// Normal booking/import paths attach automatically. Re-touching provider_id
		// makes this legacy administrative endpoint use the same database trigger.
		if (appointment.getCareEncounterId() == null)
			getDatabase().execute("""
					UPDATE appointment
					SET provider_id=provider_id
					WHERE appointment_id=?
					""", appointmentId);

		CareEncounter careEncounter = findCareEncounterByAppointmentIdForInstitutionId(appointmentId, institutionId)
				.orElseThrow(() -> new IllegalStateException("Care Navigator appointment was not attached to an encounter."));

		return careEncounter;
	}

	@Nonnull
	public CareEncounter updateCareEncounter(@Nonnull UpdateCareEncounterRequest request) {
		requireNonNull(request);

		UUID careEncounterId = request.getCareEncounterId();
		InstitutionId institutionId = request.getInstitutionId();
		UUID accountId = request.getAccountId();
		String emailAddress = getNormalizer().normalizeEmailAddress(request.getEmailAddress()).orElse(null);
		CareEncounter careEncounter = null;
		ValidationException validationException = new ValidationException();

		if (institutionId == null)
			validationException.add(new FieldError("institutionId", getStrings().get("Institution ID is required.")));

		if (accountId == null)
			validationException.add(new FieldError("accountId", getStrings().get("Account ID is required.")));

		if (careEncounterId == null) {
			validationException.add(new FieldError("careEncounterId", getStrings().get("Care Encounter ID is required.")));
		} else if (institutionId != null) {
			careEncounter = findCareEncounterByIdForInstitutionIdForUpdate(careEncounterId, institutionId).orElse(null);

			if (careEncounter == null)
				validationException.add(new FieldError("careEncounterId", getStrings().get("Care Encounter ID is invalid.")));
			else if (careEncounter.getCareEncounterStatusId() != CareEncounterStatusId.OPEN)
				validationException.add(new FieldError("careEncounterStatusId",
						getStrings().get("Only open Care Encounters can be updated.")));
		}

		if (emailAddress != null && !isValidEmailAddress(emailAddress))
			validationException.add(new FieldError("emailAddress", getStrings().get("Email address is invalid.")));

		if (validationException.hasErrors())
			throw validationException;

		getDatabase().execute("""
				UPDATE care_encounter
				SET email_address=?, last_updated_by_account_id=?
				WHERE care_encounter_id=?
				AND care_encounter_status_id='OPEN'
				""", emailAddress, accountId, careEncounterId);

		CareEncounter updatedCareEncounter = findCareEncounterByIdForInstitutionId(careEncounterId, institutionId).get();
		synchronizePendingScheduledMessagesForEmailChange(updatedCareEncounter, accountId);
		return updatedCareEncounter;
	}

	protected void synchronizePendingScheduledMessagesForEmailChange(
			@Nonnull CareEncounter careEncounter,
			@Nonnull UUID accountId) {
		List<CareEncounterScheduledMessage> pendingMessages = getDatabase().queryForList(
				careEncounterScheduledMessageSelectSql() + """
						WHERE cesm.care_encounter_id=?
						AND cesm.deleted=FALSE
						AND sm.scheduled_message_status_id='PENDING'
						ORDER BY cesm.created, cesm.care_encounter_scheduled_message_id
						FOR UPDATE OF cesm, sm
						""", CareEncounterScheduledMessage.class, careEncounter.getCareEncounterId());

		for (CareEncounterScheduledMessage pendingMessage : pendingMessages) {
			if (trimToNull(careEncounter.getEmailAddress()) == null) {
				if (!getMessageService().cancelScheduledMessage(pendingMessage.getScheduledMessageId()))
					throw pendingScheduledMessageValidationException(
							"The pending scheduled message could not be canceled after the email address was removed.");

				getDatabase().execute("""
						UPDATE care_encounter_scheduled_message
						SET last_updated_by_account_id=?
						WHERE care_encounter_scheduled_message_id=?
						AND care_encounter_id=?
						AND deleted=FALSE
						""", accountId, pendingMessage.getCareEncounterScheduledMessageId(),
						careEncounter.getCareEncounterId());
				continue;
			}

			boolean updated = getMessageService().updatePendingScheduledEmailRecipient(
					pendingMessage.getScheduledMessageId(), careEncounter.getEmailAddress());
			if (!updated)
				throw pendingScheduledMessageValidationException(
						"The pending scheduled message could not be updated with the corrected email address.");

			long updatedSnapshotCount = getDatabase().execute("""
					UPDATE care_encounter_scheduled_message
					SET recipient_email_address=?, last_updated_by_account_id=?
					WHERE care_encounter_scheduled_message_id=?
					AND care_encounter_id=?
					AND deleted=FALSE
					""", careEncounter.getEmailAddress(), accountId,
					pendingMessage.getCareEncounterScheduledMessageId(), careEncounter.getCareEncounterId());
			if (updatedSnapshotCount != 1)
				throw new IllegalStateException("The pending Care Encounter email snapshot could not be updated.");
		}
	}

	@Nonnull
	public CareEncounter closeCareEncounter(@Nullable UUID careEncounterId,
																			@Nullable InstitutionId institutionId,
																			@Nullable UUID accountId) {
		ValidationException validationException = new ValidationException();
		CareEncounter careEncounter = null;

		if (institutionId == null)
			validationException.add(new FieldError("institutionId", getStrings().get("Institution ID is required.")));

		if (accountId == null)
			validationException.add(new FieldError("accountId", getStrings().get("Account ID is required.")));

		if (careEncounterId == null) {
			validationException.add(new FieldError("careEncounterId", getStrings().get("Care Encounter ID is required.")));
		} else if (institutionId != null) {
			careEncounter = findCareEncounterByIdForInstitutionIdForUpdate(careEncounterId, institutionId).orElse(null);

			if (careEncounter == null)
				validationException.add(new FieldError("careEncounterId", getStrings().get("Care Encounter ID is invalid.")));
			else if (careEncounter.getCareEncounterStatusId() != CareEncounterStatusId.OPEN)
				validationException.add(new FieldError("careEncounterStatusId", getStrings().get("Only open Care Encounters can be closed.")));
			else if (findActiveAppointmentByCareEncounterIdForInstitutionId(careEncounterId, institutionId).isPresent())
				validationException.add(new FieldError("appointmentId", getStrings().get(
						"The active appointment must be completed or canceled before the Care Encounter can be closed.")));
			else if (hasPendingScheduledMessage(careEncounterId, null))
				validationException.add(new FieldError("careEncounterScheduledMessageId", getStrings().get(
						"Pending follow-up messages must be sent or canceled before the Care Encounter can be closed.")));
		}

		if (validationException.hasErrors())
			throw validationException;

		getDatabase().execute("""
				UPDATE care_encounter
				SET care_encounter_status_id='CLOSED',
					closed_at=NOW(),
					closed_by_account_id=?,
					last_updated_by_account_id=?
				WHERE care_encounter_id=?
				AND care_encounter_status_id='OPEN'
				""", accountId, accountId, careEncounterId);

		return findCareEncounterByIdForInstitutionId(careEncounterId, institutionId).get();
	}

	@Nonnull
	public CareEncounter assignCareEncounter(@Nullable UUID careEncounterId,
																			 @Nullable InstitutionId institutionId,
																			 @Nullable UUID updatedByAccountId,
																			 @Nullable UUID careNavigatorAccountId) {
		ValidationException validationException = new ValidationException();
		CareEncounter careEncounter = null;

		if (institutionId == null)
			validationException.add(new FieldError("institutionId", getStrings().get("Institution ID is required.")));

		if (updatedByAccountId == null)
			validationException.add(new FieldError("accountId", getStrings().get("Account ID is required.")));

		if (careNavigatorAccountId == null)
			validationException.add(new FieldError("careNavigatorAccountId", getStrings().get("Care Navigator account ID is required.")));

		if (careEncounterId == null) {
			validationException.add(new FieldError("careEncounterId", getStrings().get("Care Encounter ID is required.")));
		} else if (institutionId != null) {
			careEncounter = findCareEncounterByIdForInstitutionIdForUpdate(careEncounterId, institutionId).orElse(null);

			if (careEncounter == null)
				validationException.add(new FieldError("careEncounterId", getStrings().get("Care Encounter ID is invalid.")));
			else if (careEncounter.getCareEncounterStatusId() != CareEncounterStatusId.OPEN)
				validationException.add(new FieldError("careEncounterStatusId", getStrings().get("Only open Care Encounters can be assigned.")));
		}

		if (careEncounter != null && careNavigatorAccountId != null && institutionId != null
				&& !isEligibleCareNavigatorAssignment(careEncounterId, careNavigatorAccountId, institutionId))
			validationException.add(new FieldError("careNavigatorAccountId", getStrings().get(
					"Care Navigator account is not eligible for this encounter's current provider.")));

		if (validationException.hasErrors())
			throw validationException;

		getDatabase().execute("""
				UPDATE care_encounter
				SET care_navigator_account_id=?, last_updated_by_account_id=?
				WHERE care_encounter_id=?
				AND care_encounter_status_id='OPEN'
				""", careNavigatorAccountId, updatedByAccountId, careEncounterId);

		return findCareEncounterByIdForInstitutionId(careEncounterId, institutionId).get();
	}

	protected boolean isEligibleCareNavigatorAssignment(@Nonnull UUID careEncounterId,
																						 @Nonnull UUID careNavigatorAccountId,
																						 @Nonnull InstitutionId institutionId) {
		return getDatabase().queryForObject("""
				SELECT EXISTS (
					SELECT 1
					FROM care_navigator_provider_account mapping
					JOIN account ON account.account_id=mapping.account_id
					JOIN account_capability
						ON account_capability.account_id=account.account_id
						AND account_capability.account_capability_type_id='NAVIGATOR'
					JOIN LATERAL (
						SELECT appointment.provider_id
						FROM appointment
						WHERE appointment.care_encounter_id=?
						ORDER BY
							CASE WHEN appointment.canceled=FALSE
								AND appointment.canceled_for_reschedule=FALSE
								AND appointment.attendance_status_id='UNKNOWN' THEN 0 ELSE 1 END,
							appointment.start_time DESC,
							appointment.appointment_id
						LIMIT 1
					) encounter_appointment ON encounter_appointment.provider_id=mapping.provider_id
					JOIN provider ON provider.provider_id=mapping.provider_id
					WHERE mapping.account_id=?
					AND account.active=TRUE
					AND account.role_id IN ('ADMINISTRATOR', 'PROVIDER')
					AND account.institution_id=?
					AND provider.active=TRUE
					AND provider.institution_id=account.institution_id
					AND EXISTS (
						SELECT 1
						FROM provider_support_role
						WHERE provider_support_role.provider_id=provider.provider_id
						AND provider_support_role.support_role_id='CARE_NAVIGATOR'
					)
				)
				""", Boolean.class, careEncounterId, careNavigatorAccountId, institutionId).orElse(false);
	}

	public boolean deleteCareEncounter(@Nullable UUID careEncounterId,
																 @Nullable InstitutionId institutionId,
																 @Nullable UUID accountId) {
		ValidationException validationException = new ValidationException();
		CareEncounter careEncounter = null;

		if (institutionId == null)
			validationException.add(new FieldError("institutionId", getStrings().get("Institution ID is required.")));

		if (accountId == null)
			validationException.add(new FieldError("accountId", getStrings().get("Account ID is required.")));

		if (careEncounterId == null || institutionId == null) {
			validationException.add(new FieldError("careEncounterId", getStrings().get("Care Encounter ID is invalid.")));
		} else {
			careEncounter = findCareEncounterByIdForInstitutionId(careEncounterId, institutionId).orElse(null);

			if (careEncounter == null)
				validationException.add(new FieldError("careEncounterId", getStrings().get("Care Encounter ID is invalid.")));
			else if (careEncounter.getCareEncounterStatusId() == CareEncounterStatusId.OPEN)
				validationException.add(new FieldError("careEncounterStatusId", getStrings().get(
						"Open Care Encounters must be closed or canceled before they can be deleted.")));
			else if (hasPendingScheduledMessage(careEncounterId, null))
				validationException.add(new FieldError("careEncounterScheduledMessageId", getStrings().get(
						"Pending follow-up messages must be sent or canceled before the Care Encounter can be deleted.")));
		}

		if (validationException.hasErrors())
			throw validationException;

		return getDatabase().execute("""
				UPDATE care_encounter
				SET deleted=TRUE, last_updated_by_account_id=?
				WHERE care_encounter_id=?
				""", accountId, careEncounterId) > 0;
	}

	@Nonnull
	public List<CareEncounterCancellationReason> findCareEncounterCancellationReasons() {
		return getDatabase().queryForList("""
				SELECT *
				FROM care_encounter_cancellation_reason
				ORDER BY display_order, description
				""", CareEncounterCancellationReason.class);
	}

	@Nonnull
	public List<AttendanceStatus> findSelectableAttendanceStatuses() {
		return getDatabase().queryForList("""
				SELECT *
				FROM attendance_status
				WHERE attendance_status_id IN ('ATTENDED', 'MISSED')
				ORDER BY CASE attendance_status_id
					WHEN 'ATTENDED' THEN 1
					WHEN 'MISSED' THEN 2
				END
				""", AttendanceStatus.class);
	}

	@Nonnull
	public CareEncounter changeCareEncounterAppointmentAttendanceStatus(@Nullable UUID careEncounterId,
																				 @Nullable InstitutionId institutionId,
																				 @Nonnull ChangeAppointmentAttendanceStatusRequest request) {
		requireNonNull(request);

		UUID appointmentId = request.getAppointmentId();
		UUID accountId = request.getAccountId();
		AttendanceStatusId attendanceStatusId = request.getAttendanceStatusId();
		ValidationException validationException = new ValidationException();
		CareEncounter careEncounter = null;
		Appointment appointment = null;

		if (institutionId == null)
			validationException.add(new FieldError("institutionId", getStrings().get("Institution ID is required.")));

		if (accountId == null)
			validationException.add(new FieldError("accountId", getStrings().get("Account ID is required.")));

		if (attendanceStatusId == null) {
			validationException.add(new FieldError("attendanceStatusId",
					getStrings().get("Attendance Status ID is required.")));
		} else if (attendanceStatusId != AttendanceStatusId.ATTENDED
				&& attendanceStatusId != AttendanceStatusId.MISSED) {
			validationException.add(new FieldError("attendanceStatusId",
					getStrings().get("Attendance Status ID must be Attended or Missed.")));
		}

		if (careEncounterId == null) {
			validationException.add(new FieldError("careEncounterId", getStrings().get("Care Encounter ID is required.")));
		} else if (institutionId != null) {
			careEncounter = findCareEncounterByIdForInstitutionIdForUpdate(careEncounterId, institutionId).orElse(null);

			if (careEncounter == null)
				validationException.add(new FieldError("careEncounterId", getStrings().get("Care Encounter ID is invalid.")));
			else if (careEncounter.getCareEncounterStatusId() != CareEncounterStatusId.OPEN)
				validationException.add(new FieldError("careEncounterStatusId",
						getStrings().get("Attendance can only be updated for open Care Encounters.")));
		}

		if (appointmentId == null) {
			validationException.add(new FieldError("appointmentId", getStrings().get("Appointment ID is required.")));
		} else if (careEncounter != null && institutionId != null) {
			appointment = findCareNavigatorAppointmentByIdAndCareEncounterIdForInstitutionId(
					appointmentId, careEncounterId, institutionId).orElse(null);

			if (appointment == null) {
				validationException.add(new FieldError("appointmentId", getStrings().get("Appointment ID is invalid.")));
			} else {
				Appointment latestAppointment = findLatestAppointmentByCareEncounterIdForInstitutionId(
						careEncounterId, institutionId).orElse(null);

				if (latestAppointment == null || !appointmentId.equals(latestAppointment.getAppointmentId())) {
					validationException.add(new FieldError("appointmentId",
							getStrings().get("Attendance can only be updated for the current appointment.")));
				} else if (Boolean.TRUE.equals(appointment.getCanceled())
						|| Boolean.TRUE.equals(appointment.getCanceledForReschedule())
						|| appointment.getAttendanceStatusId() == AttendanceStatusId.CANCELED) {
					validationException.add(new FieldError("appointmentId",
							getStrings().get("Attendance cannot be updated for a canceled appointment.")));
				} else if (appointment.getStartTime() == null
						|| appointment.getTimeZone() == null
						|| Instant.now().isBefore(appointment.getStartTime().atZone(appointment.getTimeZone()).toInstant())) {
					validationException.add(new FieldError("appointmentId",
							getStrings().get("Attendance cannot be updated before the appointment starts.")));
				}
			}
		}

		if (validationException.hasErrors())
			throw validationException;

		boolean changed = getAppointmentService().changeAppointmentAttendanceStatus(request);

		if (changed)
			getDatabase().execute("""
					UPDATE care_encounter
					SET last_updated_by_account_id=?
					WHERE care_encounter_id=?
					""", accountId, careEncounterId);

		return findCareEncounterByIdForInstitutionId(careEncounterId, institutionId).get();
	}

	@Nonnull
	public CareEncounter cancelCareEncounterAppointment(@Nullable UUID careEncounterId,
																	 @Nullable UUID appointmentId,
																	 @Nullable InstitutionId institutionId,
																	 @Nullable UUID canceledByAccountId,
																	 @Nonnull CancelCareEncounterAppointmentRequest request) {
		requireNonNull(request);

		ValidationException validationException = new ValidationException();
		CareEncounter careEncounter = null;
		Appointment appointment = null;
		String cancellationReason = trimToNull(request.getCancellationReason());

		if (cancellationReason == null)
			validationException.add(new FieldError("cancellationReason",
					getStrings().get("Cancellation reason is required.")));
		else if (cancellationReason.length() > MAXIMUM_APPOINTMENT_CANCELLATION_REASON_LENGTH)
			validationException.add(new FieldError("cancellationReason",
					getStrings().get("Cancellation reason is too long.")));

		if (institutionId == null)
			validationException.add(new FieldError("institutionId", getStrings().get("Institution ID is required.")));

		if (canceledByAccountId == null)
			validationException.add(new FieldError("canceledByAccountId",
					getStrings().get("Canceled By Account ID is required.")));

		if (careEncounterId == null) {
			validationException.add(new FieldError("careEncounterId", getStrings().get("Care Encounter ID is required.")));
		} else if (institutionId != null) {
			careEncounter = findCareEncounterByIdForInstitutionIdForUpdate(careEncounterId, institutionId).orElse(null);

			if (careEncounter == null)
				validationException.add(new FieldError("careEncounterId", getStrings().get("Care Encounter ID is invalid.")));
			else if (careEncounter.getCareEncounterStatusId() != CareEncounterStatusId.OPEN)
				validationException.add(new FieldError("careEncounterStatusId",
						getStrings().get("Appointments can only be canceled for open Care Encounters.")));
		}

		if (appointmentId == null) {
			validationException.add(new FieldError("appointmentId", getStrings().get("Appointment ID is required.")));
		} else if (careEncounter != null && institutionId != null) {
			appointment = findCareNavigatorAppointmentByIdAndCareEncounterIdForInstitutionId(
					appointmentId, careEncounterId, institutionId).orElse(null);

			if (appointment == null) {
				validationException.add(new FieldError("appointmentId", getStrings().get("Appointment ID is invalid.")));
			} else if (Boolean.TRUE.equals(appointment.getCanceled())
					|| Boolean.TRUE.equals(appointment.getCanceledForReschedule())
					|| appointment.getAttendanceStatusId() != AttendanceStatusId.UNKNOWN) {
				validationException.add(new FieldError("appointmentId",
						getStrings().get("Only an active appointment can be canceled.")));
			}
		}

		if (validationException.hasErrors())
			throw validationException;

		CancelAppointmentRequest cancelAppointmentRequest = new CancelAppointmentRequest();
		cancelAppointmentRequest.setAppointmentId(appointmentId);
		cancelAppointmentRequest.setAccountId(appointment.getAccountId());
		cancelAppointmentRequest.setCanceledByAccountId(canceledByAccountId);
		cancelAppointmentRequest.setCanceledByWebhook(false);
		cancelAppointmentRequest.setCanceledForReschedule(false);
		cancelAppointmentRequest.setCancellationReason(cancellationReason);
		getAppointmentService().cancelAppointment(cancelAppointmentRequest);

		getDatabase().execute("""
				UPDATE care_encounter
				SET last_updated_by_account_id=?
				WHERE care_encounter_id=?
				""", canceledByAccountId, careEncounterId);

		return findCareEncounterByIdForInstitutionId(careEncounterId, institutionId).get();
	}

	@Nonnull
	public CareEncounter cancelCareEncounter(@Nullable UUID careEncounterId,
																		 @Nullable InstitutionId institutionId,
																	 @Nullable UUID accountId,
																	 @Nonnull CancelCareEncounterRequest request) {
		requireNonNull(request);

		ValidationException validationException = new ValidationException();
		CareEncounter careEncounter = null;
		CareEncounterCancellationReasonId cancellationReasonId = request.getCareEncounterCancellationReasonId();
		String cancellationReasonOtherText = trimToNull(request.getCareEncounterCancellationReasonOtherText());

		if (cancellationReasonId == null) {
			validationException.add(new FieldError("careEncounterCancellationReasonId",
					getStrings().get("Cancellation reason is required.")));
		} else if (cancellationReasonId == CareEncounterCancellationReasonId.OTHER) {
			if (cancellationReasonOtherText == null)
				validationException.add(new FieldError("careEncounterCancellationReasonOtherText",
						getStrings().get("Please provide a cancellation reason.")));
		} else if (cancellationReasonOtherText != null) {
			validationException.add(new FieldError("careEncounterCancellationReasonOtherText",
					getStrings().get("Other cancellation reason text is only permitted when Other is selected.")));
		}

		if (cancellationReasonOtherText != null
				&& cancellationReasonOtherText.length() > MAXIMUM_CANCELLATION_REASON_OTHER_TEXT_LENGTH)
			validationException.add(new FieldError("careEncounterCancellationReasonOtherText",
					getStrings().get("Cancellation reason is too long.")));

		if (institutionId == null)
			validationException.add(new FieldError("institutionId", getStrings().get("Institution ID is required.")));

		if (accountId == null)
			validationException.add(new FieldError("accountId", getStrings().get("Account ID is required.")));

		if (careEncounterId == null) {
			validationException.add(new FieldError("careEncounterId", getStrings().get("Care Encounter ID is required.")));
		} else if (institutionId != null) {
			careEncounter = findCareEncounterByIdForInstitutionIdForUpdate(careEncounterId, institutionId).orElse(null);

			if (careEncounter == null)
				validationException.add(new FieldError("careEncounterId", getStrings().get("Care Encounter ID is invalid.")));
			else {
				if (careEncounter.getCareEncounterStatusId() != CareEncounterStatusId.OPEN)
					validationException.add(new FieldError("careEncounterStatusId", getStrings().get("Only open Care Encounters can be canceled.")));
				else if (findActiveAppointmentByCareEncounterIdForInstitutionId(careEncounterId, institutionId).isPresent())
					validationException.add(new FieldError("appointmentId", getStrings().get(
							"The active appointment must be completed or canceled before the Care Encounter can be canceled.")));
				else if (hasPendingScheduledMessage(careEncounterId, null))
					validationException.add(new FieldError("careEncounterScheduledMessageId", getStrings().get(
							"Pending follow-up messages must be sent or canceled before the Care Encounter can be canceled.")));
			}
		}

		if (validationException.hasErrors())
			throw validationException;

		getDatabase().execute("""
				UPDATE care_encounter
				SET care_encounter_status_id='CANCELED',
					closed_at=NOW(),
					canceled_by_account_id=?,
					care_encounter_cancellation_reason_id=?,
					care_encounter_cancellation_reason_other_text=?,
					last_updated_by_account_id=?
				WHERE care_encounter_id=?
				AND care_encounter_status_id='OPEN'
				""", accountId, cancellationReasonId, cancellationReasonOtherText, accountId, careEncounterId);

		return findCareEncounterByIdForInstitutionId(careEncounterId, institutionId).get();
	}

	@Nonnull
	protected Optional<CareEncounter> findCareEncounterByAppointmentIdForInstitutionId(@Nullable UUID appointmentId,
																						 @Nullable InstitutionId institutionId) {
		if (appointmentId == null || institutionId == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				SELECT care_encounter.*
				FROM appointment
				JOIN care_encounter ON care_encounter.care_encounter_id=appointment.care_encounter_id
				JOIN provider ON provider.provider_id=appointment.provider_id
				WHERE appointment.appointment_id=?
				AND care_encounter.deleted=FALSE
				AND provider.institution_id=?
				""", CareEncounter.class, appointmentId, institutionId);
	}

	@Nonnull
	protected Optional<CareEncounter> findCareEncounterByIdForInstitutionIdForUpdate(@Nullable UUID careEncounterId,
																											@Nullable InstitutionId institutionId) {
		if (careEncounterId == null || institutionId == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				SELECT care_encounter.*
				FROM care_encounter
				JOIN LATERAL (
					SELECT appointment.*
					FROM appointment
					WHERE appointment.care_encounter_id=care_encounter.care_encounter_id
					ORDER BY appointment.start_time DESC, appointment.appointment_id
					LIMIT 1
				) appointment ON TRUE
				JOIN provider ON provider.provider_id=appointment.provider_id
				WHERE care_encounter.care_encounter_id=?
				AND care_encounter.deleted=FALSE
				AND provider.institution_id=?
				FOR UPDATE OF care_encounter
				""", CareEncounter.class, careEncounterId, institutionId);
	}

	@Nonnull
	public List<Appointment> findAppointmentsByCareEncounterIdForInstitutionId(@Nullable UUID careEncounterId,
																										 @Nullable InstitutionId institutionId) {
		if (careEncounterId == null || institutionId == null)
			return List.of();

		return getDatabase().queryForList("""
				SELECT appointment.*
				FROM appointment
				JOIN provider ON provider.provider_id=appointment.provider_id
				WHERE appointment.care_encounter_id=?
				AND provider.institution_id=?
				ORDER BY appointment.created DESC, appointment.appointment_id DESC
				""", Appointment.class, careEncounterId, institutionId);
	}

	@Nonnull
	public Optional<Appointment> findLatestAppointmentByCareEncounterIdForInstitutionId(@Nullable UUID careEncounterId,
																										@Nullable InstitutionId institutionId) {
		if (careEncounterId == null || institutionId == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				SELECT appointment.*
				FROM appointment
				JOIN provider ON provider.provider_id=appointment.provider_id
				WHERE appointment.care_encounter_id=?
				AND provider.institution_id=?
				ORDER BY appointment.created DESC, appointment.appointment_id DESC
				LIMIT 1
				""", Appointment.class, careEncounterId, institutionId);
	}

	@Nonnull
	public Optional<Appointment> findActiveAppointmentByCareEncounterIdForInstitutionId(@Nullable UUID careEncounterId,
																														@Nullable InstitutionId institutionId) {
		if (careEncounterId == null || institutionId == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				SELECT appointment.*
				FROM appointment
				JOIN provider ON provider.provider_id=appointment.provider_id
				WHERE appointment.care_encounter_id=?
				AND appointment.canceled=FALSE
				AND appointment.canceled_for_reschedule=FALSE
				AND appointment.attendance_status_id='UNKNOWN'
				AND provider.institution_id=?
				ORDER BY appointment.start_time DESC, appointment.appointment_id
				LIMIT 1
				""", Appointment.class, careEncounterId, institutionId);
	}

	@Nonnull
	protected Optional<Appointment> findCareNavigatorAppointmentByIdAndInstitutionId(@Nullable UUID appointmentId,
																												@Nullable InstitutionId institutionId) {
		if (appointmentId == null || institutionId == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				SELECT appointment.*
				FROM appointment
				JOIN provider ON provider.provider_id=appointment.provider_id
				JOIN provider_support_role
					ON provider_support_role.provider_id=appointment.provider_id
					AND provider_support_role.support_role_id='CARE_NAVIGATOR'
				WHERE appointment.appointment_id=?
				AND provider.institution_id=?
				""", Appointment.class, appointmentId, institutionId);
	}

	@Nonnull
	protected Optional<Appointment> findCareNavigatorAppointmentByIdAndCareEncounterIdForInstitutionId(
			@Nullable UUID appointmentId,
			@Nullable UUID careEncounterId,
			@Nullable InstitutionId institutionId) {
		if (appointmentId == null || careEncounterId == null || institutionId == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				SELECT appointment.*
				FROM appointment
				JOIN provider ON provider.provider_id=appointment.provider_id
				JOIN provider_support_role
					ON provider_support_role.provider_id=appointment.provider_id
					AND provider_support_role.support_role_id='CARE_NAVIGATOR'
				WHERE appointment.appointment_id=?
				AND appointment.care_encounter_id=?
				AND provider.institution_id=?
				FOR UPDATE OF appointment
				""", Appointment.class, appointmentId, careEncounterId, institutionId);
	}

	@Nullable
	protected CareEncounter validateOpenCareEncounter(@Nullable UUID careEncounterId,
																			@Nullable InstitutionId institutionId,
																			@Nullable UUID accountId,
																			@Nonnull ValidationException validationException,
																			boolean lockForUpdate) {
		if (institutionId == null)
			validationException.add(new FieldError("institutionId", getStrings().get("Institution ID is required.")));
		if (lockForUpdate && accountId == null)
			validationException.add(new FieldError("accountId", getStrings().get("Account ID is required.")));
		if (careEncounterId == null) {
			validationException.add(new FieldError("careEncounterId", getStrings().get("Care Encounter ID is required.")));
			return null;
		}
		if (institutionId == null)
			return null;

		CareEncounter careEncounter = (lockForUpdate
				? findCareEncounterByIdForInstitutionIdForUpdate(careEncounterId, institutionId)
				: findCareEncounterByIdForInstitutionId(careEncounterId, institutionId)).orElse(null);
		if (careEncounter == null)
			validationException.add(new FieldError("careEncounterId", getStrings().get("Care Encounter ID is invalid.")));
		else if (careEncounter.getCareEncounterStatusId() != CareEncounterStatusId.OPEN)
			validationException.add(new FieldError("careEncounterStatusId",
					getStrings().get("Scheduled messages can only be changed for open Care Encounters.")));
		return careEncounter;
	}

	protected void validateScheduledMessageType(@Nullable CareEncounterScheduledMessageTypeId typeId,
																			@Nonnull ValidationException validationException) {
		if (typeId == null)
			validationException.add(new FieldError("careEncounterScheduledMessageTypeId",
					getStrings().get("Message type is required.")));
	}

	@Nullable
	protected String validateAndSanitizeCustomEmailText(@Nullable String customEmailText,
																						@Nonnull ValidationException validationException) {
		String original = trimToNull(customEmailText);
		if (original != null && original.length() > MAXIMUM_CUSTOM_EMAIL_TEXT_LENGTH)
			validationException.add(new FieldError("customEmailText", getStrings().get("Custom email text is too long.")));
		String sanitized = original == null ? null : trimToNull(this.customEmailTextSanitizingPolicy.sanitize(original));
		if (sanitized == null)
			validationException.add(new FieldError("customEmailText", getStrings().get("Custom email text is required.")));
		return sanitized;
	}

	protected void validateRecipientEmailAddress(@Nullable CareEncounter careEncounter,
																			 @Nonnull ValidationException validationException) {
		String emailAddress = careEncounter == null ? null : trimToNull(careEncounter.getEmailAddress());
		if (emailAddress == null)
			validationException.add(new FieldError("emailAddress",
					getStrings().get("The Care Encounter must have an email address before a follow-up can be scheduled.")));
		else if (!isValidEmailAddress(emailAddress))
			validationException.add(new FieldError("emailAddress", getStrings().get("Email address is invalid.")));
	}

	protected void validateAttendedAppointmentForFollowUp(
			@Nullable CareEncounter careEncounter,
			@Nullable InstitutionId institutionId,
			@Nullable CareEncounterScheduledMessageTypeId typeId,
			@Nonnull ValidationException validationException) {
		if (careEncounter == null || institutionId == null || typeId != CareEncounterScheduledMessageTypeId.FOLLOW_UP)
			return;

		Appointment appointment = findLatestAppointmentByCareEncounterIdForInstitutionId(
				careEncounter.getCareEncounterId(), institutionId).orElse(null);
		if (appointment == null
				|| appointment.getAttendanceStatusId() != AttendanceStatusId.ATTENDED
				|| Boolean.TRUE.equals(appointment.getCanceled())
				|| Boolean.TRUE.equals(appointment.getCanceledForReschedule()))
			validationException.add(new FieldError("attendanceStatusId", getStrings().get(
					"A follow-up message can only be scheduled after the current appointment is marked Attended.")));
	}

	@Nullable
	protected LocalDateTime validateScheduledAt(@Nonnull CreateCareEncounterScheduledMessageRequest request,
																			 @Nonnull ValidationException validationException) {
		if (request.getScheduledAtDate() == null)
			validationException.add(new FieldError("scheduledAtDate", getStrings().get("Scheduled date is required.")));
		if (request.getScheduledAtTime() == null)
			validationException.add(new FieldError("scheduledAtTime", getStrings().get("Scheduled time is required.")));
		if (request.getScheduledAtDate() == null || request.getScheduledAtTime() == null)
			return null;

		return LocalDateTime.of(request.getScheduledAtDate(), request.getScheduledAtTime());
	}

	@Nonnull
	protected FollowUpEmailSnapshot renderCareEncounterFollowUp(@Nonnull CareEncounter careEncounter,
																			@Nonnull InstitutionId institutionId,
																			@Nonnull String customEmailText,
																			@Nonnull UUID messageId,
											@Nullable UUID linkRevisionId) {
		Institution institution = getInstitutionService().findInstitutionById(institutionId).get();
		Appointment appointment = findLatestAppointmentByCareEncounterIdForInstitutionId(
				careEncounter.getCareEncounterId(), institutionId).orElseThrow();
		Account patient = getAccountService().findAccountById(careEncounter.getAccountId()).orElse(null);
		Account careNavigator = getAccountService().findAccountById(careEncounter.getCareNavigatorAccountId()).orElse(null);
		Locale locale = Locale.forLanguageTag("en-US");
		Map<String, Object> context = new HashMap<>();
		context.put("customEmailText", customEmailText);
		context.put("patientFirstName", patient == null ? appointment.getFirstName() : patient.getFirstName());
		context.put("patientFullName", patient == null
				? String.format("%s %s", appointment.getFirstName(), appointment.getLastName()).trim()
				: getAccountService().determineDisplayName(patient));
		context.put("appointmentDate", appointment.getStartTime().toLocalDate());
		context.put("appointmentDateDescription", getFormatter().formatDate(
				appointment.getStartTime().toLocalDate(), FormatStyle.MEDIUM, locale));
		context.put("careNavigatorDisplayName", careNavigator == null ? null
				: getAccountService().determineDisplayName(careNavigator));
		String patientWebappBaseUrl = getInstitutionService()
				.findWebappBaseUrlByInstitutionIdAndUserExperienceTypeId(institutionId, UserExperienceTypeId.PATIENT)
				.orElse(null);
		if (linkRevisionId != null && patientWebappBaseUrl != null)
			context.put("customEmailText", trackFollowUpResourceLinks(customEmailText,
					careEncounter.getCareEncounterId(), messageId, linkRevisionId, patientWebappBaseUrl));
		String careNavigatorBookingPath = patient == null ? null
				: getInstitutionService().findFeaturesByInstitutionId(institution, patient).stream()
				.filter(feature -> feature.getFeatureId() == FeatureId.RESOURCE_NAVIGATOR
						&& feature.getUrlName() != null)
				.map(feature -> feature.getUrlName())
				.findFirst().orElse(null);
		context.put("careNavigatorBookingUrl", patientWebappBaseUrl == null || careNavigatorBookingPath == null ? null
				: patientWebappBaseUrl.replaceAll("/+$", "")
				+ (careNavigatorBookingPath.startsWith("/") ? "" : "/") + careNavigatorBookingPath);
		context.put("supportEmailAddress", institution.getSupportEmailAddress());
		context.put("integratedCarePhoneNumber", institution.getIntegratedCarePhoneNumber());
		context.put("careNavigatorCrisisPhoneNumber", institution.getCareNavigatorCrisisPhoneNumber());
		context.put("integratedCarePhoneNumberFormatted", getFormatter().formatPhoneNumber(
				institution.getIntegratedCarePhoneNumber(), locale));
		context.put("integratedCareAvailabilityDescription", institution.getIntegratedCareAvailabilityDescription());
		context.put("clinicalSupportPhoneNumber", institution.getClinicalSupportPhoneNumber());
		context.put("clinicalSupportPhoneNumberFormatted", getFormatter().formatPhoneNumber(
				institution.getClinicalSupportPhoneNumber(), locale));

		EmailMessage templateEmailMessage = new EmailMessage.Builder(messageId, institutionId,
				EmailMessageTemplate.CARE_ENCOUNTER_FOLLOW_UP, locale)
				.messageContext(context)
				.toAddresses(List.of(careEncounter.getEmailAddress()))
				.build();
		EmailMessage preparedEmailMessage = getMessageService().prepareEmailMessage(templateEmailMessage);
		String emailSubject = getEmailHandlebarsTemplater().mergeTemplate(
				EmailMessageTemplate.CARE_ENCOUNTER_FOLLOW_UP.name(), "subject", locale,
				preparedEmailMessage.getMessageContext()).map(String::trim).orElseThrow();
		String emailBody = getEmailHandlebarsTemplater().mergeTemplate(
				EmailMessageTemplate.CARE_ENCOUNTER_FOLLOW_UP.name(), "body", locale,
				preparedEmailMessage.getMessageContext()).map(String::trim).orElseThrow();
		RenderedEmailMessage renderedEmailMessage = new RenderedEmailMessage(emailSubject, emailBody);
		EmailMessage freeformEmailMessage = new EmailMessage.Builder(messageId, institutionId,
				EmailMessageTemplate.FREEFORM, locale)
				.messageContext(Map.of("subject", emailSubject, "body", emailBody))
				.fromAddress(institution.getDefaultFromEmailAddress())
				.toAddresses(List.of(careEncounter.getEmailAddress()))
				.build();
		return new FollowUpEmailSnapshot(renderedEmailMessage, freeformEmailMessage, institution.getTimeZone());
	}

	@Nonnull
	protected String trackFollowUpResourceLinks(@Nonnull String sanitizedHtml, @Nonnull UUID careEncounterId,
															@Nonnull UUID messageId, @Nonnull UUID linkRevisionId,
															@Nonnull String patientWebappBaseUrl) {
		return CareEncounterFollowUpLinkRewriter.rewrite(sanitizedHtml, patientWebappBaseUrl, destinationUrl -> {
			UUID linkId = UUID.randomUUID();
			getDatabase().execute("""
				INSERT INTO care_encounter_follow_up_link
				(care_encounter_follow_up_link_id, care_encounter_id, message_id, revision_id, destination_url)
				VALUES (?,?,?,?,?)
				""", linkId, careEncounterId, messageId, linkRevisionId, destinationUrl);
			return linkId;
		});
	}

	@Nonnull
	protected Map<String, Object> scheduledMessageMetadata(@Nonnull UUID careEncounterId,
																					 @Nonnull CareEncounterScheduledMessageTypeId typeId) {
		return Map.of("careEncounterId", careEncounterId.toString(),
				"careEncounterScheduledMessageTypeId", typeId.name());
	}

	@Nonnull
	protected ValidationException pendingScheduledMessageValidationException(@Nonnull String message) {
		ValidationException validationException = new ValidationException();
		validationException.add(new FieldError("careEncounterScheduledMessageId", getStrings().get(message)));
		return validationException;
	}

	@Nonnull
	protected String careEncounterScheduledMessageSelectSql() {
		return """
				SELECT cesm.*, ce.care_encounter_status_id,
					cesmt.description AS care_encounter_scheduled_message_type_description,
					sm.scheduled_message_status_id, sms.description AS scheduled_message_status_description,
					sm.scheduled_message_source_id, sm.scheduled_by_account_id, sm.message_id,
					sm.scheduled_at, sm.time_zone, sm.processed_at, sm.canceled_at, sm.errored_at,
					ml.message_status_id, ms.description AS message_status_description,
					ml.processed AS sent_at, ml.delivered AS delivered_at,
					ml.delivery_failed AS delivery_failed_at, ml.delivery_failed_reason,
					ml.complaint_registered AS complaint_registered_at,
					COALESCE(link_activity.resource_link_count, 0) AS resource_link_count,
					COALESCE(link_activity.resource_link_opened_count, 0) AS resource_link_opened_count,
					link_activity.resource_link_last_clicked_at,
					COALESCE(NULLIF(BTRIM(scheduled_by.display_name), ''),
						NULLIF(BTRIM(CONCAT_WS(' ', scheduled_by.first_name, scheduled_by.last_name)), ''))
						AS scheduled_by_account_display_name,
					COALESCE(NULLIF(BTRIM(created_by.display_name), ''),
						NULLIF(BTRIM(CONCAT_WS(' ', created_by.first_name, created_by.last_name)), ''))
						AS created_by_account_display_name,
					COALESCE(NULLIF(BTRIM(updated_by.display_name), ''),
						NULLIF(BTRIM(CONCAT_WS(' ', updated_by.first_name, updated_by.last_name)), ''))
						AS last_updated_by_account_display_name,
					COALESCE(NULLIF(BTRIM(deleted_by.display_name), ''),
						NULLIF(BTRIM(CONCAT_WS(' ', deleted_by.first_name, deleted_by.last_name)), ''))
						AS deleted_by_account_display_name
				FROM care_encounter_scheduled_message cesm
				JOIN care_encounter ce ON ce.care_encounter_id=cesm.care_encounter_id
				JOIN care_encounter_scheduled_message_type cesmt
					ON cesmt.care_encounter_scheduled_message_type_id=cesm.care_encounter_scheduled_message_type_id
				JOIN scheduled_message sm ON sm.scheduled_message_id=cesm.scheduled_message_id
				JOIN scheduled_message_status sms
					ON sms.scheduled_message_status_id=sm.scheduled_message_status_id
				LEFT JOIN message_log ml ON ml.message_id=sm.message_id
				LEFT JOIN message_status ms ON ms.message_status_id=ml.message_status_id
				LEFT JOIN LATERAL (
					SELECT COUNT(*) AS resource_link_count,
						COUNT(*) FILTER (WHERE link.click_count > 0) AS resource_link_opened_count,
						MAX(link.last_clicked_at) AS resource_link_last_clicked_at
					FROM care_encounter_follow_up_link link
					WHERE link.message_id=sm.message_id
				) link_activity ON TRUE
				LEFT JOIN account scheduled_by ON scheduled_by.account_id=sm.scheduled_by_account_id
				JOIN account created_by ON created_by.account_id=cesm.created_by_account_id
				JOIN account updated_by ON updated_by.account_id=cesm.last_updated_by_account_id
				LEFT JOIN account deleted_by ON deleted_by.account_id=cesm.deleted_by_account_id
				""";
	}

	protected static class FollowUpEmailSnapshot {
		@Nonnull final RenderedEmailMessage renderedEmailMessage;
		@Nonnull final EmailMessage freeformEmailMessage;
		@Nonnull final ZoneId timeZone;
		FollowUpEmailSnapshot(@Nonnull RenderedEmailMessage renderedEmailMessage,
												@Nonnull EmailMessage freeformEmailMessage,
												@Nonnull ZoneId timeZone) {
			this.renderedEmailMessage = requireNonNull(renderedEmailMessage);
			this.freeformEmailMessage = requireNonNull(freeformEmailMessage);
			this.timeZone = requireNonNull(timeZone);
		}
	}

	protected void validateCareEncounterForNote(@Nullable UUID careEncounterId,
																				@Nullable InstitutionId institutionId,
																				@Nullable UUID accountId,
																				@Nonnull ValidationException validationException) {
		requireNonNull(validationException);

		if (institutionId == null)
			validationException.add(new FieldError("institutionId", getStrings().get("Institution ID is required.")));

		if (accountId == null)
			validationException.add(new FieldError("accountId", getStrings().get("Account ID is required.")));

		if (careEncounterId == null) {
			validationException.add(new FieldError("careEncounterId", getStrings().get("Care Encounter ID is required.")));
		} else if (institutionId != null) {
			CareEncounter careEncounter = findCareEncounterByIdForInstitutionId(careEncounterId, institutionId).orElse(null);
			if (careEncounter == null)
				validationException.add(new FieldError("careEncounterId", getStrings().get("Care Encounter ID is invalid.")));
			else if (careEncounter.getCareEncounterStatusId() != CareEncounterStatusId.OPEN)
				validationException.add(new FieldError("careEncounterStatusId",
						getStrings().get("Notes cannot be changed after a Care Encounter is closed or canceled.")));
		}
	}

	protected void touchCareEncounter(@Nonnull UUID careEncounterId,
															@Nonnull UUID accountId) {
		requireNonNull(careEncounterId);
		requireNonNull(accountId);

		getDatabase().execute("""
				UPDATE care_encounter
				SET last_updated_by_account_id=?
				WHERE care_encounter_id=?
				""", accountId, careEncounterId);
	}

	@Nullable
	protected static String normalizeNote(@Nullable String note) {
		return trimToNull(note);
	}

	protected void validateNote(@Nullable String note,
														@Nonnull ValidationException validationException) {
		requireNonNull(validationException);

		if (note == null)
			validationException.add(new FieldError("note", getStrings().get("Note is required.")));
		else if (note.length() > MAXIMUM_NOTE_LENGTH)
			validationException.add(new FieldError("note", getStrings().get("Note is too long.")));
	}

	@Nonnull
	protected Database getDatabase() {
		return this.databaseProvider.get();
	}

	@Nonnull
	protected AppointmentService getAppointmentService() {
		return this.appointmentService;
	}

	@Nonnull
	protected Normalizer getNormalizer() {
		return this.normalizer;
	}

	@Nonnull
	protected Strings getStrings() {
		return this.strings;
	}

	@Nonnull
	protected MessageService getMessageService() { return this.messageService; }

	@Nonnull
	protected InstitutionService getInstitutionService() { return this.institutionServiceProvider.get(); }

	@Nonnull
	protected AccountService getAccountService() { return this.accountServiceProvider.get(); }

	@Nonnull
	protected Formatter getFormatter() { return this.formatter; }

	@Nonnull
	protected HandlebarsTemplater getEmailHandlebarsTemplater() { return this.emailHandlebarsTemplater; }

}
