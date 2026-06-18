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

import com.cobaltplatform.api.Configuration;
import com.cobaltplatform.api.model.api.response.AvailabilityTimeApiResponse.AvailabilityTimeApiResponseFactory;
import com.cobaltplatform.api.model.api.response.SupportRoleApiResponse.SupportRoleApiResponseFactory;
import com.cobaltplatform.api.model.db.AppointmentType;
import com.cobaltplatform.api.model.db.Provider;
import com.cobaltplatform.api.model.db.VideoconferencePlatform.VideoconferencePlatformId;
import com.cobaltplatform.api.model.service.ProviderFind;
import com.cobaltplatform.api.model.service.ProviderFind.AvailabilityDate;
import com.cobaltplatform.api.model.service.ProviderFind.AvailabilityStatus;
import com.cobaltplatform.api.model.service.ProviderFind.AvailabilityTime;
import com.cobaltplatform.api.service.ClinicService;
import com.cobaltplatform.api.service.InstitutionService;
import com.cobaltplatform.api.service.ProviderService;
import com.cobaltplatform.api.util.Formatter;
import com.cobaltplatform.api.util.JsonMapper;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lokalized.Strings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class ProviderListDetailsApiResponse extends ProviderApiResponse {
	@Nullable
	private final String description;
	@Nullable
	private final List<ProviderAppointmentModalityApiResponse> supportedAppointmentModalities;
	@Nullable
	private final ProviderAppointmentSelectionTypeId appointmentSelectionTypeId;
	@Nullable
	private final String appointmentDescription;
	@Nullable
	private final FirstAvailableAppointmentApiResponse firstAvailableAppointment;
	@Nonnull
	private final Boolean hasMoreAppointments;

	public enum ProviderAppointmentModalityId {
		PHONE,
		IN_PERSON,
		VIRTUAL
	}

	public enum ProviderAppointmentSelectionTypeId {
		APPOINTMENT_PREDETERMINED,
		APPOINTMENT_UNDETERMINED,
		APPOINTMENT_BY_PHONE
	}

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface ProviderListDetailsApiResponseFactory {
		@Nonnull
		ProviderListDetailsApiResponse create(@Nonnull Provider provider,
																					@Nonnull ProviderFind providerFind,
																					@Nonnull Map<UUID, AppointmentType> appointmentTypesById);
	}

	@AssistedInject
	public ProviderListDetailsApiResponse(@Nonnull ProviderService providerService,
																					 @Nonnull ClinicService clinicService,
																					 @Nonnull Formatter formatter,
																					 @Nonnull Strings strings,
																					 @Nonnull JsonMapper jsonMapper,
																					 @Nonnull AvailabilityTimeApiResponseFactory availabilityTimeApiResponseFactory,
																					 @Nonnull SupportRoleApiResponseFactory supportRoleApiResponseFactory,
																					 @Nonnull InstitutionService institutionService,
																					 @Nonnull Configuration configuration,
																					 @Assisted @Nonnull Provider provider,
																					 @Assisted @Nonnull ProviderFind providerFind,
																					 @Assisted @Nonnull Map<UUID, AppointmentType> appointmentTypesById) {
		super(providerService, clinicService, formatter, strings, jsonMapper, availabilityTimeApiResponseFactory, supportRoleApiResponseFactory, institutionService, configuration, provider);

		requireNonNull(formatter);
		requireNonNull(institutionService);
		requireNonNull(provider);
		requireNonNull(providerFind);
		requireNonNull(appointmentTypesById);

		boolean bookingV2Enabled = institutionService.isBookingV2Enabled(provider.getInstitutionId());
		List<AvailableAppointment> availableAppointments = availableAppointmentsFor(providerFind, appointmentTypesById);
		AvailableAppointment firstAvailableAppointment = availableAppointments.size() == 0 ? null : availableAppointments.get(0);

		this.description = providerFind.getDescription();
		this.supportedAppointmentModalities = bookingV2Enabled ? supportedAppointmentModalitiesFor(provider, strings) : null;
		this.appointmentSelectionTypeId = appointmentSelectionTypeIdFor(provider, providerFind);
		this.appointmentDescription = appointmentDescriptionFor(providerFind, firstAvailableAppointment, appointmentTypesById);
		this.firstAvailableAppointment = firstAvailableAppointment == null ? null : new FirstAvailableAppointmentApiResponse(firstAvailableAppointment, formatter, provider.getLocale(), bookingV2Enabled);
		this.hasMoreAppointments = availableAppointments.size() > 1;
	}

	@Nonnull
	protected List<ProviderAppointmentModalityApiResponse> supportedAppointmentModalitiesFor(@Nonnull Provider provider,
																																													 @Nonnull Strings strings) {
		requireNonNull(provider);
		requireNonNull(strings);

		List<ProviderAppointmentModalityApiResponse> supportedAppointmentModalities =
				new ArrayList<>(ProviderAppointmentModalitySupport.providerAppointmentModalityIdDisplayOrder().size());
		Set<ProviderAppointmentModalityId> providerAppointmentModalityIds =
				ProviderAppointmentModalitySupport.providerAppointmentModalityIdsFor(provider);

		for (ProviderAppointmentModalityId providerAppointmentModalityId : ProviderAppointmentModalitySupport.providerAppointmentModalityIdDisplayOrder())
			if (providerAppointmentModalityIds.contains(providerAppointmentModalityId))
				supportedAppointmentModalities.add(new ProviderAppointmentModalityApiResponse(providerAppointmentModalityId, switch (providerAppointmentModalityId) {
					case PHONE -> strings.get("Phone");
					case VIRTUAL -> strings.get("Virtual");
					case IN_PERSON -> strings.get("In Person");
				}));

		return supportedAppointmentModalities;
	}

	@Nullable
	protected ProviderAppointmentSelectionTypeId appointmentSelectionTypeIdFor(@Nonnull Provider provider,
																																						 @Nonnull ProviderFind providerFind) {
		requireNonNull(provider);
		requireNonNull(providerFind);

		if (provider.getVideoconferencePlatformId() == VideoconferencePlatformId.TELEPHONE)
			return ProviderAppointmentSelectionTypeId.APPOINTMENT_BY_PHONE;

		int appointmentTypeCount = providerFind.getAppointmentTypeIds() == null ? 0 : providerFind.getAppointmentTypeIds().size();

		return appointmentTypeCount == 1
				? ProviderAppointmentSelectionTypeId.APPOINTMENT_PREDETERMINED
				: ProviderAppointmentSelectionTypeId.APPOINTMENT_UNDETERMINED;
	}

	@Nonnull
	protected List<AvailableAppointment> availableAppointmentsFor(@Nonnull ProviderFind providerFind,
																																@Nonnull Map<UUID, AppointmentType> appointmentTypesById) {
		requireNonNull(providerFind);
		requireNonNull(appointmentTypesById);

		if (providerFind.getDates() == null)
			return List.of();

		List<AvailableAppointment> availableAppointments = new ArrayList<>();

		for (AvailabilityDate availabilityDate : providerFind.getDates()) {
			if (availabilityDate.getDate() == null || availabilityDate.getTimes() == null)
				continue;

			for (AvailabilityTime availabilityTime : availabilityDate.getTimes()) {
				if (availabilityTime.getTime() == null || availabilityTime.getStatus() == AvailabilityStatus.BOOKED)
					continue;

				availableAppointments.add(new AvailableAppointment(availabilityDate.getDate(), availabilityTime,
						appointmentTypeFor(availabilityTime, appointmentTypesById)));
			}
		}

		return availableAppointments;
	}

	@Nullable
	protected AppointmentType appointmentTypeFor(@Nonnull AvailabilityTime availabilityTime,
																							 @Nonnull Map<UUID, AppointmentType> appointmentTypesById) {
		requireNonNull(availabilityTime);
		requireNonNull(appointmentTypesById);

		if (availabilityTime.getAppointmentTypeIds() == null || availabilityTime.getAppointmentTypeIds().size() != 1)
			return null;

		return appointmentTypesById.get(availabilityTime.getAppointmentTypeIds().get(0));
	}

	@Nullable
	protected String appointmentDescriptionFor(@Nonnull ProviderFind providerFind,
																						 @Nullable AvailableAppointment firstAvailableAppointment,
																						 @Nonnull Map<UUID, AppointmentType> appointmentTypesById) {
		requireNonNull(providerFind);
		requireNonNull(appointmentTypesById);

		if (firstAvailableAppointment != null && firstAvailableAppointment.getAppointmentType() != null)
			return descriptionFor(firstAvailableAppointment.getAppointmentType());

		if (providerFind.getAppointmentTypeIds() != null && providerFind.getAppointmentTypeIds().size() == 1) {
			UUID appointmentTypeId = providerFind.getAppointmentTypeIds().iterator().next();
			AppointmentType appointmentType = appointmentTypesById.get(appointmentTypeId);

			if (appointmentType != null)
				return descriptionFor(appointmentType);
		}

		return null;
	}

	@Nullable
	protected String descriptionFor(@Nonnull AppointmentType appointmentType) {
		requireNonNull(appointmentType);

		String description = trimToNull(appointmentType.getDescription());

		return description == null ? trimToNull(appointmentType.getName()) : description;
	}

	@Nullable
	public String getDescription() {
		return description;
	}

	@Nullable
	public List<ProviderAppointmentModalityApiResponse> getSupportedAppointmentModalities() {
		return supportedAppointmentModalities;
	}

	@Nullable
	public ProviderAppointmentSelectionTypeId getAppointmentSelectionTypeId() {
		return appointmentSelectionTypeId;
	}

	@Nullable
	public String getAppointmentDescription() {
		return appointmentDescription;
	}

	@Nullable
	public FirstAvailableAppointmentApiResponse getFirstAvailableAppointment() {
		return firstAvailableAppointment;
	}

	@Nonnull
	public Boolean getHasMoreAppointments() {
		return hasMoreAppointments;
	}

	@ThreadSafe
	public static class ProviderAppointmentModalityApiResponse {
		@Nonnull
		private final ProviderAppointmentModalityId appointmentModalityId;
		@Nonnull
		private final String description;

		public ProviderAppointmentModalityApiResponse(@Nonnull ProviderAppointmentModalityId appointmentModalityId,
																									@Nonnull String description) {
			requireNonNull(appointmentModalityId);
			requireNonNull(description);

			this.appointmentModalityId = appointmentModalityId;
			this.description = description;
		}

		@Nonnull
		public ProviderAppointmentModalityId getAppointmentModalityId() {
			return appointmentModalityId;
		}

		@Nonnull
		public String getDescription() {
			return description;
		}
	}

	@ThreadSafe
	protected static class AvailableAppointment {
		@Nonnull
		private final LocalDate date;
		@Nonnull
		private final AvailabilityTime availabilityTime;
		@Nullable
		private final AppointmentType appointmentType;

		public AvailableAppointment(@Nonnull LocalDate date,
																@Nonnull AvailabilityTime availabilityTime,
																@Nullable AppointmentType appointmentType) {
			requireNonNull(date);
			requireNonNull(availabilityTime);

			this.date = date;
			this.availabilityTime = availabilityTime;
			this.appointmentType = appointmentType;
		}

		@Nonnull
		public LocalDate getDate() {
			return date;
		}

		@Nonnull
		public AvailabilityTime getAvailabilityTime() {
			return availabilityTime;
		}

		@Nullable
		public AppointmentType getAppointmentType() {
			return appointmentType;
		}
	}

	@ThreadSafe
	public static class FirstAvailableAppointmentApiResponse {
		@Nonnull
		private final LocalDate date;
		@Nonnull
		private final LocalTime time;
		@Nonnull
		private final LocalDateTime dateTime;
		@Nonnull
		private final String timeDescription;
		@Nullable
		private final UUID appointmentTypeId;
		@Nullable
		private final List<UUID> appointmentTypeIds;
		@Nullable
		private final String appointmentDescription;
		@Nullable
		private final UUID assessmentId;
		@Nullable
		private final UUID screeningFlowId;
		@Nullable
		private final UUID epicDepartmentId;
		@Nullable
		private final String epicAppointmentFhirId;

		public FirstAvailableAppointmentApiResponse(@Nonnull AvailableAppointment availableAppointment,
																								@Nonnull Formatter formatter,
																								@Nonnull Locale locale,
																								@Nonnull Boolean includeScreeningFlowId) {
			requireNonNull(availableAppointment);
			requireNonNull(formatter);
			requireNonNull(locale);
			requireNonNull(includeScreeningFlowId);

			AvailabilityTime availabilityTime = availableAppointment.getAvailabilityTime();
			AppointmentType appointmentType = availableAppointment.getAppointmentType();

			this.date = availableAppointment.getDate();
			this.time = availabilityTime.getTime();
			this.dateTime = LocalDateTime.of(this.date, this.time);
			this.timeDescription = normalizeTimeFormat(formatter.formatTime(this.time, FormatStyle.SHORT), locale);
			this.appointmentTypeId = appointmentType == null ? null : appointmentType.getAppointmentTypeId();
			this.appointmentTypeIds = availabilityTime.getAppointmentTypeIds();
			this.appointmentDescription = appointmentType == null ? null : descriptionFor(appointmentType);
			this.assessmentId = appointmentType == null ? null : appointmentType.getAssessmentId();
			this.screeningFlowId = includeScreeningFlowId && appointmentType != null ? appointmentType.getScreeningFlowId() : null;
			this.epicDepartmentId = availabilityTime.getEpicDepartmentId();
			this.epicAppointmentFhirId = availabilityTime.getEpicAppointmentFhirId();
		}

		@Nonnull
		protected static String normalizeTimeFormat(@Nonnull String timeDescription,
																								@Nonnull Locale locale) {
			requireNonNull(timeDescription);
			requireNonNull(locale);

			// Turns "10:00 AM" into "10:00am", for example
			return timeDescription.replace(" ", "").toLowerCase(locale);
		}

		@Nullable
		protected static String descriptionFor(@Nonnull AppointmentType appointmentType) {
			requireNonNull(appointmentType);

			String description = trimToNull(appointmentType.getDescription());

			return description == null ? trimToNull(appointmentType.getName()) : description;
		}

		@Nonnull
		public LocalDate getDate() {
			return date;
		}

		@Nonnull
		public LocalTime getTime() {
			return time;
		}

		@Nonnull
		public LocalDateTime getDateTime() {
			return dateTime;
		}

		@Nonnull
		public String getTimeDescription() {
			return timeDescription;
		}

		@Nullable
		public UUID getAppointmentTypeId() {
			return appointmentTypeId;
		}

		@Nullable
		public List<UUID> getAppointmentTypeIds() {
			return appointmentTypeIds;
		}

		@Nullable
		public String getAppointmentDescription() {
			return appointmentDescription;
		}

		@Nullable
		public UUID getAssessmentId() {
			return assessmentId;
		}

		@Nullable
		public UUID getScreeningFlowId() {
			return this.screeningFlowId;
		}

		@Nullable
		public UUID getEpicDepartmentId() {
			return epicDepartmentId;
		}

		@Nullable
		public String getEpicAppointmentFhirId() {
			return epicAppointmentFhirId;
		}
	}
}
