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

import com.cobaltplatform.api.model.api.response.ProviderListDetailsApiResponse.ProviderAppointmentModalityApiResponse;
import com.cobaltplatform.api.model.api.response.ProviderListDetailsApiResponse.ProviderAppointmentModalityId;
import com.cobaltplatform.api.model.api.response.ProviderListDetailsApiResponse.ProviderAppointmentSelectionTypeId;
import com.cobaltplatform.api.model.db.AppointmentType;
import com.cobaltplatform.api.model.db.Clinic;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.Provider;
import com.cobaltplatform.api.model.db.VideoconferencePlatform.VideoconferencePlatformId;
import com.cobaltplatform.api.model.service.ProviderFind;
import com.cobaltplatform.api.model.service.ProviderFind.AvailabilityDate;
import com.cobaltplatform.api.model.service.ProviderFind.AvailabilityStatus;
import com.cobaltplatform.api.model.service.ProviderFind.AvailabilityTime;
import com.cobaltplatform.api.model.service.ProviderSearchResult;
import com.cobaltplatform.api.util.Formatter;
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
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class ProviderSearchResultApiResponse {
	@Nonnull
	private final ProviderSearchResultTypeId providerSearchResultTypeId;
	@Nonnull
	private final UUID providerSearchResultId;
	@Nullable
	private final UUID providerId;
	@Nullable
	private final UUID clinicId;
	@Nullable
	private final InstitutionId institutionId;
	@Nullable
	private final String name;
	@Nullable
	private final String title;
	@Nullable
	private final String description;
	@Nullable
	private final String treatmentDescription;
	@Nullable
	private final String imageUrl;
	@Nullable
	private final String formattedPhoneNumber;
	@Nonnull
	private final List<ProviderAppointmentModalityApiResponse> supportedAppointmentModalities;
	@Nullable
	private final ProviderAppointmentSelectionTypeId appointmentSelectionTypeId;
	@Nullable
	private final String appointmentDescription;
	@Nullable
	private final FirstAvailableAppointmentApiResponse firstAvailableAppointment;
	@Nonnull
	private final Boolean hasMoreAppointments;

	public enum ProviderSearchResultTypeId {
		PROVIDER,
		CLINIC
	}

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface ProviderSearchResultApiResponseFactory {
		@Nonnull
		ProviderSearchResultApiResponse create(@Nonnull ProviderSearchResult providerSearchResult);

		@Nonnull
		ProviderSearchResultApiResponse create(@Nonnull Provider provider,
																						@Nonnull ProviderFind providerFind,
																						@Assisted("appointmentTypesById") @Nonnull Map<UUID, AppointmentType> appointmentTypesById);

		@Nonnull
		ProviderSearchResultApiResponse create(@Nonnull Clinic clinic,
																						@Nonnull List<ProviderFind> providerFinds,
																						@Assisted("providersById") @Nonnull Map<UUID, Provider> providersById,
																						@Assisted("appointmentTypesById") @Nonnull Map<UUID, AppointmentType> appointmentTypesById);
	}

	@AssistedInject
	public ProviderSearchResultApiResponse(@Nonnull Formatter formatter,
																				 @Nonnull Strings strings,
																				 @Assisted @Nonnull ProviderSearchResult providerSearchResult) {
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(providerSearchResult);

		if (providerSearchResult.getProviderSearchResultTypeId() == ProviderSearchResult.ProviderSearchResultTypeId.PROVIDER) {
			Provider provider = requireNonNull(providerSearchResult.getProvider());
			ProviderFind providerFind = requireNonNull(providerSearchResult.getProviderFind());
			Map<UUID, AppointmentType> appointmentTypesById = providerSearchResult.getAppointmentTypesById();
			List<AvailableAppointment> availableAppointments = availableAppointmentsFor(List.of(providerFind), Map.of(provider.getProviderId(), provider), appointmentTypesById);
			AvailableAppointment firstAvailableAppointment = availableAppointments.size() == 0 ? null : availableAppointments.get(0);

			this.providerSearchResultTypeId = ProviderSearchResultTypeId.PROVIDER;
			this.providerSearchResultId = provider.getProviderId();
			this.providerId = provider.getProviderId();
			this.clinicId = null;
			this.institutionId = provider.getInstitutionId();
			this.name = providerFind.getName();
			this.title = providerFind.getTitle();
			this.description = providerFind.getDescription();
			this.treatmentDescription = providerFind.getTreatmentDescription();
			this.imageUrl = providerFind.getImageUrl();
			this.formattedPhoneNumber = formatter.formatPhoneNumber(providerFind.getPhoneNumber(), provider.getLocale());
			this.supportedAppointmentModalities = supportedAppointmentModalitiesFor(provider, strings);
			this.appointmentSelectionTypeId = appointmentSelectionTypeIdFor(List.of(providerFind), Map.of(provider.getProviderId(), provider), availableAppointments);
			this.appointmentDescription = appointmentDescriptionFor(providerFind, firstAvailableAppointment, appointmentTypesById);
			this.firstAvailableAppointment = firstAvailableAppointment == null ? null : new FirstAvailableAppointmentApiResponse(firstAvailableAppointment, formatter, provider.getLocale());
			this.hasMoreAppointments = availableAppointments.size() > 1;
		} else {
			Clinic clinic = requireNonNull(providerSearchResult.getClinic());
			List<ProviderFind> providerFinds = providerSearchResult.getProviderFinds();
			Map<UUID, Provider> providersById = providerSearchResult.getProvidersById();
			Map<UUID, AppointmentType> appointmentTypesById = providerSearchResult.getAppointmentTypesById();
			List<AvailableAppointment> availableAppointments = availableAppointmentsFor(providerFinds, providersById, appointmentTypesById);
			AvailableAppointment firstAvailableAppointment = availableAppointments.size() == 0 ? null : availableAppointments.get(0);
			Locale locale = firstAvailableAppointment == null || firstAvailableAppointment.getProvider() == null
					? (clinic.getLocale() == null ? Locale.US : clinic.getLocale())
					: firstAvailableAppointment.getProvider().getLocale();

			this.providerSearchResultTypeId = ProviderSearchResultTypeId.CLINIC;
			this.providerSearchResultId = clinic.getClinicId();
			this.providerId = null;
			this.clinicId = clinic.getClinicId();
			this.institutionId = clinic.getInstitutionId();
			this.name = clinic.getDescription();
			this.title = null;
			this.description = clinic.getDescription();
			this.treatmentDescription = clinic.getTreatmentDescription();
			this.imageUrl = clinic.getImageUrl();
			this.formattedPhoneNumber = formatter.formatPhoneNumber(clinic.getPhoneNumber(), clinic.getLocale());
			this.supportedAppointmentModalities = supportedAppointmentModalitiesFor(providerFinds, providersById, strings);
			this.appointmentSelectionTypeId = appointmentSelectionTypeIdFor(providerFinds, providersById, availableAppointments);
			this.appointmentDescription = firstAvailableAppointment == null || firstAvailableAppointment.getAppointmentType() == null
					? null
					: descriptionFor(firstAvailableAppointment.getAppointmentType());
			this.firstAvailableAppointment = firstAvailableAppointment == null ? null : new FirstAvailableAppointmentApiResponse(firstAvailableAppointment, formatter, locale);
			this.hasMoreAppointments = availableAppointments.size() > 1;
		}
	}

	@AssistedInject
	public ProviderSearchResultApiResponse(@Nonnull Formatter formatter,
																				 @Nonnull Strings strings,
																				 @Assisted @Nonnull Provider provider,
																				 @Assisted @Nonnull ProviderFind providerFind,
																				 @Assisted("appointmentTypesById") @Nonnull Map<UUID, AppointmentType> appointmentTypesById) {
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(provider);
		requireNonNull(providerFind);
		requireNonNull(appointmentTypesById);

		List<AvailableAppointment> availableAppointments = availableAppointmentsFor(List.of(providerFind), Map.of(provider.getProviderId(), provider), appointmentTypesById);
		AvailableAppointment firstAvailableAppointment = availableAppointments.size() == 0 ? null : availableAppointments.get(0);

		this.providerSearchResultTypeId = ProviderSearchResultTypeId.PROVIDER;
		this.providerSearchResultId = provider.getProviderId();
		this.providerId = provider.getProviderId();
		this.clinicId = null;
		this.institutionId = provider.getInstitutionId();
		this.name = providerFind.getName();
		this.title = providerFind.getTitle();
		this.description = providerFind.getDescription();
		this.treatmentDescription = providerFind.getTreatmentDescription();
		this.imageUrl = providerFind.getImageUrl();
		this.formattedPhoneNumber = formatter.formatPhoneNumber(providerFind.getPhoneNumber(), provider.getLocale());
		this.supportedAppointmentModalities = supportedAppointmentModalitiesFor(provider, strings);
		this.appointmentSelectionTypeId = appointmentSelectionTypeIdFor(List.of(providerFind), Map.of(provider.getProviderId(), provider), availableAppointments);
		this.appointmentDescription = appointmentDescriptionFor(providerFind, firstAvailableAppointment, appointmentTypesById);
		this.firstAvailableAppointment = firstAvailableAppointment == null ? null : new FirstAvailableAppointmentApiResponse(firstAvailableAppointment, formatter, provider.getLocale());
		this.hasMoreAppointments = availableAppointments.size() > 1;
	}

	@AssistedInject
	public ProviderSearchResultApiResponse(@Nonnull Formatter formatter,
																				 @Nonnull Strings strings,
																				 @Assisted @Nonnull Clinic clinic,
																				 @Assisted @Nonnull List<ProviderFind> providerFinds,
																				 @Assisted("providersById") @Nonnull Map<UUID, Provider> providersById,
																				 @Assisted("appointmentTypesById") @Nonnull Map<UUID, AppointmentType> appointmentTypesById) {
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(clinic);
		requireNonNull(providerFinds);
		requireNonNull(providersById);
		requireNonNull(appointmentTypesById);

		List<AvailableAppointment> availableAppointments = availableAppointmentsFor(providerFinds, providersById, appointmentTypesById);
		AvailableAppointment firstAvailableAppointment = availableAppointments.size() == 0 ? null : availableAppointments.get(0);
		Locale locale = firstAvailableAppointment == null || firstAvailableAppointment.getProvider() == null
				? (clinic.getLocale() == null ? Locale.US : clinic.getLocale())
				: firstAvailableAppointment.getProvider().getLocale();

		this.providerSearchResultTypeId = ProviderSearchResultTypeId.CLINIC;
		this.providerSearchResultId = clinic.getClinicId();
		this.providerId = null;
		this.clinicId = clinic.getClinicId();
		this.institutionId = clinic.getInstitutionId();
		this.name = clinic.getDescription();
		this.title = null;
		this.description = clinic.getDescription();
		this.treatmentDescription = clinic.getTreatmentDescription();
		this.imageUrl = clinic.getImageUrl();
		this.formattedPhoneNumber = formatter.formatPhoneNumber(clinic.getPhoneNumber(), clinic.getLocale());
		this.supportedAppointmentModalities = supportedAppointmentModalitiesFor(providerFinds, providersById, strings);
		this.appointmentSelectionTypeId = appointmentSelectionTypeIdFor(providerFinds, providersById, availableAppointments);
		this.appointmentDescription = firstAvailableAppointment == null || firstAvailableAppointment.getAppointmentType() == null
				? null
				: descriptionFor(firstAvailableAppointment.getAppointmentType());
		this.firstAvailableAppointment = firstAvailableAppointment == null ? null : new FirstAvailableAppointmentApiResponse(firstAvailableAppointment, formatter, locale);
		this.hasMoreAppointments = availableAppointments.size() > 1;
	}

	@Nonnull
	protected List<ProviderAppointmentModalityApiResponse> supportedAppointmentModalitiesFor(@Nonnull Provider provider,
																																													 @Nonnull Strings strings) {
		requireNonNull(provider);
		requireNonNull(strings);

		return supportedAppointmentModalitiesFor(providerAppointmentModalityIdsFor(provider), strings);
	}

	@Nonnull
	protected List<ProviderAppointmentModalityApiResponse> supportedAppointmentModalitiesFor(@Nonnull List<ProviderFind> providerFinds,
																																													 @Nonnull Map<UUID, Provider> providersById,
																																													 @Nonnull Strings strings) {
		requireNonNull(providerFinds);
		requireNonNull(providersById);
		requireNonNull(strings);

		EnumSet<ProviderAppointmentModalityId> providerAppointmentModalityIds = EnumSet.noneOf(ProviderAppointmentModalityId.class);

		for (ProviderFind providerFind : providerFinds) {
			Provider provider = providersById.get(providerFind.getProviderId());

			if (provider != null)
				providerAppointmentModalityIds.addAll(providerAppointmentModalityIdsFor(provider));
		}

		return supportedAppointmentModalitiesFor(providerAppointmentModalityIds, strings);
	}

	@Nonnull
	protected List<ProviderAppointmentModalityApiResponse> supportedAppointmentModalitiesFor(@Nonnull Set<ProviderAppointmentModalityId> providerAppointmentModalityIds,
																																													 @Nonnull Strings strings) {
		requireNonNull(providerAppointmentModalityIds);
		requireNonNull(strings);

		List<ProviderAppointmentModalityApiResponse> supportedAppointmentModalities = new ArrayList<>(providerAppointmentModalityIds.size());

		for (ProviderAppointmentModalityId providerAppointmentModalityId : List.of(ProviderAppointmentModalityId.PHONE, ProviderAppointmentModalityId.VIRTUAL, ProviderAppointmentModalityId.IN_PERSON))
			if (providerAppointmentModalityIds.contains(providerAppointmentModalityId))
				supportedAppointmentModalities.add(new ProviderAppointmentModalityApiResponse(providerAppointmentModalityId, providerAppointmentModalityDescriptionFor(providerAppointmentModalityId, strings)));

		return supportedAppointmentModalities;
	}

	@Nonnull
	protected String providerAppointmentModalityDescriptionFor(@Nonnull ProviderAppointmentModalityId providerAppointmentModalityId,
																														@Nonnull Strings strings) {
		requireNonNull(providerAppointmentModalityId);
		requireNonNull(strings);

		return switch (providerAppointmentModalityId) {
			case PHONE -> strings.get("Phone");
			case VIRTUAL -> strings.get("Virtual");
			case IN_PERSON -> strings.get("In Person");
		};
	}

	@Nonnull
	protected Set<ProviderAppointmentModalityId> providerAppointmentModalityIdsFor(@Nonnull Provider provider) {
		requireNonNull(provider);

		EnumSet<ProviderAppointmentModalityId> providerAppointmentModalityIds = EnumSet.noneOf(ProviderAppointmentModalityId.class);
		VideoconferencePlatformId videoconferencePlatformId = provider.getVideoconferencePlatformId();

		if (videoconferencePlatformId == VideoconferencePlatformId.TELEPHONE
				|| (trimToNull(provider.getPhoneNumber()) != null && !Boolean.TRUE.equals(provider.getDisplayPhoneNumberOnlyForBooking())))
			providerAppointmentModalityIds.add(ProviderAppointmentModalityId.PHONE);

		if (videoconferencePlatformId != null && videoconferencePlatformId != VideoconferencePlatformId.TELEPHONE)
			providerAppointmentModalityIds.add(ProviderAppointmentModalityId.VIRTUAL);

		if (providerAppointmentModalityIds.size() == 0)
			// TODO: Identify an explicit provider or appointment-type source for IN_PERSON instead of inferring it from missing remote modalities.
			providerAppointmentModalityIds.add(ProviderAppointmentModalityId.IN_PERSON);

		return providerAppointmentModalityIds;
	}

	@Nonnull
	protected static ProviderAppointmentSelectionTypeId appointmentSelectionTypeIdFor(@Nonnull List<ProviderFind> providerFinds,
																																									 @Nonnull Map<UUID, Provider> providersById,
																																									 @Nonnull List<AvailableAppointment> availableAppointments) {
		requireNonNull(providerFinds);
		requireNonNull(providersById);
		requireNonNull(availableAppointments);

		if (appointmentByPhoneFor(providerFinds, providersById))
			return ProviderAppointmentSelectionTypeId.APPOINTMENT_BY_PHONE;

		Set<UUID> appointmentTypeIds = distinctAppointmentTypeIdsForAvailableAppointments(availableAppointments);

		if (appointmentTypeIds == null)
			appointmentTypeIds = distinctAppointmentTypeIdsForProviderFinds(providerFinds);

		return appointmentTypeIds.size() == 1
				? ProviderAppointmentSelectionTypeId.APPOINTMENT_PREDETERMINED
				: ProviderAppointmentSelectionTypeId.APPOINTMENT_UNDETERMINED;
	}

	protected static boolean appointmentByPhoneFor(@Nonnull List<ProviderFind> providerFinds,
																								 @Nonnull Map<UUID, Provider> providersById) {
		requireNonNull(providerFinds);
		requireNonNull(providersById);

		if (providerFinds.size() == 0)
			return false;

		for (ProviderFind providerFind : providerFinds) {
			Provider provider = providersById.get(providerFind.getProviderId());

			if (provider == null || provider.getVideoconferencePlatformId() != VideoconferencePlatformId.TELEPHONE)
				return false;
		}

		return true;
	}

	@Nullable
	protected static Set<UUID> distinctAppointmentTypeIdsForAvailableAppointments(@Nonnull List<AvailableAppointment> availableAppointments) {
		requireNonNull(availableAppointments);

		if (availableAppointments.size() == 0)
			return null;

		Set<UUID> appointmentTypeIds = new HashSet<>();

		for (AvailableAppointment availableAppointment : availableAppointments) {
			List<UUID> availableAppointmentTypeIds = availableAppointment.getAvailabilityTime().getAppointmentTypeIds();

			if (availableAppointmentTypeIds == null || availableAppointmentTypeIds.stream().noneMatch(appointmentTypeId -> appointmentTypeId != null))
				return null;

			for (UUID appointmentTypeId : availableAppointmentTypeIds)
				if (appointmentTypeId != null)
					appointmentTypeIds.add(appointmentTypeId);
		}

		return appointmentTypeIds;
	}

	@Nonnull
	protected static Set<UUID> distinctAppointmentTypeIdsForProviderFinds(@Nonnull List<ProviderFind> providerFinds) {
		requireNonNull(providerFinds);

		Set<UUID> appointmentTypeIds = new HashSet<>();

		for (ProviderFind providerFind : providerFinds) {
			Set<UUID> providerFindAppointmentTypeIds = providerFind.getAppointmentTypeIds();

			if (providerFindAppointmentTypeIds == null || providerFindAppointmentTypeIds.stream().noneMatch(appointmentTypeId -> appointmentTypeId != null))
				return Set.of();

			for (UUID appointmentTypeId : providerFindAppointmentTypeIds)
				if (appointmentTypeId != null)
					appointmentTypeIds.add(appointmentTypeId);
		}

		return appointmentTypeIds;
	}

	@Nonnull
	protected static List<AvailableAppointment> availableAppointmentsFor(@Nonnull List<ProviderFind> providerFinds,
																																			 @Nonnull Map<UUID, Provider> providersById,
																																			 @Nonnull Map<UUID, AppointmentType> appointmentTypesById) {
		requireNonNull(providerFinds);
		requireNonNull(providersById);
		requireNonNull(appointmentTypesById);

		List<AvailableAppointment> availableAppointments = new ArrayList<>();

		for (ProviderFind providerFind : providerFinds) {
			if (providerFind.getDates() == null)
				continue;

			Provider provider = providersById.get(providerFind.getProviderId());

			for (AvailabilityDate availabilityDate : providerFind.getDates()) {
				if (availabilityDate.getDate() == null || availabilityDate.getTimes() == null)
					continue;

				for (AvailabilityTime availabilityTime : availabilityDate.getTimes()) {
					if (availabilityTime.getTime() == null || availabilityTime.getStatus() == AvailabilityStatus.BOOKED)
						continue;

					availableAppointments.add(new AvailableAppointment(provider, availabilityDate.getDate(), availabilityTime,
							appointmentTypeFor(availabilityTime, appointmentTypesById)));
				}
			}
		}

		return availableAppointments.stream()
				.sorted(Comparator
						.comparing(AvailableAppointment::getDate)
						.thenComparing(availableAppointment -> availableAppointment.getAvailabilityTime().getTime())
						.thenComparing(availableAppointment -> availableAppointment.getProvider() == null ? null : availableAppointment.getProvider().getName(), Comparator.nullsLast(String::compareToIgnoreCase))
						.thenComparing(availableAppointment -> availableAppointment.getProvider() == null ? null : availableAppointment.getProvider().getProviderId(), Comparator.nullsLast(UUID::compareTo)))
				.collect(Collectors.toList());
	}

	@Nullable
	protected static AppointmentType appointmentTypeFor(@Nonnull AvailabilityTime availabilityTime,
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
	protected static String descriptionFor(@Nonnull AppointmentType appointmentType) {
		requireNonNull(appointmentType);

		String description = trimToNull(appointmentType.getDescription());

		return description == null ? trimToNull(appointmentType.getName()) : description;
	}

	@Nonnull
	public ProviderSearchResultTypeId getProviderSearchResultTypeId() {
		return this.providerSearchResultTypeId;
	}

	@Nonnull
	public UUID getProviderSearchResultId() {
		return this.providerSearchResultId;
	}

	@Nullable
	public UUID getProviderId() {
		return this.providerId;
	}

	@Nullable
	public UUID getClinicId() {
		return this.clinicId;
	}

	@Nullable
	public InstitutionId getInstitutionId() {
		return this.institutionId;
	}

	@Nullable
	public String getName() {
		return this.name;
	}

	@Nullable
	public String getTitle() {
		return this.title;
	}

	@Nullable
	public String getDescription() {
		return this.description;
	}

	@Nullable
	public String getTreatmentDescription() {
		return this.treatmentDescription;
	}

	@Nullable
	public String getImageUrl() {
		return this.imageUrl;
	}

	@Nullable
	public String getFormattedPhoneNumber() {
		return this.formattedPhoneNumber;
	}

	@Nonnull
	public List<ProviderAppointmentModalityApiResponse> getSupportedAppointmentModalities() {
		return this.supportedAppointmentModalities;
	}

	@Nullable
	public ProviderAppointmentSelectionTypeId getAppointmentSelectionTypeId() {
		return this.appointmentSelectionTypeId;
	}

	@Nullable
	public String getAppointmentDescription() {
		return this.appointmentDescription;
	}

	@Nullable
	public FirstAvailableAppointmentApiResponse getFirstAvailableAppointment() {
		return this.firstAvailableAppointment;
	}

	@Nonnull
	public Boolean getHasMoreAppointments() {
		return this.hasMoreAppointments;
	}

	@ThreadSafe
	protected static class AvailableAppointment {
		@Nullable
		private final Provider provider;
		@Nonnull
		private final LocalDate date;
		@Nonnull
		private final AvailabilityTime availabilityTime;
		@Nullable
		private final AppointmentType appointmentType;

		public AvailableAppointment(@Nullable Provider provider,
																@Nonnull LocalDate date,
																@Nonnull AvailabilityTime availabilityTime,
																@Nullable AppointmentType appointmentType) {
			requireNonNull(date);
			requireNonNull(availabilityTime);

			this.provider = provider;
			this.date = date;
			this.availabilityTime = availabilityTime;
			this.appointmentType = appointmentType;
		}

		@Nullable
		public Provider getProvider() {
			return this.provider;
		}

		@Nonnull
		public LocalDate getDate() {
			return this.date;
		}

		@Nonnull
		public AvailabilityTime getAvailabilityTime() {
			return this.availabilityTime;
		}

		@Nullable
		public AppointmentType getAppointmentType() {
			return this.appointmentType;
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
																								@Nonnull Locale locale) {
			requireNonNull(availableAppointment);
			requireNonNull(formatter);
			requireNonNull(locale);

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
			this.screeningFlowId = appointmentType == null ? null : appointmentType.getScreeningFlowId();
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

		@Nonnull
		public LocalDate getDate() {
			return this.date;
		}

		@Nonnull
		public LocalTime getTime() {
			return this.time;
		}

		@Nonnull
		public LocalDateTime getDateTime() {
			return this.dateTime;
		}

		@Nonnull
		public String getTimeDescription() {
			return this.timeDescription;
		}

		@Nullable
		public UUID getAppointmentTypeId() {
			return this.appointmentTypeId;
		}

		@Nullable
		public List<UUID> getAppointmentTypeIds() {
			return this.appointmentTypeIds;
		}

		@Nullable
		public String getAppointmentDescription() {
			return this.appointmentDescription;
		}

		@Nullable
		public UUID getAssessmentId() {
			return this.assessmentId;
		}

		@Nullable
		public UUID getScreeningFlowId() {
			return this.screeningFlowId;
		}

		@Nullable
		public UUID getEpicDepartmentId() {
			return this.epicDepartmentId;
		}

		@Nullable
		public String getEpicAppointmentFhirId() {
			return this.epicAppointmentFhirId;
		}
	}
}
