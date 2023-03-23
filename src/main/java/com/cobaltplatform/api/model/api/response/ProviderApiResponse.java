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
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.PaymentFunding;
import com.cobaltplatform.api.model.db.Provider;
import com.cobaltplatform.api.model.db.SupportRole;
import com.cobaltplatform.api.model.service.AvailabilityTime;
import com.cobaltplatform.api.service.ProviderService;
import com.cobaltplatform.api.util.Formatter;
import com.cobaltplatform.api.util.JsonMapper;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lokalized.Strings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class ProviderApiResponse {
	@Nonnull
	private final UUID providerId;
	@Nonnull
	private final InstitutionId institutionId;
	@Nonnull
	private final String name;
	@Nonnull
	private final String emailAddress;
	@Nullable
	private final String title;
	@Nullable
	private final String entity;
	@Nullable
	private final String clinic;
	@Nullable
	private final String license;
	@Nullable
	private final String specialty;
	@Nonnull
	private final String imageUrl;
	@Nonnull
	private final Boolean isDefaultImageUrl;
	@Nonnull
	private final ZoneId timeZone;
	@Nonnull
	private final Locale locale;
	@Nonnull
	private final List<String> tags; // e.g. ["Experienced Coach", "Calming Voice"]
	@Nullable
	private final List<AvailabilityTimeApiResponse> availabilityTimes;
	@Nullable
	private final List<SupportRoleApiResponse> supportRoles;
	@Nullable
	private final String supportRolesDescription;
	@Nullable
	private final Boolean phoneNumberRequiredForAppointment;
	@Nullable
	private final List<String> paymentFundingDescriptions;
	@Nullable
	private final String bioUrl;
	@Nullable
	private final String bio;
	@Nullable
	private final String phoneNumber;
	@Nullable
	private final String formattedPhoneNumber;
	@Nullable
	private Boolean displayPhoneNumberOnlyForBooking;

	public enum ProviderApiResponseSupplement {
		EVERYTHING,
		SUPPORT_ROLES,
		PAYMENT_FUNDING
	}

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface ProviderApiResponseFactory {
		@Nonnull
		ProviderApiResponse create(@Nonnull Provider provider,
															 @Nullable ProviderApiResponseSupplement... supplements);

		@Nonnull
		ProviderApiResponse create(@Nonnull Provider provider,
															 @Nullable List<AvailabilityTime> availabilityTimes,
															 @Nullable ProviderApiResponseSupplement... supplements);
	}

	@AssistedInject
	public ProviderApiResponse(@Nonnull ProviderService providerService,
														 @Nonnull Formatter formatter,
														 @Nonnull Strings strings,
														 @Nonnull JsonMapper jsonMapper,
														 @Nonnull AvailabilityTimeApiResponseFactory availabilityTimeApiResponseFactory,
														 @Nonnull SupportRoleApiResponseFactory supportRoleApiResponseFactory,
														 @Nonnull Configuration configuration,
														 @Assisted @Nonnull Provider provider,
														 @Assisted @Nullable ProviderApiResponseSupplement... supplements) {
		this(providerService, formatter, strings, jsonMapper, availabilityTimeApiResponseFactory, supportRoleApiResponseFactory, configuration, provider, null, supplements);
	}

	@AssistedInject
	public ProviderApiResponse(@Nonnull ProviderService providerService,
														 @Nonnull Formatter formatter,
														 @Nonnull Strings strings,
														 @Nonnull JsonMapper jsonMapper,
														 @Nonnull AvailabilityTimeApiResponseFactory availabilityTimeApiResponseFactory,
														 @Nonnull SupportRoleApiResponseFactory supportRoleApiResponseFactory,
														 @Nonnull Configuration configuration,
														 @Assisted @Nonnull Provider provider,
														 @Assisted @Nullable List<AvailabilityTime> availabilityTimes,
														 @Assisted @Nullable ProviderApiResponseSupplement... supplements) {
		requireNonNull(providerService);
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(jsonMapper);
		requireNonNull(availabilityTimeApiResponseFactory);
		requireNonNull(supportRoleApiResponseFactory);
		requireNonNull(provider);
		requireNonNull(configuration);

		List<ProviderApiResponseSupplement> supplementsList = Arrays.asList(supplements);

		this.providerId = provider.getProviderId();
		this.institutionId = provider.getInstitutionId();
		this.emailAddress = provider.getEmailAddress();
		this.name = provider.getName();
		this.title = provider.getTitle();
		this.clinic = provider.getClinic();
		this.specialty = provider.getSpecialty();
		this.license = provider.getLicense();
		this.entity = provider.getEntity();
		this.imageUrl = provider.getImageUrl();
		this.isDefaultImageUrl = provider.getImageUrl() == null;
		this.timeZone = provider.getTimeZone();
		this.locale = provider.getLocale();
		this.tags = provider.getTags() == null ? Collections.emptyList() : jsonMapper.toList(provider.getTags(), String.class);
		this.bioUrl = trimToNull(provider.getBioUrl());
		this.phoneNumber = provider.getPhoneNumber();
		this.displayPhoneNumberOnlyForBooking = provider.getDisplayPhoneNumberOnlyForBooking();
		this.formattedPhoneNumber = formatter.formatPhoneNumber(provider.getPhoneNumber(), provider.getLocale());

		String bio = trimToNull(provider.getBio());

		if (bio != null) {
			// HTML-ify line breaks if we do have a bio
			bio = bio.replace("\n", "<br/>");
		} else if (bioUrl != null) {
			// Make a synthetic bio if we have a URL but no real bio
			bio = format(strings.get("<a target='_blank' href='{{bioUrl}}'>Click here to read more about {{providerName}}</a>", new HashMap<String, Object>() {{
				put("bioUrl", bioUrl);
				put("providerName", getName());
			}}));
		}

		this.bio = bio;

		boolean includeEverything = supplementsList.contains(ProviderApiResponseSupplement.EVERYTHING);

		if (availabilityTimes == null)
			this.availabilityTimes = null;
		else
			this.availabilityTimes = availabilityTimes.stream()
					.map((availabilityTime -> availabilityTimeApiResponseFactory.create(availabilityTime)))
					.collect(Collectors.toList());

		if (includeEverything || supplementsList.contains(ProviderApiResponseSupplement.SUPPORT_ROLES)) {
			this.supportRoles = providerService.findSupportRolesByProviderId(provider.getProviderId()).stream()
					.map((supportRole) -> supportRoleApiResponseFactory.create(supportRole))
					.collect(Collectors.toList());
			this.supportRolesDescription = this.supportRoles.size() == 0
					? null
					: this.supportRoles.stream()
					.map(supportRole -> supportRole.getDescription())
					.collect(Collectors.joining(", "));
			this.phoneNumberRequiredForAppointment = this.supportRoles.stream()
					.anyMatch(role -> role.getSupportRoleId().equals(SupportRole.SupportRoleId.PSYCHIATRIST));
		} else {
			this.supportRoles = null;
			this.supportRolesDescription = null;
			this.phoneNumberRequiredForAppointment = false;
		}

		if (includeEverything || supplementsList.contains(ProviderApiResponseSupplement.PAYMENT_FUNDING)) {
			// Can be done more optimally later...
			List<PaymentFunding> paymentFundings = providerService.findPaymentFundings();
			Map<PaymentFunding.PaymentFundingId, String> paymentFundingDescriptionsById = new HashMap<>(paymentFundings.size());

			for (PaymentFunding paymentFunding : paymentFundings)
				paymentFundingDescriptionsById.put(paymentFunding.getPaymentFundingId(), paymentFunding.getDescription());

			List<PaymentFunding> providerPaymentFundings = providerService.findPaymentFundingsByProviderId(provider.getProviderId());

			this.paymentFundingDescriptions = providerPaymentFundings.stream()
					.map(paymentFunding -> paymentFundingDescriptionsById.get(paymentFunding.getPaymentFundingId()))
					.collect(Collectors.toList());
		} else {
			this.paymentFundingDescriptions = null;
		}
	}

	@Nonnull
	public UUID getProviderId() {
		return providerId;
	}

	@Nonnull
	public InstitutionId getInstitutionId() {
		return institutionId;
	}

	@Nonnull
	public String getEmailAddress() {
		return emailAddress;
	}

	@Nonnull
	public String getName() {
		return name;
	}

	@Nullable
	public String getTitle() {
		return title;
	}

	@Nullable
	public String getEntity() {
		return entity;
	}

	@Nullable
	public String getClinic() {
		return clinic;
	}

	@Nullable
	public String getLicense() {
		return license;
	}

	@Nullable
	public String getSpecialty() {
		return specialty;
	}

	@Nullable
	public List<SupportRoleApiResponse> getSupportRoles() {
		return supportRoles;
	}

	@Nullable
	public String getSupportRolesDescription() {
		return supportRolesDescription;
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
	public ZoneId getTimeZone() {
		return timeZone;
	}

	@Nonnull
	public Locale getLocale() {
		return locale;
	}

	@Nonnull
	public List<String> getTags() {
		return tags;
	}

	@Nullable
	public List<AvailabilityTimeApiResponse> getAvailabilityTimes() {
		return availabilityTimes;
	}

	@Nullable
	public Boolean getPhoneNumberRequiredForAppointment() {
		return phoneNumberRequiredForAppointment;
	}

	@Nullable
	public List<String> getPaymentFundingDescriptions() {
		return paymentFundingDescriptions;
	}

	@Nullable
	public String getBioUrl() {
		return bioUrl;
	}

	@Nullable
	public String getBio() {
		return bio;
	}
}