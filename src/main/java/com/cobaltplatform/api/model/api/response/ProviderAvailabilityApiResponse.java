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
import com.cobaltplatform.api.model.db.AppointmentType;
import com.cobaltplatform.api.model.db.Clinic;
import com.cobaltplatform.api.model.db.Provider;
import com.cobaltplatform.api.model.db.SchedulingSystem.SchedulingSystemId;
import com.cobaltplatform.api.model.db.VisitType.VisitTypeId;
import com.cobaltplatform.api.model.service.ProviderFind;
import com.cobaltplatform.api.model.service.ProviderFind.AvailabilityDate;
import com.cobaltplatform.api.model.service.ProviderFind.AvailabilityStatus;
import com.cobaltplatform.api.model.service.ProviderFind.AvailabilityTime;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class ProviderAvailabilityApiResponse {
	@Nullable
	private final UUID providerId;
	@Nullable
	private final String providerName;
	@Nullable
	private final UUID clinicId;
	@Nullable
	private final String clinicDescription;
	@Nonnull
	private final LocalDate startDate;
	@Nonnull
	private final LocalDate endDate;
	@Nonnull
	private final List<AppointmentTypeAvailabilityApiResponse> appointmentTypes;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface ProviderAvailabilityApiResponseFactory {
		@Nonnull
		ProviderAvailabilityApiResponse create(@Nonnull Provider provider,
																					 @Nonnull List<ProviderFind> providerFinds,
																					 @Assisted("appointmentTypesById") @Nonnull Map<UUID, AppointmentType> appointmentTypesById,
																					 @Assisted("startDate") @Nonnull LocalDate startDate,
																					 @Assisted("endDate") @Nonnull LocalDate endDate);

		@Nonnull
		ProviderAvailabilityApiResponse create(@Nonnull Clinic clinic,
																					 @Nonnull List<ProviderFind> providerFinds,
																					 @Assisted("providersById") @Nonnull Map<UUID, Provider> providersById,
																					 @Assisted("appointmentTypesById") @Nonnull Map<UUID, AppointmentType> appointmentTypesById,
																					 @Assisted("startDate") @Nonnull LocalDate startDate,
																					 @Assisted("endDate") @Nonnull LocalDate endDate);
	}

	@AssistedInject
	public ProviderAvailabilityApiResponse(@Nonnull javax.inject.Provider<CurrentContext> currentContextProvider,
																				 @Assisted @Nonnull Provider provider,
																				 @Assisted @Nonnull List<ProviderFind> providerFinds,
																				 @Assisted("appointmentTypesById") @Nonnull Map<UUID, AppointmentType> appointmentTypesById,
																				 @Assisted("startDate") @Nonnull LocalDate startDate,
																				 @Assisted("endDate") @Nonnull LocalDate endDate) {
		requireNonNull(currentContextProvider);
		requireNonNull(provider);
		requireNonNull(providerFinds);
		requireNonNull(appointmentTypesById);
		requireNonNull(startDate);
		requireNonNull(endDate);

		this.providerId = provider.getProviderId();
		this.providerName = provider.getName();
		this.clinicId = null;
		this.clinicDescription = null;
		this.startDate = startDate;
		this.endDate = endDate;
		this.appointmentTypes = appointmentTypeAvailabilitiesFor(providerFinds, Map.of(provider.getProviderId(), provider),
				appointmentTypesById, currentContextProvider.get().getLocale());
	}

	@AssistedInject
	public ProviderAvailabilityApiResponse(@Nonnull javax.inject.Provider<CurrentContext> currentContextProvider,
																				 @Assisted @Nonnull Clinic clinic,
																				 @Assisted @Nonnull List<ProviderFind> providerFinds,
																				 @Assisted("providersById") @Nonnull Map<UUID, Provider> providersById,
																				 @Assisted("appointmentTypesById") @Nonnull Map<UUID, AppointmentType> appointmentTypesById,
																				 @Assisted("startDate") @Nonnull LocalDate startDate,
																				 @Assisted("endDate") @Nonnull LocalDate endDate) {
		requireNonNull(currentContextProvider);
		requireNonNull(clinic);
		requireNonNull(providerFinds);
		requireNonNull(providersById);
		requireNonNull(appointmentTypesById);
		requireNonNull(startDate);
		requireNonNull(endDate);

		this.providerId = null;
		this.providerName = null;
		this.clinicId = clinic.getClinicId();
		this.clinicDescription = clinic.getDescription();
		this.startDate = startDate;
		this.endDate = endDate;
		this.appointmentTypes = appointmentTypeAvailabilitiesFor(providerFinds, providersById,
				appointmentTypesById, currentContextProvider.get().getLocale());
	}

	@Nonnull
	protected static List<AppointmentTypeAvailabilityApiResponse> appointmentTypeAvailabilitiesFor(@Nonnull List<ProviderFind> providerFinds,
																																																 @Nonnull Map<UUID, Provider> providersById,
																																																 @Nonnull Map<UUID, AppointmentType> appointmentTypesById,
																																																 @Nonnull Locale locale) {
		requireNonNull(providerFinds);
		requireNonNull(providersById);
		requireNonNull(appointmentTypesById);
		requireNonNull(locale);

		Map<UUID, SortedMap<LocalDate, List<TimeApiResponse>>> timesByDateByAppointmentTypeId = new LinkedHashMap<>();

		for (ProviderFind providerFind : providerFinds) {
			UUID providerId = providerFind.getProviderId();

			if (providerId == null || providerFind.getDates() == null)
				continue;

			Provider provider = providersById.get(providerId);
			String providerName = providerFind.getName() == null && provider != null ? provider.getName() : providerFind.getName();

			for (AvailabilityDate availabilityDate : providerFind.getDates()) {
				LocalDate date = availabilityDate.getDate();

				if (date == null || availabilityDate.getTimes() == null)
					continue;

				for (AvailabilityTime availabilityTime : availabilityDate.getTimes()) {
					if (availabilityTime.getTime() == null || availabilityTime.getStatus() != AvailabilityStatus.AVAILABLE
							|| availabilityTime.getAppointmentTypeIds() == null)
						continue;

					for (UUID appointmentTypeId : availabilityTime.getAppointmentTypeIds()) {
						if (appointmentTypeId == null || !appointmentTypesById.containsKey(appointmentTypeId))
							continue;

						SortedMap<LocalDate, List<TimeApiResponse>> timesByDate =
								timesByDateByAppointmentTypeId.computeIfAbsent(appointmentTypeId, ignored -> new TreeMap<>());
						List<TimeApiResponse> times = timesByDate.computeIfAbsent(date, ignored -> new ArrayList<>());

						times.add(new TimeApiResponse(providerId, providerName, date, availabilityTime, locale));
					}
				}
			}
		}

		return timesByDateByAppointmentTypeId.keySet().stream()
				.map(appointmentTypeId -> appointmentTypesById.get(appointmentTypeId))
				.filter(Objects::nonNull)
				.sorted(Comparator
						.comparing(AppointmentType::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
						.thenComparing(AppointmentType::getAppointmentTypeId))
				.map(appointmentType -> new AppointmentTypeAvailabilityApiResponse(appointmentType,
						timesByDateByAppointmentTypeId.get(appointmentType.getAppointmentTypeId())))
				.collect(Collectors.toList());
	}

	@Nullable
	public UUID getProviderId() {
		return providerId;
	}

	@Nullable
	public String getProviderName() {
		return providerName;
	}

	@Nullable
	public UUID getClinicId() {
		return clinicId;
	}

	@Nullable
	public String getClinicDescription() {
		return clinicDescription;
	}

	@Nonnull
	public LocalDate getStartDate() {
		return startDate;
	}

	@Nonnull
	public LocalDate getEndDate() {
		return endDate;
	}

	@Nonnull
	public List<AppointmentTypeAvailabilityApiResponse> getAppointmentTypes() {
		return appointmentTypes;
	}

	@ThreadSafe
	public static class AppointmentTypeAvailabilityApiResponse {
		@Nonnull
		private final UUID appointmentTypeId;
		@Nullable
		private final SchedulingSystemId schedulingSystemId;
		@Nullable
		private final VisitTypeId visitTypeId;
		@Nullable
		private final Long acuityAppointmentTypeId;
		@Nullable
		private final String epicVisitTypeId;
		@Nullable
		private final String epicVisitTypeIdType;
		@Nullable
		private final String name;
		@Nullable
		private final String description;
		@Nullable
		private final Long durationInMinutes;
		@Nullable
		private final String durationInMinutesDescription;
		@Nullable
		private final UUID screeningFlowId;
		@Nullable
		private final UUID assessmentId;
		@Nonnull
		private final List<AvailabilityApiResponse> availability;

		public AppointmentTypeAvailabilityApiResponse(@Nonnull AppointmentType appointmentType,
																									@Nonnull SortedMap<LocalDate, List<TimeApiResponse>> timesByDate) {
			requireNonNull(appointmentType);
			requireNonNull(timesByDate);

			this.appointmentTypeId = appointmentType.getAppointmentTypeId();
			this.schedulingSystemId = appointmentType.getSchedulingSystemId();
			this.visitTypeId = appointmentType.getVisitTypeId();
			this.acuityAppointmentTypeId = appointmentType.getAcuityAppointmentTypeId();
			this.epicVisitTypeId = appointmentType.getEpicVisitTypeId();
			this.epicVisitTypeIdType = appointmentType.getEpicVisitTypeIdType();
			this.name = appointmentType.getName();
			this.description = appointmentType.getDescription();
			this.durationInMinutes = appointmentType.getDurationInMinutes();
			this.durationInMinutesDescription = appointmentType.getDurationInMinutes() == null
					? null
					: format("%s minutes", appointmentType.getDurationInMinutes());
			this.screeningFlowId = appointmentType.getScreeningFlowId();
			this.assessmentId = appointmentType.getAssessmentId();
			this.availability = timesByDate.entrySet().stream()
					.map(entry -> new AvailabilityApiResponse(entry.getKey(), entry.getValue()))
					.collect(Collectors.toList());
		}

		@Nonnull
		public UUID getAppointmentTypeId() {
			return appointmentTypeId;
		}

		@Nullable
		public SchedulingSystemId getSchedulingSystemId() {
			return schedulingSystemId;
		}

		@Nullable
		public VisitTypeId getVisitTypeId() {
			return visitTypeId;
		}

		@Nullable
		public Long getAcuityAppointmentTypeId() {
			return acuityAppointmentTypeId;
		}

		@Nullable
		public String getEpicVisitTypeId() {
			return epicVisitTypeId;
		}

		@Nullable
		public String getEpicVisitTypeIdType() {
			return epicVisitTypeIdType;
		}

		@Nullable
		public String getName() {
			return name;
		}

		@Nullable
		public String getDescription() {
			return description;
		}

		@Nullable
		public Long getDurationInMinutes() {
			return durationInMinutes;
		}

		@Nullable
		public String getDurationInMinutesDescription() {
			return durationInMinutesDescription;
		}

		@Nullable
		public UUID getScreeningFlowId() {
			return screeningFlowId;
		}

		@Nullable
		public UUID getAssessmentId() {
			return assessmentId;
		}

		@Nonnull
		public List<AvailabilityApiResponse> getAvailability() {
			return availability;
		}
	}

	@ThreadSafe
	public static class AvailabilityApiResponse {
		@Nonnull
		private final LocalDate date;
		@Nonnull
		private final List<TimeApiResponse> times;

		public AvailabilityApiResponse(@Nonnull LocalDate date,
																	 @Nonnull List<TimeApiResponse> times) {
			requireNonNull(date);
			requireNonNull(times);

			this.date = date;
			this.times = times.stream()
					.sorted(Comparator
							.comparing(TimeApiResponse::getTime)
							.thenComparing(TimeApiResponse::getProviderName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
							.thenComparing(TimeApiResponse::getProviderId))
					.collect(Collectors.toList());
		}

		@Nonnull
		public LocalDate getDate() {
			return date;
		}

		@Nonnull
		public List<TimeApiResponse> getTimes() {
			return times;
		}
	}

	@ThreadSafe
	public static class TimeApiResponse {
		@Nonnull
		private final UUID providerId;
		@Nullable
		private final String providerName;
		@Nonnull
		private final LocalTime time;
		@Nonnull
		private final String timeDescription;
		@Nonnull
		private final LocalDateTime dateTime;
		@Nullable
		private final UUID epicDepartmentId;
		@Nullable
		private final String epicAppointmentFhirId;

		public TimeApiResponse(@Nonnull UUID providerId,
													 @Nullable String providerName,
													 @Nonnull LocalDate date,
													 @Nonnull AvailabilityTime availabilityTime,
													 @Nonnull Locale locale) {
			requireNonNull(providerId);
			requireNonNull(date);
			requireNonNull(availabilityTime);
			requireNonNull(availabilityTime.getTime());
			requireNonNull(locale);

			this.providerId = providerId;
			this.providerName = providerName;
			this.time = availabilityTime.getTime();
			this.timeDescription = normalizeTimeFormat(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
					.withLocale(locale)
					.format(this.time), locale);
			this.dateTime = LocalDateTime.of(date, this.time);
			this.epicDepartmentId = availabilityTime.getEpicDepartmentId();
			this.epicAppointmentFhirId = availabilityTime.getEpicAppointmentFhirId();
		}

		@Nonnull
		protected static String normalizeTimeFormat(@Nonnull String timeDescription,
																								@Nonnull Locale locale) {
			requireNonNull(timeDescription);
			requireNonNull(locale);

			return timeDescription.replace(" ", "").toLowerCase(locale);
		}

		@Nonnull
		public UUID getProviderId() {
			return providerId;
		}

		@Nullable
		public String getProviderName() {
			return providerName;
		}

		@Nonnull
		public LocalTime getTime() {
			return time;
		}

		@Nonnull
		public String getTimeDescription() {
			return timeDescription;
		}

		@Nonnull
		public LocalDateTime getDateTime() {
			return dateTime;
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
