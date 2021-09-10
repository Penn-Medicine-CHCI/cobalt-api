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
import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.model.api.response.AppointmentApiResponse.AppointmentApiResponseFactory;
import com.cobaltplatform.api.model.api.response.ProviderApiResponse.ProviderApiResponseFactory;
import com.cobaltplatform.api.model.db.Appointment;
import com.cobaltplatform.api.model.service.GroupEvent;
import com.cobaltplatform.api.util.AppointmentTimeFormatter;
import com.cobaltplatform.api.util.Formatter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.HashMap;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
@Deprecated
public class GroupEventApiResponse {
	@Nonnull
	private final String groupEventId;
	@Nonnull
	private final UUID groupEventTypeId;
	@Nonnull
	private final String name;
	@Nullable
	private final String description;
	@Nonnull
	private final Instant startTime;
	@Nonnull
	private final String startTimeDescription;
	@Nonnull
	private final LocalDate localStartDate;
	@Nonnull
	private final LocalTime localStartTime;
	@Nonnull
	private final Instant endTime;
	@Nonnull
	private final String endTimeDescription;
	@Nonnull
	private final LocalDate localEndDate;
	@Nonnull
	private final LocalTime localEndTime;
	@Nonnull
	private final Long durationInMinutes;
	@Nonnull
	private final String durationInMinutesDescription;
	@Nullable
	private String timeDescription;
	@Nonnull
	private final ZoneId timeZone;
	@Nonnull
	private final Long seats;
	@Nonnull
	private final String seatsDescription;
	@Nonnull
	private final Long seatsAvailable;
	@Nonnull
	private final String seatsAvailableDescription;
	@Nonnull
	private final String imageUrl;
	@Nonnull
	private final Boolean isDefaultImageUrl;
	@Nonnull
	private final ProviderApiResponse provider;
	@Nullable
	private final AppointmentApiResponse appointment;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	@Deprecated
	public interface GroupEventApiResponseFactory {
		@Nonnull
		GroupEventApiResponse create(@Nonnull GroupEvent groupEvent,
																 @Nullable Appointment appointment);
	}

	@AssistedInject
	public GroupEventApiResponse(@Nonnull Formatter formatter,
															 @Nonnull Strings strings,
															 @Nonnull javax.inject.Provider<CurrentContext> currentContextProvider,
															 @Nonnull ProviderApiResponseFactory providerApiResponseFactory,
															 @Nonnull AppointmentApiResponseFactory appointmentApiResponseFactory,
															 @Assisted @Nonnull GroupEvent groupEvent,
															 @Assisted @Nullable Appointment appointment) {
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(currentContextProvider);
		requireNonNull(providerApiResponseFactory);
		requireNonNull(appointmentApiResponseFactory);
		requireNonNull(groupEvent);

		LocalDateTime localStartDateTime = groupEvent.getStartTime().atZone(groupEvent.getTimeZone()).toLocalDateTime();
		LocalDateTime localEndDateTime = groupEvent.getEndTime().atZone(groupEvent.getTimeZone()).toLocalDateTime();

		this.groupEventId = groupEvent.getGroupEventId();
		this.groupEventTypeId = groupEvent.getGroupEventTypeId();
		this.name = groupEvent.getName();
		this.description = groupEvent.getDescription();
		this.startTime = groupEvent.getStartTime();
		this.startTimeDescription = formatter.formatTimestamp(groupEvent.getStartTime());
		this.localStartDate = localStartDateTime.toLocalDate();
		this.localStartTime = localStartDateTime.toLocalTime();
		this.endTime = groupEvent.getEndTime();
		this.endTimeDescription = formatter.formatTimestamp(groupEvent.getEndTime());
		this.localEndDate = localEndDateTime.toLocalDate();
		this.localEndTime = localEndDateTime.toLocalTime();
		this.durationInMinutes = groupEvent.getDurationInMinutes();
		this.durationInMinutesDescription = strings.get("{{duration}} minutes", new HashMap<String, Object>() {{
			put("duration", groupEvent.getDurationInMinutes());
		}});
		this.timeDescription = AppointmentTimeFormatter.createTimeDescription(groupEvent.getStartTime(), groupEvent.getEndTime(), groupEvent.getTimeZone());
		this.seats = groupEvent.getSeats();
		this.seatsDescription = strings.get("{{seatsDescription}} seats", new HashMap<String, Object>() {{
			put("seats", groupEvent.getSeats());
			put("seatsDescription", formatter.formatNumber(groupEvent.getSeats()));
		}});
		this.seatsAvailable = groupEvent.getSeatsAvailable();
		this.seatsAvailableDescription = strings.get("{{seatsAvailableDescription}} seats left", new HashMap<String, Object>() {{
			put("seatsAvailable", groupEvent.getSeatsAvailable());
			put("seatsAvailableDescription", formatter.formatNumber(groupEvent.getSeatsAvailable()));
		}});
		this.timeZone = groupEvent.getTimeZone();
		this.imageUrl = groupEvent.getImageUrl();
		this.isDefaultImageUrl = groupEvent.getDefaultImageUrl();

		this.provider = groupEvent.getProvider() == null ? null : providerApiResponseFactory.create(groupEvent.getProvider());

		if (appointment != null)
			this.appointment = appointmentApiResponseFactory.create(appointment, Collections.emptySet());
		else
			this.appointment = null;
	}

	@Nonnull
	public String getGroupEventId() {
		return groupEventId;
	}

	@Nonnull
	public UUID getGroupEventTypeId() {
		return groupEventTypeId;
	}

	@Nonnull
	public String getName() {
		return name;
	}

	@Nullable
	public String getDescription() {
		return description;
	}

	@Nonnull
	public Instant getStartTime() {
		return startTime;
	}

	@Nonnull
	public String getStartTimeDescription() {
		return startTimeDescription;
	}

	@Nonnull
	public Instant getEndTime() {
		return endTime;
	}

	@Nonnull
	public String getEndTimeDescription() {
		return endTimeDescription;
	}

	@Nonnull
	public Long getDurationInMinutes() {
		return durationInMinutes;
	}

	@Nonnull
	public String getDurationInMinutesDescription() {
		return durationInMinutesDescription;
	}

	@Nullable
	public String getTimeDescription() {
		return timeDescription;
	}

	@Nonnull
	public ZoneId getTimeZone() {
		return timeZone;
	}

	@Nonnull
	public Long getSeats() {
		return seats;
	}

	@Nonnull
	public String getSeatsDescription() {
		return seatsDescription;
	}

	@Nonnull
	public Long getSeatsAvailable() {
		return seatsAvailable;
	}

	@Nonnull
	public String getSeatsAvailableDescription() {
		return seatsAvailableDescription;
	}

	@Nonnull
	public String getImageUrl() {
		return imageUrl;
	}

	@Nonnull
	public Boolean getDefaultImageUrl() {
		return isDefaultImageUrl;
	}

	@Nonnull
	public ProviderApiResponse getProvider() {
		return provider;
	}

	@Nullable
	public AppointmentApiResponse getAppointment() {
		return appointment;
	}

	@Nonnull
	public LocalDate getLocalStartDate() {
		return localStartDate;
	}

	@Nonnull
	public LocalTime getLocalStartTime() {
		return localStartTime;
	}

	@Nonnull
	public LocalDate getLocalEndDate() {
		return localEndDate;
	}

	@Nonnull
	public LocalTime getLocalEndTime() {
		return localEndTime;
	}
}
