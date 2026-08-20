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
import java.util.UUID;

import static java.util.Objects.requireNonNull;

@ThreadSafe
public class CareEncounterListApiResponse {
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
	private final CareEncounterAppointmentApiResponse appointment;

	@ThreadSafe
	public interface CareEncounterListApiResponseFactory {
		@Nonnull
		CareEncounterListApiResponse create(@Nonnull CareEncounter careEncounter);
	}

	@AssistedInject
	public CareEncounterListApiResponse(@Nonnull CareEncounterService careEncounterService,
																			@Nonnull AccountService accountService,
																			@Nonnull Formatter formatter,
																			@Nonnull javax.inject.Provider<CurrentContext> currentContextProvider,
																			@Assisted @Nonnull CareEncounter careEncounter) {
		requireNonNull(careEncounterService);
		requireNonNull(accountService);
		requireNonNull(formatter);
		requireNonNull(currentContextProvider);
		requireNonNull(careEncounter);

		InstitutionId institutionId = currentContextProvider.get().getInstitutionId();
		Appointment appointmentModel = careEncounterService
				.findLatestAppointmentByCareEncounterIdForInstitutionId(careEncounter.getCareEncounterId(), institutionId)
				.orElseThrow(() -> new IllegalStateException("Care Encounter has no appointments."));

		this.careEncounterId = careEncounter.getCareEncounterId();
		this.appointmentId = appointmentModel.getAppointmentId();
		this.accountId = careEncounter.getAccountId();
		this.careNavigatorAccountId = careEncounter.getCareNavigatorAccountId();
		this.careNavigatorDisplayName = CareEncounterApiResponse
				.displayNameForAccountId(accountService, this.careNavigatorAccountId);
		this.careEncounterStatusId = careEncounter.getCareEncounterStatusId();
		this.careEncounterStatusDisplayLabel = careEncounter.getCareEncounterStatusId().getDisplayLabel();
		this.patientFullName = String.format("%s %s",
				appointmentModel.getFirstName() == null ? "" : appointmentModel.getFirstName(),
				appointmentModel.getLastName() == null ? "" : appointmentModel.getLastName()).trim();
		this.appointmentDate = appointmentModel.getStartTime().toLocalDate();
		this.appointmentDateDescription = formatter.formatDate(this.appointmentDate, FormatStyle.MEDIUM);
		this.notes = careEncounter.getNotes();
		this.closedAt = careEncounter.getClosedAt();
		this.closedAtDescription = careEncounter.getClosedAt() == null
				? null
				: formatter.formatTimestamp(careEncounter.getClosedAt());
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
		this.appointment = new CareEncounterAppointmentApiResponse(formatter, appointmentModel);
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
	@Nonnull public CareEncounterAppointmentApiResponse getAppointment() { return this.appointment; }
}
