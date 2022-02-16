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

import com.cobaltplatform.api.model.db.LogicalAvailabilityType;
import com.cobaltplatform.api.model.db.LogicalAvailabilityType.LogicalAvailabilityTypeId;
import com.cobaltplatform.api.model.db.RecurrenceType;
import com.cobaltplatform.api.model.db.RecurrenceType.RecurrenceTypeId;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lokalized.Strings;
import com.cobaltplatform.api.model.api.response.AppointmentTypeApiResponse.AppointmentTypeApiResponseFactory;
import com.cobaltplatform.api.model.db.AppointmentType;
import com.cobaltplatform.api.model.db.LogicalAvailability;
import com.cobaltplatform.api.service.AvailabilityService;
import com.cobaltplatform.api.util.Formatter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
	private final LogicalAvailabilityTypeId logicalAvailabilityTypeId;
	@Nonnull
	private final RecurrenceTypeId recurrenceTypeId;
	@Nonnull
	private final LocalDateTime startDateTime;
	@Nonnull
	private final String startDateTimeDescription;
	@Nonnull
	private final LocalDateTime endDateTime;
	@Nonnull
	private final String endDateTimeDescription;
	@Nonnull
	private final Boolean recurSunday;
	@Nonnull
	private final Boolean recurMonday;
	@Nonnull
	private final Boolean recurTuesday;
	@Nonnull
	private final Boolean recurWednesday;
	@Nonnull
	private final Boolean recurThursday;
	@Nonnull
	private final Boolean recurFriday;
	@Nonnull
	private final Boolean recurSaturday;
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
		this.logicalAvailabilityTypeId = logicalAvailability.getLogicalAvailabilityTypeId();
		this.recurrenceTypeId = logicalAvailability.getRecurrenceTypeId();
		this.startDateTime = logicalAvailability.getStartDateTime();
		this.startDateTimeDescription = formatter.formatDateTime(logicalAvailability.getStartDateTime(), FormatStyle.LONG, FormatStyle.MEDIUM);
		this.endDateTime = logicalAvailability.getEndDateTime();
		this.endDateTimeDescription = formatter.formatDateTime(logicalAvailability.getEndDateTime(), FormatStyle.LONG, FormatStyle.MEDIUM);
		this.recurSunday = logicalAvailability.getRecurSunday();
		this.recurMonday = logicalAvailability.getRecurMonday();
		this.recurTuesday = logicalAvailability.getRecurTuesday();
		this.recurWednesday = logicalAvailability.getRecurWednesday();
		this.recurThursday = logicalAvailability.getRecurThursday();
		this.recurFriday = logicalAvailability.getRecurFriday();
		this.recurSaturday = logicalAvailability.getRecurSaturday();

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
	public LogicalAvailabilityTypeId getLogicalAvailabilityTypeId() {
		return logicalAvailabilityTypeId;
	}

	@Nonnull
	public RecurrenceTypeId getRecurrenceTypeId() {
		return recurrenceTypeId;
	}

	@Nonnull
	public LocalDateTime getStartDateTime() {
		return startDateTime;
	}

	@Nonnull
	public String getStartDateTimeDescription() {
		return startDateTimeDescription;
	}

	@Nonnull
	public LocalDateTime getEndDateTime() {
		return endDateTime;
	}

	@Nonnull
	public String getEndDateTimeDescription() {
		return endDateTimeDescription;
	}

	@Nonnull
	public Boolean getRecurSunday() {
		return recurSunday;
	}

	@Nonnull
	public Boolean getRecurMonday() {
		return recurMonday;
	}

	@Nonnull
	public Boolean getRecurTuesday() {
		return recurTuesday;
	}

	@Nonnull
	public Boolean getRecurWednesday() {
		return recurWednesday;
	}

	@Nonnull
	public Boolean getRecurThursday() {
		return recurThursday;
	}

	@Nonnull
	public Boolean getRecurFriday() {
		return recurFriday;
	}

	@Nonnull
	public Boolean getRecurSaturday() {
		return recurSaturday;
	}

	@Nonnull
	public List<AppointmentTypeApiResponse> getAppointmentTypes() {
		return appointmentTypes;
	}
}