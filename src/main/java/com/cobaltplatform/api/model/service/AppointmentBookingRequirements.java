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

package com.cobaltplatform.api.model.service;

import com.cobaltplatform.api.model.api.response.ProviderListDetailsApiResponse.ProviderAppointmentSelectionTypeId;
import com.cobaltplatform.api.model.db.ScreeningSession;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Map;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class AppointmentBookingRequirements {
	@Nonnull
	private final AppointmentBookingRequirementsDestinationId appointmentBookingRequirementsDestinationId;
	@Nonnull
	private final UUID accountId;
	@Nonnull
	private final UUID providerId;
	@Nonnull
	private final UUID appointmentTypeId;
	@Nullable
	private final ProviderAppointmentSelectionTypeId appointmentSelectionTypeId;
	@Nullable
	private final UUID screeningFlowId;
	@Nonnull
	private final Boolean screeningRequired;
	@Nonnull
	private final Boolean screeningSatisfied;
	@Nullable
	private final ScreeningSession screeningSession;
	@Nonnull
	private final Map<String, Object> context;

	public enum AppointmentBookingRequirementsDestinationId {
		APPOINTMENT_BOOKING,
		SCREENING_SESSION
	}

	public AppointmentBookingRequirements(@Nonnull AppointmentBookingRequirementsDestinationId appointmentBookingRequirementsDestinationId,
																				@Nonnull UUID accountId,
																				@Nonnull UUID providerId,
																				@Nonnull UUID appointmentTypeId,
																				@Nullable ProviderAppointmentSelectionTypeId appointmentSelectionTypeId,
																				@Nullable UUID screeningFlowId,
																				@Nonnull Boolean screeningRequired,
																				@Nonnull Boolean screeningSatisfied,
																				@Nullable ScreeningSession screeningSession,
																				@Nonnull Map<String, Object> context) {
		requireNonNull(appointmentBookingRequirementsDestinationId);
		requireNonNull(accountId);
		requireNonNull(providerId);
		requireNonNull(appointmentTypeId);
		requireNonNull(screeningRequired);
		requireNonNull(screeningSatisfied);
		requireNonNull(context);

		this.appointmentBookingRequirementsDestinationId = appointmentBookingRequirementsDestinationId;
		this.accountId = accountId;
		this.providerId = providerId;
		this.appointmentTypeId = appointmentTypeId;
		this.appointmentSelectionTypeId = appointmentSelectionTypeId;
		this.screeningFlowId = screeningFlowId;
		this.screeningRequired = screeningRequired;
		this.screeningSatisfied = screeningSatisfied;
		this.screeningSession = screeningSession;
		this.context = Map.copyOf(context);
	}

	@Nonnull
	public AppointmentBookingRequirementsDestinationId getAppointmentBookingRequirementsDestinationId() {
		return this.appointmentBookingRequirementsDestinationId;
	}

	@Nonnull
	public UUID getAccountId() {
		return this.accountId;
	}

	@Nonnull
	public UUID getProviderId() {
		return this.providerId;
	}

	@Nonnull
	public UUID getAppointmentTypeId() {
		return this.appointmentTypeId;
	}

	@Nullable
	public ProviderAppointmentSelectionTypeId getAppointmentSelectionTypeId() {
		return this.appointmentSelectionTypeId;
	}

	@Nullable
	public UUID getScreeningFlowId() {
		return this.screeningFlowId;
	}

	@Nonnull
	public Boolean getScreeningRequired() {
		return this.screeningRequired;
	}

	@Nonnull
	public Boolean getScreeningSatisfied() {
		return this.screeningSatisfied;
	}

	@Nullable
	public ScreeningSession getScreeningSession() {
		return this.screeningSession;
	}

	@Nonnull
	public Map<String, Object> getContext() {
		return this.context;
	}
}
