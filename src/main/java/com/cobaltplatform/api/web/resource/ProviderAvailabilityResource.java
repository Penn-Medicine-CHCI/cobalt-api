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

package com.cobaltplatform.api.web.resource;

import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.model.api.request.ProviderFindRequest;
import com.cobaltplatform.api.model.api.request.ProviderFindRequest.ProviderFindAvailability;
import com.cobaltplatform.api.model.api.response.ProviderAvailabilityApiResponse.ProviderAvailabilityApiResponseFactory;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.AppointmentType;
import com.cobaltplatform.api.model.db.Clinic;
import com.cobaltplatform.api.model.db.Feature.FeatureId;
import com.cobaltplatform.api.model.db.Provider;
import com.cobaltplatform.api.model.db.SupportRole;
import com.cobaltplatform.api.model.db.SupportRole.SupportRoleId;
import com.cobaltplatform.api.model.security.AuthenticationRequired;
import com.cobaltplatform.api.model.service.ProviderFind;
import com.cobaltplatform.api.model.service.ProviderFind.AvailabilityDate;
import com.cobaltplatform.api.model.service.ProviderFind.AvailabilityStatus;
import com.cobaltplatform.api.model.service.ProviderFind.AvailabilityTime;
import com.cobaltplatform.api.service.AppointmentService;
import com.cobaltplatform.api.service.AuthorizationService;
import com.cobaltplatform.api.service.ClinicService;
import com.cobaltplatform.api.service.FeatureService;
import com.cobaltplatform.api.service.InstitutionService;
import com.cobaltplatform.api.service.ProviderService;
import com.cobaltplatform.api.util.ValidationException;
import com.lokalized.Strings;
import com.soklet.web.annotation.GET;
import com.soklet.web.annotation.PathParameter;
import com.soklet.web.annotation.QueryParameter;
import com.soklet.web.annotation.Resource;
import com.soklet.web.exception.AuthorizationException;
import com.soklet.web.exception.NotFoundException;
import com.soklet.web.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Resource
@Singleton
@ThreadSafe
public class ProviderAvailabilityResource {
	@Nonnull
	protected static final Long DEFAULT_AVAILABILITY_RANGE_IN_DAYS = 90L;
	@Nonnull
	private final ProviderService providerService;
	@Nonnull
	private final ClinicService clinicService;
	@Nonnull
	private final AppointmentService appointmentService;
	@Nonnull
	private final FeatureService featureService;
	@Nonnull
	private final InstitutionService institutionService;
	@Nonnull
	private final AuthorizationService authorizationService;
	@Nonnull
	private final ProviderAvailabilityApiResponseFactory providerAvailabilityApiResponseFactory;
	@Nonnull
	private final javax.inject.Provider<CurrentContext> currentContextProvider;
	@Nonnull
	private final Strings strings;
	@Nonnull
	private final Logger logger;

	@Inject
	public ProviderAvailabilityResource(@Nonnull ProviderService providerService,
																			@Nonnull ClinicService clinicService,
																			@Nonnull AppointmentService appointmentService,
																			@Nonnull FeatureService featureService,
																			@Nonnull InstitutionService institutionService,
																			@Nonnull AuthorizationService authorizationService,
																			@Nonnull ProviderAvailabilityApiResponseFactory providerAvailabilityApiResponseFactory,
																			@Nonnull javax.inject.Provider<CurrentContext> currentContextProvider,
																			@Nonnull Strings strings) {
		requireNonNull(providerService);
		requireNonNull(clinicService);
		requireNonNull(appointmentService);
		requireNonNull(featureService);
		requireNonNull(institutionService);
		requireNonNull(authorizationService);
		requireNonNull(providerAvailabilityApiResponseFactory);
		requireNonNull(currentContextProvider);
		requireNonNull(strings);

		this.providerService = providerService;
		this.clinicService = clinicService;
		this.appointmentService = appointmentService;
		this.featureService = featureService;
		this.institutionService = institutionService;
		this.authorizationService = authorizationService;
		this.providerAvailabilityApiResponseFactory = providerAvailabilityApiResponseFactory;
		this.currentContextProvider = currentContextProvider;
		this.strings = strings;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	@GET("/providers/{providerId}/availability")
	@AuthenticationRequired
	public ApiResponse providerAvailability(@Nonnull @PathParameter UUID providerId,
																					@Nonnull @QueryParameter Optional<LocalDate> startDate,
																					@Nonnull @QueryParameter Optional<LocalDate> endDate,
																					@Nonnull @QueryParameter Optional<String> featureId,
																					@Nonnull @QueryParameter Optional<UUID> appointmentTypeId) {
		requireNonNull(providerId);
		requireNonNull(startDate);
		requireNonNull(endDate);
		requireNonNull(featureId);
		requireNonNull(appointmentTypeId);

		Account account = getCurrentContext().getAccount().get();

		if (!getInstitutionService().isBookingV2Enabled(account.getInstitutionId()))
			throw new NotFoundException();

		Optional<FeatureId> parsedFeatureId = parseFeatureIdForAvailability(featureId);
		Provider provider = getProviderService().findProviderById(providerId).orElse(null);

		if (provider == null)
			throw new NotFoundException();

		if (!getAuthorizationService().canViewProvider(provider, account))
			throw new AuthorizationException();

		AvailabilityDateRange dateRange = availabilityDateRangeFor(startDate, endDate, timeZoneFor(account));
		Set<UUID> appointmentTypeIds = appointmentTypeId.map(Set::of).orElse(Collections.emptySet());
		List<ProviderFind> providerFinds = Boolean.TRUE.equals(provider.getActive()) && providerMatchesFeature(provider, parsedFeatureId)
				? getProviderService().findProviders(providerFindRequest(providerId, null, dateRange, appointmentTypeIds), account)
				: List.of();
		filterProviderFindsByAppointmentTypeIds(providerFinds, appointmentTypeIds);
		Map<UUID, AppointmentType> appointmentTypesById = appointmentTypesByIdFor(account);

		return new ApiResponse(new HashMap<String, Object>() {{
			put("providerAvailability", getProviderAvailabilityApiResponseFactory().create(provider, providerFinds,
					appointmentTypesById, dateRange.getStartDate(), dateRange.getEndDate()));
		}});
	}

	@Nonnull
	@GET("/clinics/{clinicId}/availability")
	@AuthenticationRequired
	public ApiResponse clinicAvailability(@Nonnull @PathParameter UUID clinicId,
																				@Nonnull @QueryParameter Optional<LocalDate> startDate,
																				@Nonnull @QueryParameter Optional<LocalDate> endDate,
																				@Nonnull @QueryParameter Optional<String> featureId,
																				@Nonnull @QueryParameter Optional<UUID> appointmentTypeId) {
		requireNonNull(clinicId);
		requireNonNull(startDate);
		requireNonNull(endDate);
		requireNonNull(featureId);
		requireNonNull(appointmentTypeId);

		Account account = getCurrentContext().getAccount().get();

		if (!getInstitutionService().isBookingV2Enabled(account.getInstitutionId()))
			throw new NotFoundException();

		Optional<FeatureId> parsedFeatureId = parseFeatureIdForAvailability(featureId);
		Clinic clinic = getClinicService().findClinicById(clinicId).orElse(null);

		if (clinic == null)
			throw new NotFoundException();

		if (!Objects.equals(clinic.getInstitutionId(), account.getInstitutionId()))
			throw new AuthorizationException();

		AvailabilityDateRange dateRange = availabilityDateRangeFor(startDate, endDate, timeZoneFor(account));
		Set<UUID> appointmentTypeIds = appointmentTypeId.map(Set::of).orElse(Collections.emptySet());
		List<ProviderFind> providerFinds = getProviderService().findProviders(providerFindRequest(null, clinicId, dateRange, appointmentTypeIds), account);
		filterProviderFindsByAppointmentTypeIds(providerFinds, appointmentTypeIds);
		Map<UUID, Provider> providersById = activeProvidersByIdFor(providerFinds, account, parsedFeatureId);
		List<ProviderFind> activeProviderFinds = providerFinds.stream()
				.filter(providerFind -> providersById.containsKey(providerFind.getProviderId()))
				.collect(Collectors.toList());
		Map<UUID, AppointmentType> appointmentTypesById = appointmentTypesByIdFor(account);

		return new ApiResponse(new HashMap<String, Object>() {{
			put("clinicAvailability", getProviderAvailabilityApiResponseFactory().create(clinic, activeProviderFinds,
					providersById, appointmentTypesById, dateRange.getStartDate(), dateRange.getEndDate()));
		}});
	}

	@Nonnull
	protected ProviderFindRequest providerFindRequest(@Nullable UUID providerId,
																										@Nullable UUID clinicId,
																										@Nonnull AvailabilityDateRange dateRange,
																										@Nonnull Set<UUID> appointmentTypeIds) {
		requireNonNull(dateRange);
		requireNonNull(appointmentTypeIds);

		ProviderFindRequest request = new ProviderFindRequest();
		request.setInstitutionId(getCurrentContext().getAccount().get().getInstitutionId());
		request.setProviderId(providerId);
		request.setClinicIds(clinicId == null ? Collections.emptySet() : Set.of(clinicId));
		request.setAppointmentTypeIds(appointmentTypeIds);
		request.setAvailability(ProviderFindAvailability.ONLY_AVAILABLE);
		request.setStartDate(dateRange.getStartDate());
		request.setEndDate(dateRange.getEndDate());
		request.setIncludePastAvailability(false);

		return request;
	}

	protected static void filterProviderFindsByAppointmentTypeIds(@Nonnull List<ProviderFind> providerFinds,
																																@Nonnull Set<UUID> appointmentTypeIds) {
		requireNonNull(providerFinds);
		requireNonNull(appointmentTypeIds);

		if (appointmentTypeIds.size() == 0)
			return;

		for (ProviderFind providerFind : providerFinds) {
			Set<UUID> providerAppointmentTypeIds = providerFind.getAppointmentTypeIds() == null
					? Set.of()
					: providerFind.getAppointmentTypeIds().stream()
					.filter(appointmentTypeIds::contains)
					.collect(Collectors.toSet());

			providerFind.setAppointmentTypeIds(providerAppointmentTypeIds);

			List<AvailabilityDate> availabilityDates = providerFind.getDates() == null ? List.of() : providerFind.getDates();
			List<AvailabilityDate> filteredAvailabilityDates = new ArrayList<>(availabilityDates.size());

			for (AvailabilityDate availabilityDate : availabilityDates) {
				List<AvailabilityTime> availabilityTimes = availabilityDate.getTimes() == null ? List.of() : availabilityDate.getTimes();
				List<AvailabilityTime> filteredAvailabilityTimes = new ArrayList<>(availabilityTimes.size());

				for (AvailabilityTime availabilityTime : availabilityTimes) {
					List<UUID> filteredAppointmentTypeIds = availabilityTime.getAppointmentTypeIds() == null
							? List.of()
							: availabilityTime.getAppointmentTypeIds().stream()
							.filter(appointmentTypeIds::contains)
							.collect(Collectors.toList());

					if (filteredAppointmentTypeIds.size() == 0)
						continue;

					availabilityTime.setAppointmentTypeIds(filteredAppointmentTypeIds);
					filteredAvailabilityTimes.add(availabilityTime);
				}

				if (filteredAvailabilityTimes.size() == 0)
					continue;

				availabilityDate.setTimes(filteredAvailabilityTimes);
				availabilityDate.setFullyBooked(filteredAvailabilityTimes.stream()
						.allMatch(availabilityTime -> availabilityTime.getStatus() == AvailabilityStatus.BOOKED));
				filteredAvailabilityDates.add(availabilityDate);
			}

			providerFind.setDates(filteredAvailabilityDates);
		}
	}

	@Nonnull
	protected static Optional<FeatureId> parseFeatureIdForAvailability(@Nonnull Optional<String> featureId) {
		requireNonNull(featureId);

		String featureIdAsString = featureId
				.map(String::trim)
				.filter(value -> value.length() > 0)
				.orElse(null);

		if (featureIdAsString == null)
			return Optional.empty();

		try {
			return Optional.of(FeatureId.valueOf(featureIdAsString));
		} catch (IllegalArgumentException e) {
			throw new ValidationException(new ValidationException.FieldError("featureId", "Feature ID is invalid."));
		}
	}

	@Nonnull
	protected Map<UUID, Provider> activeProvidersByIdFor(@Nonnull List<ProviderFind> providerFinds,
																											 @Nonnull Account account,
																											 @Nonnull Optional<FeatureId> featureId) {
		requireNonNull(providerFinds);
		requireNonNull(account);
		requireNonNull(featureId);

		return providerFinds.stream()
				.map(ProviderFind::getProviderId)
				.filter(Objects::nonNull)
				.distinct()
				.map(providerId -> getProviderService().findProviderById(providerId).orElse(null))
				.filter(Objects::nonNull)
				.filter(provider -> Boolean.TRUE.equals(provider.getActive()))
				.filter(provider -> Objects.equals(provider.getInstitutionId(), account.getInstitutionId()))
				.filter(provider -> providerMatchesFeature(provider, featureId))
				.collect(Collectors.toMap(Provider::getProviderId, Function.identity()));
	}

	protected boolean providerMatchesFeature(@Nonnull Provider provider,
																					 @Nonnull Optional<FeatureId> featureId) {
		requireNonNull(provider);
		requireNonNull(featureId);

		if (featureId.isEmpty())
			return true;

		List<SupportRoleId> featureSupportRoleIds = getFeatureService().findSupportRoleByFeatureId(featureId.get());
		List<SupportRoleId> providerSupportRoleIds = getProviderService().findSupportRolesByProviderId(provider.getProviderId()).stream()
				.map(SupportRole::getSupportRoleId)
				.filter(Objects::nonNull)
				.collect(Collectors.toList());

		return providerSupportRolesMatchFeature(featureSupportRoleIds, providerSupportRoleIds);
	}

	public static boolean providerSupportRolesMatchFeature(@Nonnull List<SupportRoleId> featureSupportRoleIds,
																												 @Nonnull List<SupportRoleId> providerSupportRoleIds) {
		requireNonNull(featureSupportRoleIds);
		requireNonNull(providerSupportRoleIds);

		if (featureSupportRoleIds.size() == 0)
			return false;

		return providerSupportRoleIds.stream().anyMatch(featureSupportRoleIds::contains);
	}

	@Nonnull
	protected Map<UUID, AppointmentType> appointmentTypesByIdFor(@Nonnull Account account) {
		requireNonNull(account);

		return getAppointmentService().findAppointmentTypesByInstitutionId(account.getInstitutionId()).stream()
				.collect(Collectors.toMap(AppointmentType::getAppointmentTypeId, Function.identity()));
	}

	@Nonnull
	protected ZoneId timeZoneFor(@Nonnull Account account) {
		requireNonNull(account);

		return account.getTimeZone() == null ? getCurrentContext().getTimeZone() : account.getTimeZone();
	}

	@Nonnull
	public static AvailabilityDateRange availabilityDateRangeFor(@Nonnull Optional<LocalDate> startDate,
																															 @Nonnull Optional<LocalDate> endDate,
																															 @Nonnull ZoneId timeZone) {
		requireNonNull(startDate);
		requireNonNull(endDate);
		requireNonNull(timeZone);

		LocalDate resolvedStartDate = startDate.orElse(LocalDate.now(timeZone));
		LocalDate resolvedEndDate = endDate.orElse(resolvedStartDate.plusDays(DEFAULT_AVAILABILITY_RANGE_IN_DAYS));

		if (resolvedStartDate.isAfter(resolvedEndDate))
			throw new ValidationException(new ValidationException.FieldError("startDate",
					"Start date cannot be after end date."));

		return new AvailabilityDateRange(resolvedStartDate, resolvedEndDate);
	}

	@Nonnull
	protected ProviderService getProviderService() {
		return providerService;
	}

	@Nonnull
	protected ClinicService getClinicService() {
		return clinicService;
	}

	@Nonnull
	protected AppointmentService getAppointmentService() {
		return appointmentService;
	}

	@Nonnull
	protected FeatureService getFeatureService() {
		return featureService;
	}

	@Nonnull
	protected InstitutionService getInstitutionService() {
		return this.institutionService;
	}

	@Nonnull
	protected AuthorizationService getAuthorizationService() {
		return authorizationService;
	}

	@Nonnull
	protected ProviderAvailabilityApiResponseFactory getProviderAvailabilityApiResponseFactory() {
		return providerAvailabilityApiResponseFactory;
	}

	@Nonnull
	protected CurrentContext getCurrentContext() {
		return currentContextProvider.get();
	}

	@Nonnull
	protected Strings getStrings() {
		return strings;
	}

	@Nonnull
	protected Logger getLogger() {
		return logger;
	}

	@ThreadSafe
	public static class AvailabilityDateRange {
		@Nonnull
		private final LocalDate startDate;
		@Nonnull
		private final LocalDate endDate;

		public AvailabilityDateRange(@Nonnull LocalDate startDate,
																 @Nonnull LocalDate endDate) {
			requireNonNull(startDate);
			requireNonNull(endDate);

			this.startDate = startDate;
			this.endDate = endDate;
		}

		@Nonnull
		public LocalDate getStartDate() {
			return startDate;
		}

		@Nonnull
		public LocalDate getEndDate() {
			return endDate;
		}
	}
}
