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

import com.cobaltplatform.api.model.api.response.AppointmentApiResponse.AppointmentApiResponseFactory;
import com.cobaltplatform.api.model.api.response.AppointmentApiResponse.AppointmentApiResponseSupplement;
import com.cobaltplatform.api.model.db.Appointment;
import com.cobaltplatform.api.model.db.CareEncounter;
import com.cobaltplatform.api.model.db.CareEncounterStatus.CareEncounterStatusId;
import com.cobaltplatform.api.service.AppointmentService;
import com.cobaltplatform.api.util.Formatter;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

@ThreadSafe
public class CareEncounterApiResponse {
	@Nonnull
	private final UUID careEncounterId;
	@Nonnull
	private final UUID appointmentId;
	@Nonnull
	private final UUID accountId;
	@Nonnull
	private final CareEncounterStatusId careEncounterStatusId;
	@Nonnull
	private final String careEncounterStatusDisplayLabel;
	@Nullable
	private final String notes;
	@Nullable
	private final Instant closedAt;
	@Nullable
	private final String closedAtDescription;
	@Nullable
	private final UUID canceledByAccountId;
	@Nonnull
	private final UUID createdByAccountId;
	@Nonnull
	private final UUID lastUpdatedByAccountId;
	@Nonnull
	private final Instant created;
	@Nonnull
	private final String createdDescription;
	@Nonnull
	private final Instant lastUpdated;
	@Nonnull
	private final String lastUpdatedDescription;
	@Nonnull
	private final AppointmentApiResponse appointment;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface CareEncounterApiResponseFactory {
		@Nonnull
		CareEncounterApiResponse create(@Nonnull CareEncounter careEncounter);
	}

	@AssistedInject
	public CareEncounterApiResponse(@Nonnull AppointmentService appointmentService,
																 @Nonnull AppointmentApiResponseFactory appointmentApiResponseFactory,
																 @Nonnull Formatter formatter,
																 @Assisted @Nonnull CareEncounter careEncounter) {
		requireNonNull(appointmentService);
		requireNonNull(appointmentApiResponseFactory);
		requireNonNull(formatter);
		requireNonNull(careEncounter);

		Appointment appointment = appointmentService.findAppointmentById(careEncounter.getAppointmentId()).get();

		this.careEncounterId = careEncounter.getCareEncounterId();
		this.appointmentId = careEncounter.getAppointmentId();
		this.accountId = careEncounter.getAccountId();
		this.careEncounterStatusId = careEncounter.getCareEncounterStatusId();
		this.careEncounterStatusDisplayLabel = careEncounter.getCareEncounterStatusId().getDisplayLabel();
		this.notes = careEncounter.getNotes();
		this.closedAt = careEncounter.getClosedAt();
		this.closedAtDescription = careEncounter.getClosedAt() == null ? null : formatter.formatTimestamp(careEncounter.getClosedAt());
		this.canceledByAccountId = careEncounter.getCanceledByAccountId();
		this.createdByAccountId = careEncounter.getCreatedByAccountId();
		this.lastUpdatedByAccountId = careEncounter.getLastUpdatedByAccountId();
		this.created = careEncounter.getCreated();
		this.createdDescription = formatter.formatTimestamp(careEncounter.getCreated());
		this.lastUpdated = careEncounter.getLastUpdated();
		this.lastUpdatedDescription = formatter.formatTimestamp(careEncounter.getLastUpdated());
		this.appointment = appointmentApiResponseFactory.create(appointment, Set.of(
				AppointmentApiResponseSupplement.ACCOUNT,
				AppointmentApiResponseSupplement.APPOINTMENT_REASON,
				AppointmentApiResponseSupplement.APPOINTMENT_TYPE,
				AppointmentApiResponseSupplement.PRIVATE_DETAILS));
	}

	@Nonnull
	public UUID getCareEncounterId() {
		return this.careEncounterId;
	}

	@Nonnull
	public UUID getAppointmentId() {
		return this.appointmentId;
	}

	@Nonnull
	public UUID getAccountId() {
		return this.accountId;
	}

	@Nonnull
	public CareEncounterStatusId getCareEncounterStatusId() {
		return this.careEncounterStatusId;
	}

	@Nonnull
	public String getCareEncounterStatusDisplayLabel() {
		return this.careEncounterStatusDisplayLabel;
	}

	@Nullable
	public String getNotes() {
		return this.notes;
	}

	@Nullable
	public Instant getClosedAt() {
		return this.closedAt;
	}

	@Nullable
	public String getClosedAtDescription() {
		return this.closedAtDescription;
	}

	@Nullable
	public UUID getCanceledByAccountId() {
		return this.canceledByAccountId;
	}

	@Nonnull
	public UUID getCreatedByAccountId() {
		return this.createdByAccountId;
	}

	@Nonnull
	public UUID getLastUpdatedByAccountId() {
		return this.lastUpdatedByAccountId;
	}

	@Nonnull
	public Instant getCreated() {
		return this.created;
	}

	@Nonnull
	public String getCreatedDescription() {
		return this.createdDescription;
	}

	@Nonnull
	public Instant getLastUpdated() {
		return this.lastUpdated;
	}

	@Nonnull
	public String getLastUpdatedDescription() {
		return this.lastUpdatedDescription;
	}

	@Nonnull
	public AppointmentApiResponse getAppointment() {
		return this.appointment;
	}
}
