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

import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.model.api.response.AppointmentTypeApiResponse.AppointmentTypeApiResponseFactory;
import com.cobaltplatform.api.model.db.AppointmentType;
import com.cobaltplatform.api.model.db.LogicalAvailability;
import com.cobaltplatform.api.model.db.LogicalAvailabilityType.LogicalAvailabilityTypeId;
import com.cobaltplatform.api.model.db.RecurrenceType.RecurrenceTypeId;
import com.cobaltplatform.api.service.AvailabilityService;
import com.cobaltplatform.api.util.Formatter;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lokalized.Strings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Provider;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.FormatStyle;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.lang.String.format;
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
	@Nullable
	private final LocalDate endDate;
	@Nullable
	private final String endDateDescription;
	@Nullable
	private final LocalTime endTime;
	@Nullable
	private final String endTimeDescription;
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
	private List<String> descriptionComponents;
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
																				@Nonnull Provider<CurrentContext> currentContextProvider,
																				@Assisted @Nonnull LogicalAvailability logicalAvailability) {
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(availabilityService);
		requireNonNull(appointmentTypeApiResponseFactory);
		requireNonNull(currentContextProvider);
		requireNonNull(logicalAvailability);

		this.logicalAvailabilityId = logicalAvailability.getLogicalAvailabilityId();
		this.providerId = logicalAvailability.getProviderId();
		this.logicalAvailabilityTypeId = logicalAvailability.getLogicalAvailabilityTypeId();
		this.recurrenceTypeId = logicalAvailability.getRecurrenceTypeId();
		this.startDateTime = logicalAvailability.getStartDateTime();
		this.startDateTimeDescription = formatter.formatDateTime(logicalAvailability.getStartDateTime(), FormatStyle.LONG, FormatStyle.SHORT);
		this.endDate = availabilityService.normalizedEndDate(logicalAvailability).orElse(null);
		this.endDateDescription = endDate == null ? null : formatter.formatDate(endDate, FormatStyle.LONG);
		this.endTime = logicalAvailability.getEndDateTime().toLocalTime();
		this.endTimeDescription = formatter.formatTime(endTime, FormatStyle.SHORT);
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

		List<String> descriptionComponents = new ArrayList<>(4);

		Locale locale = currentContextProvider.get().getLocale();

		if (recurrenceTypeId == RecurrenceTypeId.NONE) {
			// e.g. "9:00 AM - 5:00 PM" / "March 1, 2022"
			descriptionComponents.add(format("%s - %s", formatter.formatTime(getStartDateTime().toLocalTime(), FormatStyle.SHORT),
					formatter.formatTime(getEndTime(), FormatStyle.SHORT)));
			descriptionComponents.add(formatter.formatDate(getStartDateTime().toLocalDate(), FormatStyle.LONG));
		} else if (recurrenceTypeId == RecurrenceTypeId.DAILY) {
			// e.g "9:00 AM - 5:00 PM" / "Every MTWRF" / "Starting March 1, 2022" / "Ending March 31, 2022"
			descriptionComponents.add(format("%s - %s", formatter.formatTime(getStartDateTime().toLocalTime(), FormatStyle.SHORT),
					formatter.formatTime(getEndTime(), FormatStyle.SHORT)));

			List<String> dayAbbreviations = new ArrayList<>(7 /* days in a week */);

			if (getRecurSunday())
				dayAbbreviations.add(DayOfWeek.SUNDAY.getDisplayName(TextStyle.SHORT_STANDALONE, locale));
			if (getRecurMonday())
				dayAbbreviations.add(DayOfWeek.MONDAY.getDisplayName(TextStyle.SHORT_STANDALONE, locale));
			if (getRecurTuesday())
				dayAbbreviations.add(DayOfWeek.TUESDAY.getDisplayName(TextStyle.SHORT_STANDALONE, locale));
			if (getRecurWednesday())
				dayAbbreviations.add(DayOfWeek.WEDNESDAY.getDisplayName(TextStyle.SHORT_STANDALONE, locale));
			if (getRecurThursday())
				dayAbbreviations.add(DayOfWeek.THURSDAY.getDisplayName(TextStyle.SHORT_STANDALONE, locale));
			if (getRecurFriday())
				dayAbbreviations.add(DayOfWeek.FRIDAY.getDisplayName(TextStyle.SHORT_STANDALONE, locale));
			if (getRecurSaturday())
				dayAbbreviations.add(DayOfWeek.SATURDAY.getDisplayName(TextStyle.SHORT_STANDALONE, locale));

			descriptionComponents.add(dayAbbreviations.stream().collect(Collectors.joining(", ")));

			descriptionComponents.add(strings.get("Starting on {{startDate}}", new HashMap<String, Object>() {{
				put("startDate", formatter.formatDate(getStartDateTime().toLocalDate(), FormatStyle.LONG));
			}}));

			if (getEndDate() != null)
				descriptionComponents.add(strings.get("Ending on {{endDate}}", new HashMap<String, Object>() {{
					put("endDate", formatter.formatDate(getEndDate(), FormatStyle.LONG));
				}}));
		} else {
			throw new IllegalStateException(format("Not sure how to handle %s.%s", RecurrenceTypeId.class.getSimpleName(),
					logicalAvailability.getRecurrenceTypeId().name()));
		}

		this.descriptionComponents = descriptionComponents;
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
	public List<String> getDescriptionComponents() {
		return descriptionComponents;
	}

	@Nonnull
	public LocalDateTime getStartDateTime() {
		return startDateTime;
	}

	@Nonnull
	public String getStartDateTimeDescription() {
		return startDateTimeDescription;
	}

	@Nullable
	public LocalDate getEndDate() {
		return endDate;
	}

	@Nullable
	public String getEndDateDescription() {
		return endDateDescription;
	}

	@Nullable
	public LocalTime getEndTime() {
		return endTime;
	}

	@Nullable
	public String getEndTimeDescription() {
		return endTimeDescription;
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