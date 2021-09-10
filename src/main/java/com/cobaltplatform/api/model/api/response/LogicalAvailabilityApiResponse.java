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

package com.cobaltplatform.api.model.api.response;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lokalized.Strings;
import com.cobaltplatform.api.model.api.response.AppointmentTypeApiResponse.AppointmentTypeApiResponseFactory;
import com.cobaltplatform.api.model.db.AppointmentType;
import com.cobaltplatform.api.model.db.LogicalAvailability;
import com.cobaltplatform.api.service.AvailabilityService;
import com.cobaltplatform.api.util.Formatter;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.time.LocalDateTime;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class LogicalAvailabilityApiResponse {
	@Nonnull
	private final UUID logicalAvailabilityId;
	@Nonnull
	private final UUID providerId;
	@Nonnull
	private final LocalDateTime startDateTime;
	@Nonnull
	private final String startDateTimeDescription;
	@Nonnull
	private final LocalDateTime endDateTime;
	@Nonnull
	private final String endDateTimeDescription;
	@Nonnull
	private final List<AppointmentTypeApiResponse> appointmentTypes;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface LogicalAvailabilityApiResponseFactory {
		@Nonnull
		LogicalAvailabilityApiResponse create(@Nonnull LogicalAvailability logicalAvailability);
	}

	@AssistedInject
	public LogicalAvailabilityApiResponse(@Nonnull Formatter formatter,
																				@Nonnull Strings strings,
																				@Nonnull AvailabilityService availabilityService,
																				@Nonnull AppointmentTypeApiResponseFactory appointmentTypeApiResponseFactory,
																				@Assisted @Nonnull LogicalAvailability logicalAvailability) {
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(availabilityService);
		requireNonNull(appointmentTypeApiResponseFactory);
		requireNonNull(logicalAvailability);

		this.logicalAvailabilityId = logicalAvailability.getLogicalAvailabilityId();
		this.providerId = logicalAvailability.getProviderId();
		this.startDateTime = logicalAvailability.getStartDateTime();
		this.startDateTimeDescription = formatter.formatDateTime(logicalAvailability.getStartDateTime(), FormatStyle.LONG, FormatStyle.MEDIUM);
		this.endDateTime = logicalAvailability.getEndDateTime();
		this.endDateTimeDescription = formatter.formatDateTime(logicalAvailability.getEndDateTime(), FormatStyle.LONG, FormatStyle.MEDIUM);

		List<AppointmentType> appointmentTypes = availabilityService.findAppointmentTypesByLogicalAvailabilityId(logicalAvailability.getLogicalAvailabilityId());

		this.appointmentTypes = appointmentTypes.stream()
				.map(appointmentType -> appointmentTypeApiResponseFactory.create(appointmentType))
				.collect(Collectors.toList());
	}

	@Nonnull
	public UUID getLogicalAvailabilityId() {
		return logicalAvailabilityId;
	}

	@Nonnull
	public UUID getProviderId() {
		return providerId;
	}

	@Nonnull
	public LocalDateTime getStartDateTime() {
		return startDateTime;
	}

	@Nonnull
	public LocalDateTime getEndDateTime() {
		return endDateTime;
	}

	@Nonnull
	public List<AppointmentTypeApiResponse> getAppointmentTypes() {
		return appointmentTypes;
	}
}