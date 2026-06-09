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
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class ProviderSearchScreeningRequirement {
	@Nonnull
	private final UUID providerId;
	@Nonnull
	private final UUID appointmentTypeId;
	@Nullable
	private final String appointmentTypeName;
	@Nullable
	private final String appointmentDescription;
	@Nullable
	private final UUID screeningFlowId;
	@Nonnull
	private final Boolean screeningRequired;
	@Nonnull
	private final Boolean screeningSatisfied;

	public ProviderSearchScreeningRequirement(@Nonnull UUID providerId,
																						@Nonnull UUID appointmentTypeId,
																						@Nullable String appointmentTypeName,
																						@Nullable String appointmentDescription,
																						@Nullable UUID screeningFlowId,
																						@Nonnull Boolean screeningRequired,
																						@Nonnull Boolean screeningSatisfied) {
		requireNonNull(providerId);
		requireNonNull(appointmentTypeId);
		requireNonNull(screeningRequired);
		requireNonNull(screeningSatisfied);

		this.providerId = providerId;
		this.appointmentTypeId = appointmentTypeId;
		this.appointmentTypeName = appointmentTypeName;
		this.appointmentDescription = appointmentDescription;
		this.screeningFlowId = screeningFlowId;
		this.screeningRequired = screeningRequired;
		this.screeningSatisfied = screeningSatisfied;
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
	public String getAppointmentTypeName() {
		return this.appointmentTypeName;
	}

	@Nullable
	public String getAppointmentDescription() {
		return this.appointmentDescription;
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
}
