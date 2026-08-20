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

import com.cobaltplatform.api.model.api.request.CancelCareEncounterRequest;
import com.cobaltplatform.api.model.api.request.CreateCareEncounterRequest;
import com.cobaltplatform.api.model.api.request.FindCareEncountersRequest;
import com.cobaltplatform.api.model.api.request.FindCareEncountersRequest.CareEncounterAssignmentScopeId;
import com.cobaltplatform.api.model.api.request.FindCareEncountersRequest.CareEncounterSortColumnId;
import com.cobaltplatform.api.model.api.request.UpdateCareEncounterRequest;
import com.cobaltplatform.api.model.db.Appointment;
import com.cobaltplatform.api.model.db.CareEncounter;
import com.cobaltplatform.api.model.db.CareEncounterCancellationReason;
import com.cobaltplatform.api.model.db.CareEncounterCancellationReason.CareEncounterCancellationReasonId;
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
	protected static final int MAXIMUM_CANCELLATION_REASON_OTHER_TEXT_LENGTH = 2_000;

	@Nonnull
	private final DatabaseProvider databaseProvider;
	@Nonnull
	private final Strings strings;

	@Inject
	public CareEncounterService(@Nonnull DatabaseProvider databaseProvider,
														@Nonnull Strings strings) {
		requireNonNull(databaseProvider);
		requireNonNull(strings);

		this.databaseProvider = databaseProvider;
		this.strings = strings;
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
	public List<CareEncounter> findOtherCareEncountersByAccountId(@Nullable UUID accountId,
																	 @Nullable UUID excludedCareEncounterId,
																	 @Nullable InstitutionId institutionId) {
		if (accountId == null || excludedCareEncounterId == null || institutionId == null)
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

		getDatabase().execute("""
				UPDATE care_encounter
				SET notes=?, last_updated_by_account_id=?
				WHERE care_encounter_id=?
				""", notes, accountId, careEncounter.getCareEncounterId());

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

}
