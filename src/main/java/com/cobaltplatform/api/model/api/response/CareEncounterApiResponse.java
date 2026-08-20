/*
 * Copyright 2021 The University of Pennsylvania and Penn Medicine
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package com.cobaltplatform.api.model.api.response;

import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.model.api.response.AppointmentApiResponse.AppointmentApiResponseFactory;
import com.cobaltplatform.api.model.api.response.AppointmentApiResponse.AppointmentApiResponseSupplement;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.Appointment;
import com.cobaltplatform.api.model.db.CareEncounter;
import com.cobaltplatform.api.model.db.CareEncounterCancellationReason.CareEncounterCancellationReasonId;
import com.cobaltplatform.api.model.db.CareEncounterStatus.CareEncounterStatusId;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.service.AccountService;
import com.cobaltplatform.api.service.CareEncounterService;
import com.cobaltplatform.api.util.Formatter;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

@ThreadSafe
public class CareEncounterApiResponse {
	@Nonnull
	private final UUID careEncounterId;
	@Nonnull
	private final UUID appointmentId;
	@Nonnull
	private final UUID accountId;
	@Nullable
	private final UUID careNavigatorAccountId;
	@Nullable
	private final String careNavigatorDisplayName;
	@Nonnull
	private final CareEncounterStatusId careEncounterStatusId;
	@Nonnull
	private final String careEncounterStatusDisplayLabel;
	@Nonnull
	private final String patientFullName;
	@Nonnull
	private final LocalDate appointmentDate;
	@Nonnull
	private final String appointmentDateDescription;
	@Nullable
	private final String emailAddress;
	@Nullable
	private final String notes;
	@Nullable
	private final Instant closedAt;
	@Nullable
	private final String closedAtDescription;
	@Nullable
	private final UUID closedByAccountId;
	@Nullable
	private final UUID canceledByAccountId;
	@Nullable
	private final CareEncounterCancellationReasonId careEncounterCancellationReasonId;
	@Nullable
	private final String careEncounterCancellationReasonOtherText;
	@Nonnull
	private final UUID createdByAccountId;
	@Nonnull
	private final UUID lastUpdatedByAccountId;
	@Nonnull
	private final Instant created;
	@Nonnull
	private final String createdDescription;
	@Nonnull
	private final LocalDate createdDate;
	@Nonnull
	private final String createdDateDescription;
	@Nonnull
	private final Instant lastUpdated;
	@Nonnull
	private final String lastUpdatedDescription;
	@Nonnull
	private final AppointmentApiResponse appointment;
	@Nonnull
	private final List<AppointmentApiResponse> appointmentHistory;

	@ThreadSafe
	public interface CareEncounterApiResponseFactory {
		@Nonnull
		CareEncounterApiResponse create(@Nonnull CareEncounter careEncounter);
	}

	@AssistedInject
	public CareEncounterApiResponse(@Nonnull CareEncounterService careEncounterService,
																 @Nonnull AccountService accountService,
																 @Nonnull AppointmentApiResponseFactory appointmentApiResponseFactory,
																 @Nonnull Formatter formatter,
																 @Nonnull javax.inject.Provider<CurrentContext> currentContextProvider,
																 @Assisted @Nonnull CareEncounter careEncounter) {
		requireNonNull(careEncounterService);
		requireNonNull(accountService);
		requireNonNull(appointmentApiResponseFactory);
		requireNonNull(formatter);
		requireNonNull(currentContextProvider);
		requireNonNull(careEncounter);

		InstitutionId institutionId = currentContextProvider.get().getInstitutionId();
		List<Appointment> appointmentModels = careEncounterService
				.findAppointmentsByCareEncounterIdForInstitutionId(careEncounter.getCareEncounterId(), institutionId);

		if (appointmentModels.isEmpty())
			throw new IllegalStateException("Care Encounter has no appointments.");

		Appointment latestAppointment = appointmentModels.get(0);
		Set<AppointmentApiResponseSupplement> supplements = Set.of(
				AppointmentApiResponseSupplement.ACCOUNT,
				AppointmentApiResponseSupplement.APPOINTMENT_REASON,
				AppointmentApiResponseSupplement.APPOINTMENT_TYPE,
				AppointmentApiResponseSupplement.PRIVATE_DETAILS,
				AppointmentApiResponseSupplement.SCREENING_SESSION_RESULT);

		this.careEncounterId = careEncounter.getCareEncounterId();
		this.appointmentId = latestAppointment.getAppointmentId();
		this.accountId = careEncounter.getAccountId();
		this.careNavigatorAccountId = careEncounter.getCareNavigatorAccountId();
		this.careNavigatorDisplayName = displayNameForAccountId(accountService, this.careNavigatorAccountId);
		this.careEncounterStatusId = careEncounter.getCareEncounterStatusId();
		this.careEncounterStatusDisplayLabel = careEncounter.getCareEncounterStatusId().getDisplayLabel();
		this.patientFullName = String.format("%s %s",
				latestAppointment.getFirstName() == null ? "" : latestAppointment.getFirstName(),
				latestAppointment.getLastName() == null ? "" : latestAppointment.getLastName()).trim();
		this.appointmentDate = latestAppointment.getStartTime().toLocalDate();
		this.appointmentDateDescription = formatter.formatDate(this.appointmentDate, FormatStyle.MEDIUM);
		this.emailAddress = careEncounter.getEmailAddress();
		this.notes = careEncounter.getNotes();
		this.closedAt = careEncounter.getClosedAt();
		this.closedAtDescription = careEncounter.getClosedAt() == null ? null : formatter.formatTimestamp(careEncounter.getClosedAt());
		this.closedByAccountId = careEncounter.getClosedByAccountId();
		this.canceledByAccountId = careEncounter.getCanceledByAccountId();
		this.careEncounterCancellationReasonId = careEncounter.getCareEncounterCancellationReasonId();
		this.careEncounterCancellationReasonOtherText = careEncounter.getCareEncounterCancellationReasonOtherText();
		this.createdByAccountId = careEncounter.getCreatedByAccountId();
		this.lastUpdatedByAccountId = careEncounter.getLastUpdatedByAccountId();
		this.created = careEncounter.getCreated();
		this.createdDescription = formatter.formatTimestamp(careEncounter.getCreated());
		this.createdDate = LocalDate.ofInstant(careEncounter.getCreated(), currentContextProvider.get().getTimeZone());
		this.createdDateDescription = formatter.formatDate(this.createdDate, FormatStyle.MEDIUM);
		this.lastUpdated = careEncounter.getLastUpdated();
		this.lastUpdatedDescription = formatter.formatTimestamp(careEncounter.getLastUpdated());
		this.appointment = appointmentApiResponseFactory.create(latestAppointment, supplements);
		this.appointmentHistory = appointmentModels.stream()
				.skip(1)
				.map(appointmentModel -> appointmentApiResponseFactory.create(appointmentModel, supplements))
				.collect(Collectors.toUnmodifiableList());
	}

	@Nullable
	protected static String displayNameForAccountId(@Nonnull AccountService accountService,
																							 @Nullable UUID accountId) {
		if (accountId == null)
			return null;

		Account account = accountService.findAccountById(accountId).orElse(null);
		if (account == null)
			return null;

		String displayName = trimToNull(account.getDisplayName());
		return displayName == null
				? trimToNull(String.format("%s %s",
				account.getFirstName() == null ? "" : account.getFirstName(),
				account.getLastName() == null ? "" : account.getLastName()))
				: displayName;
	}

	@Nonnull public UUID getCareEncounterId() { return this.careEncounterId; }
	@Nonnull public UUID getAppointmentId() { return this.appointmentId; }
	@Nonnull public UUID getAccountId() { return this.accountId; }
	@Nullable public UUID getCareNavigatorAccountId() { return this.careNavigatorAccountId; }
	@Nullable public String getCareNavigatorDisplayName() { return this.careNavigatorDisplayName; }
	@Nonnull public CareEncounterStatusId getCareEncounterStatusId() { return this.careEncounterStatusId; }
	@Nonnull public String getCareEncounterStatusDisplayLabel() { return this.careEncounterStatusDisplayLabel; }
	@Nonnull public String getPatientFullName() { return this.patientFullName; }
	@Nonnull public LocalDate getAppointmentDate() { return this.appointmentDate; }
	@Nonnull public String getAppointmentDateDescription() { return this.appointmentDateDescription; }
	@Nullable public String getEmailAddress() { return this.emailAddress; }
	@Nullable public String getNotes() { return this.notes; }
	@Nullable public Instant getClosedAt() { return this.closedAt; }
	@Nullable public String getClosedAtDescription() { return this.closedAtDescription; }
	@Nullable public UUID getClosedByAccountId() { return this.closedByAccountId; }
	@Nullable public UUID getCanceledByAccountId() { return this.canceledByAccountId; }
	@Nullable public CareEncounterCancellationReasonId getCareEncounterCancellationReasonId() { return this.careEncounterCancellationReasonId; }
	@Nullable public String getCareEncounterCancellationReasonOtherText() { return this.careEncounterCancellationReasonOtherText; }
	@Nonnull public UUID getCreatedByAccountId() { return this.createdByAccountId; }
	@Nonnull public UUID getLastUpdatedByAccountId() { return this.lastUpdatedByAccountId; }
	@Nonnull public Instant getCreated() { return this.created; }
	@Nonnull public String getCreatedDescription() { return this.createdDescription; }
	@Nonnull public LocalDate getCreatedDate() { return this.createdDate; }
	@Nonnull public String getCreatedDateDescription() { return this.createdDateDescription; }
	@Nonnull public Instant getLastUpdated() { return this.lastUpdated; }
	@Nonnull public String getLastUpdatedDescription() { return this.lastUpdatedDescription; }
	@Nonnull public AppointmentApiResponse getAppointment() { return this.appointment; }
	@Nonnull public List<AppointmentApiResponse> getAppointmentHistory() { return this.appointmentHistory; }
}
