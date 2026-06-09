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

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Objects;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class AppointmentBookingScreeningKey {
	@Nonnull
	private final UUID providerId;
	@Nonnull
	private final UUID appointmentTypeId;
	@Nonnull
	private final UUID screeningFlowId;

	public AppointmentBookingScreeningKey(@Nonnull UUID providerId,
																				@Nonnull UUID appointmentTypeId,
																				@Nonnull UUID screeningFlowId) {
		requireNonNull(providerId);
		requireNonNull(appointmentTypeId);
		requireNonNull(screeningFlowId);

		this.providerId = providerId;
		this.appointmentTypeId = appointmentTypeId;
		this.screeningFlowId = screeningFlowId;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other)
			return true;

		if (other == null || !getClass().equals(other.getClass()))
			return false;

		AppointmentBookingScreeningKey otherAppointmentBookingScreeningKey = (AppointmentBookingScreeningKey) other;
		return Objects.equals(getProviderId(), otherAppointmentBookingScreeningKey.getProviderId())
				&& Objects.equals(getAppointmentTypeId(), otherAppointmentBookingScreeningKey.getAppointmentTypeId())
				&& Objects.equals(getScreeningFlowId(), otherAppointmentBookingScreeningKey.getScreeningFlowId());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getProviderId(), getAppointmentTypeId(), getScreeningFlowId());
	}

	@Nonnull
	public UUID getProviderId() {
		return this.providerId;
	}

	@Nonnull
	public UUID getAppointmentTypeId() {
		return this.appointmentTypeId;
	}

	@Nonnull
	public UUID getScreeningFlowId() {
		return this.screeningFlowId;
	}
}
