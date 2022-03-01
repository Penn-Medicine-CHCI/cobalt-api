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

import com.cobaltplatform.api.model.api.response.AppointmentApiResponse.AppointmentApiResponseFactory;
import com.cobaltplatform.api.model.api.response.AppointmentApiResponse.AppointmentApiResponseSupplement;
import com.cobaltplatform.api.model.api.response.AppointmentTypeApiResponse.AppointmentTypeApiResponseFactory;
import com.cobaltplatform.api.model.api.response.FollowupApiResponse.FollowupApiResponseFactory;
import com.cobaltplatform.api.model.service.ProviderCalendar;
import com.cobaltplatform.api.service.AvailabilityService;
import com.cobaltplatform.api.util.Formatter;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lokalized.Strings;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class ProviderCalendarApiResponse {
	@Nonnull
	private final UUID providerId;
	@Nonnull
	private final List<ProviderCalendarAvailabilityApiResponse> availabilities;
	@Nonnull
	private final List<ProviderCalendarBlockApiResponse> blocks;
	@Nonnull
	private final List<FollowupApiResponse> followups;
	@Nonnull
	private final List<AppointmentApiResponse> appointments;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface ProviderCalendarApiResponseFactory {
		@Nonnull
		ProviderCalendarApiResponse create(@Nonnull ProviderCalendar providerCalendar);
	}

	@AssistedInject
	public ProviderCalendarApiResponse(@Nonnull Formatter formatter,
																		 @Nonnull Strings strings,
																		 @Nonnull AvailabilityService availabilityService,
																		 @Nonnull AppointmentTypeApiResponseFactory appointmentTypeApiResponseFactory,
																		 @Nonnull FollowupApiResponseFactory followupApiResponseFactory,
																		 @Nonnull AppointmentApiResponseFactory appointmentApiResponseFactory,
																		 @Assisted @Nonnull ProviderCalendar providerCalendar) {
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(availabilityService);
		requireNonNull(appointmentTypeApiResponseFactory);
		requireNonNull(followupApiResponseFactory);
		requireNonNull(appointmentApiResponseFactory);
		requireNonNull(providerCalendar);

		this.providerId = providerCalendar.getProviderId();
		this.availabilities = providerCalendar.getAvailabilities() == null ? Collections.emptyList() : providerCalendar.getAvailabilities().stream()
				.map(availability -> new ProviderCalendarAvailabilityApiResponse(availability, appointmentTypeApiResponseFactory, formatter))
				.collect(Collectors.toList());
		this.blocks = providerCalendar.getBlocks() == null ? Collections.emptyList() : providerCalendar.getBlocks().stream()
				.map(block -> new ProviderCalendarBlockApiResponse(block, formatter))
				.collect(Collectors.toList());
		this.followups = providerCalendar.getFollowups() == null ? Collections.emptyList() : providerCalendar.getFollowups().stream()
				.map(followup -> followupApiResponseFactory.create(followup))
				.collect(Collectors.toList());
		this.appointments = providerCalendar.getAppointments() == null ? Collections.emptyList() : providerCalendar.getAppointments().stream()
				.map(appointment -> appointmentApiResponseFactory.create(appointment, Set.of(AppointmentApiResponseSupplement.ACCOUNT)))
				.collect(Collectors.toList());
	}

	@ThreadSafe
	public static class ProviderCalendarAvailabilityApiResponse {
		@Nonnull
		private final UUID logicalAvailabilityId;
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

		public ProviderCalendarAvailabilityApiResponse(@Nonnull ProviderCalendar.Availability availability,
																									 @Nonnull AppointmentTypeApiResponseFactory appointmentTypeApiResponseFactory,
																									 @Nonnull Formatter formatter) {
			requireNonNull(availability);
			requireNonNull(appointmentTypeApiResponseFactory);
			requireNonNull(formatter);

			this.logicalAvailabilityId = availability.getLogicalAvailabilityId();
			this.startDateTime = availability.getStartDateTime();
			this.startDateTimeDescription = getStartDateTime() == null ? null : formatter.formatDateTime(getStartDateTime());
			this.endDateTime = availability.getEndDateTime();
			this.endDateTimeDescription = getEndDateTime() == null ? null : formatter.formatDateTime(getEndDateTime());
			this.appointmentTypes = availability.getAppointmentTypes() == null ? Collections.emptyList() : availability.getAppointmentTypes().stream()
					.map(appointmentType -> appointmentTypeApiResponseFactory.create(appointmentType))
					.collect(Collectors.toList());
		}

		@Nonnull
		public UUID getLogicalAvailabilityId() {
			return logicalAvailabilityId;
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
		public List<AppointmentTypeApiResponse> getAppointmentTypes() {
			return appointmentTypes;
		}
	}

	@ThreadSafe
	public static class ProviderCalendarBlockApiResponse {
		@Nonnull
		private final UUID logicalAvailabilityId;
		@Nonnull
		private final LocalDateTime startDateTime;
		@Nonnull
		private final String startDateTimeDescription;
		@Nonnull
		private final LocalDateTime endDateTime;
		@Nonnull
		private final String endDateTimeDescription;

		public ProviderCalendarBlockApiResponse(@Nonnull ProviderCalendar.Block block,
																						@Nonnull Formatter formatter) {
			requireNonNull(block);
			requireNonNull(formatter);

			this.logicalAvailabilityId = block.getLogicalAvailabilityId();
			this.startDateTime = block.getStartDateTime();
			this.startDateTimeDescription = getStartDateTime() == null ? null : formatter.formatDateTime(getStartDateTime());
			this.endDateTime = block.getEndDateTime();
			this.endDateTimeDescription = getEndDateTime() == null ? null : formatter.formatDateTime(getEndDateTime());
		}

		@Nonnull
		public UUID getLogicalAvailabilityId() {
			return logicalAvailabilityId;
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
	}

	@Nonnull
	public UUID getProviderId() {
		return providerId;
	}

	@Nonnull
	public List<ProviderCalendarAvailabilityApiResponse> getAvailabilities() {
		return availabilities;
	}

	@Nonnull
	public List<ProviderCalendarBlockApiResponse> getBlocks() {
		return blocks;
	}

	@Nonnull
	public List<FollowupApiResponse> getFollowups() {
		return followups;
	}

	@Nonnull
	public List<AppointmentApiResponse> getAppointments() {
		return appointments;
	}
}