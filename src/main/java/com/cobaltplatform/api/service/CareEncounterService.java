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
import com.cobaltplatform.api.model.api.request.CreateCareEncounterRequest;
import com.cobaltplatform.api.model.api.request.FindCareEncountersRequest;
import com.cobaltplatform.api.model.api.request.FindCareEncountersRequest.CareEncounterSortColumnId;
import com.cobaltplatform.api.model.api.request.UpdateCareEncounterRequest;
import com.cobaltplatform.api.model.db.Appointment;
import com.cobaltplatform.api.model.db.CareEncounter;
import com.cobaltplatform.api.model.db.CareEncounterStatus.CareEncounterStatusId;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.service.FindResult;
import com.cobaltplatform.api.model.service.SortDirectionId;
import com.cobaltplatform.api.util.ValidationException;
import com.cobaltplatform.api.util.ValidationException.FieldError;
import com.cobaltplatform.api.util.db.DatabaseProvider;
import com.lokalized.Strings;
import com.pyranid.Database;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.cobaltplatform.api.util.DatabaseUtility.sqlVaragsParameters;
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
	protected static final int MAXIMUM_NOTES_LENGTH = 20_000;

	@Nonnull
	private final DatabaseProvider databaseProvider;
	@Nonnull
	private final Strings strings;
	@Nonnull
	private final AppointmentService appointmentService;

	@Inject
	public CareEncounterService(@Nonnull DatabaseProvider databaseProvider,
														@Nonnull Strings strings,
														@Nonnull AppointmentService appointmentService) {
		requireNonNull(databaseProvider);
		requireNonNull(strings);
		requireNonNull(appointmentService);

		this.databaseProvider = databaseProvider;
		this.strings = strings;
		this.appointmentService = appointmentService;
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
		CareEncounterSortColumnId careEncounterSortColumnId = request.getCareEncounterSortColumnId();
		SortDirectionId sortDirectionId = request.getSortDirectionId();

		if (careEncounterSortColumnId == null || sortDirectionId == null) {
			careEncounterSortColumnId = CareEncounterSortColumnId.APPOINTMENT_DATE;
			sortDirectionId = SortDirectionId.DESCENDING;
		}
		ValidationException validationException = new ValidationException();

		if (institutionId == null)
			validationException.add(new FieldError("institutionId", getStrings().get("Institution ID is required.")));

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
				JOIN appointment ON appointment.appointment_id=care_encounter.appointment_id
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
						CONCAT_WS(' ', appointment.first_name, appointment.last_name) ILIKE ?
						OR CONCAT_WS(' ', appointment.last_name, appointment.first_name) ILIKE ?
						OR appointment.email_address ILIKE ?
						OR appointment.contact_phone_number ILIKE ?
						OR appointment.title ILIKE ?
						OR care_encounter.notes ILIKE ?
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
				JOIN appointment ON appointment.appointment_id=care_encounter.appointment_id
				JOIN provider ON provider.provider_id=appointment.provider_id
				WHERE care_encounter.care_encounter_id=?
				AND care_encounter.deleted=FALSE
				AND provider.institution_id=?
				""", CareEncounter.class, careEncounterId, institutionId);
	}

	@Nonnull
	public List<CareEncounter> findOtherCareEncountersByAccountId(@Nullable UUID accountId,
																	 @Nullable UUID excludedCareEncounterId,
																	 @Nullable InstitutionId institutionId) {
		if (accountId == null || excludedCareEncounterId == null || institutionId == null)
			return List.of();

		return getDatabase().queryForList("""
				SELECT care_encounter.*
				FROM care_encounter
				JOIN appointment ON appointment.appointment_id=care_encounter.appointment_id
				JOIN provider ON provider.provider_id=appointment.provider_id
				WHERE care_encounter.account_id=?
				AND care_encounter.care_encounter_id<>?
				AND care_encounter.deleted=FALSE
				AND provider.institution_id=?
				ORDER BY appointment.start_time DESC, care_encounter.care_encounter_id ASC
				""", CareEncounter.class, accountId, excludedCareEncounterId, institutionId);
	}

	@Nonnull
	public CareEncounter createCareEncounter(@Nonnull CreateCareEncounterRequest request) {
		requireNonNull(request);

		UUID appointmentId = request.getAppointmentId();
		InstitutionId institutionId = request.getInstitutionId();
		UUID accountId = request.getAccountId();
		String notes = normalizeNotes(request.getNotes());
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

		validateNotes(notes, validationException);

		if (validationException.hasErrors())
			throw validationException;

		UUID careEncounterId = UUID.randomUUID();
		getDatabase().execute("""
				INSERT INTO care_encounter (
					care_encounter_id,
					appointment_id,
					account_id,
					notes,
					created_by_account_id,
					last_updated_by_account_id
				) VALUES (?,?,?,?,?,?)
				ON CONFLICT (appointment_id) DO UPDATE
				SET notes=EXCLUDED.notes,
					deleted=FALSE,
					last_updated_by_account_id=EXCLUDED.last_updated_by_account_id
				""", careEncounterId, appointmentId, appointment.getAccountId(), notes, accountId, accountId);

		return findCareEncounterByAppointmentIdForInstitutionId(appointmentId, institutionId).get();
	}

	@Nonnull
	public CareEncounter updateCareEncounter(@Nonnull UpdateCareEncounterRequest request) {
		requireNonNull(request);

		UUID careEncounterId = request.getCareEncounterId();
		InstitutionId institutionId = request.getInstitutionId();
		UUID accountId = request.getAccountId();
		String notes = normalizeNotes(request.getNotes());
		CareEncounter careEncounter = null;
		ValidationException validationException = new ValidationException();

		if (institutionId == null)
			validationException.add(new FieldError("institutionId", getStrings().get("Institution ID is required.")));

		if (accountId == null)
			validationException.add(new FieldError("accountId", getStrings().get("Account ID is required.")));

		if (careEncounterId == null) {
			validationException.add(new FieldError("careEncounterId", getStrings().get("Care Encounter ID is required.")));
		} else if (institutionId != null) {
			careEncounter = findCareEncounterByIdForInstitutionId(careEncounterId, institutionId).orElse(null);

			if (careEncounter == null)
				validationException.add(new FieldError("careEncounterId", getStrings().get("Care Encounter ID is invalid.")));
		}

		validateNotes(notes, validationException);

		if (validationException.hasErrors())
			throw validationException;

		getDatabase().execute("""
				UPDATE care_encounter
				SET notes=?, last_updated_by_account_id=?
				WHERE care_encounter_id=?
				""", notes, accountId, careEncounterId);

		return findCareEncounterByIdForInstitutionId(careEncounterId, institutionId).get();
	}

	public boolean deleteCareEncounter(@Nullable UUID careEncounterId,
															 @Nullable InstitutionId institutionId,
															 @Nullable UUID accountId) {
		ValidationException validationException = new ValidationException();

		if (institutionId == null)
			validationException.add(new FieldError("institutionId", getStrings().get("Institution ID is required.")));

		if (accountId == null)
			validationException.add(new FieldError("accountId", getStrings().get("Account ID is required.")));

		if (careEncounterId == null || institutionId == null
				|| findCareEncounterByIdForInstitutionId(careEncounterId, institutionId).isEmpty())
			validationException.add(new FieldError("careEncounterId", getStrings().get("Care Encounter ID is invalid.")));

		if (validationException.hasErrors())
			throw validationException;

		return getDatabase().execute("""
				UPDATE care_encounter
				SET deleted=TRUE, last_updated_by_account_id=?
				WHERE care_encounter_id=?
				""", accountId, careEncounterId) > 0;
	}

	@Nonnull
	public CareEncounter cancelCareEncounter(@Nullable UUID careEncounterId,
																		 @Nullable InstitutionId institutionId,
																	 @Nullable UUID accountId,
																	 @Nonnull CancelAppointmentRequest request) {
		requireNonNull(request);

		ValidationException validationException = new ValidationException();
		CareEncounter careEncounter = null;
		Appointment appointment = null;

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

				appointment = findCareNavigatorAppointmentByIdAndInstitutionId(careEncounter.getAppointmentId(), institutionId).orElse(null);

				if (appointment == null || appointment.getCanceled())
					validationException.add(new FieldError("appointmentId", getStrings().get("Appointment cannot be canceled.")));
			}
		}

		if (validationException.hasErrors())
			throw validationException;

		request.setAppointmentId(appointment.getAppointmentId());
		request.setAccountId(appointment.getAccountId());
		request.setCanceledByWebhook(false);
		request.setCanceledForReschedule(false);
		Boolean appointmentCanceled = getAppointmentService().cancelAppointment(request);

		if (!Boolean.TRUE.equals(appointmentCanceled)) {
			ValidationException cancellationException = new ValidationException();
			cancellationException.add(new FieldError("appointmentId", getStrings().get("Appointment cannot be canceled.")));
			throw cancellationException;
		}

		getDatabase().execute("""
				UPDATE care_encounter
				SET care_encounter_status_id='CANCELED',
					closed_at=NOW(),
					canceled_by_account_id=?,
					last_updated_by_account_id=?
				WHERE care_encounter_id=?
				AND care_encounter_status_id IN ('OPEN', 'CLOSED')
				""", accountId, accountId, careEncounterId);

		return findCareEncounterByIdForInstitutionId(careEncounterId, institutionId).get();
	}

	@Nonnull
	protected Optional<CareEncounter> findCareEncounterByAppointmentIdForInstitutionId(@Nullable UUID appointmentId,
																												@Nullable InstitutionId institutionId) {
		if (appointmentId == null || institutionId == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				SELECT care_encounter.*
				FROM care_encounter
				JOIN appointment ON appointment.appointment_id=care_encounter.appointment_id
				JOIN provider ON provider.provider_id=appointment.provider_id
				WHERE care_encounter.appointment_id=?
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
				JOIN appointment ON appointment.appointment_id=care_encounter.appointment_id
				JOIN provider ON provider.provider_id=appointment.provider_id
				WHERE care_encounter.care_encounter_id=?
				AND care_encounter.deleted=FALSE
				AND provider.institution_id=?
				FOR UPDATE OF care_encounter, appointment
				""", CareEncounter.class, careEncounterId, institutionId);
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

	@Nullable
	protected static String normalizeNotes(@Nullable String notes) {
		return trimToNull(notes);
	}

	protected void validateNotes(@Nullable String notes,
														 @Nonnull ValidationException validationException) {
		requireNonNull(validationException);

		if (notes != null && notes.length() > MAXIMUM_NOTES_LENGTH)
			validationException.add(new FieldError("notes", getStrings().get("Notes are too long.")));
	}

	@Nonnull
	protected Database getDatabase() {
		return this.databaseProvider.get();
	}

	@Nonnull
	protected Strings getStrings() {
		return this.strings;
	}

	@Nonnull
	protected AppointmentService getAppointmentService() {
		return this.appointmentService;
	}
}
