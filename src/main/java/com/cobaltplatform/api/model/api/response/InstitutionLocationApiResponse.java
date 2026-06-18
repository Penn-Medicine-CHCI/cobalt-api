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

import com.cobaltplatform.api.model.db.Address;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.InstitutionLocation;
import com.cobaltplatform.api.service.AddressService;
import com.cobaltplatform.api.util.Formatter;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Optional;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class InstitutionLocationApiResponse {
	@Nonnull
	private final UUID institutionLocationId;
	@Nonnull
	private final InstitutionId institutionId;
	@Nonnull
	private final String name;
	@Nullable
	private final String shortName;
	@Nullable
	private final Address address;
	@Nullable
	private final String phoneNumber;
	@Nullable
	private final String formattedPhoneNumber;
	@Nullable
	private final String websiteUrl;
	@Nullable
	private final String emailAddress;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface InstitutionLocationApiResponseFactory {
		@Nonnull
		InstitutionLocationApiResponse create(@Nonnull InstitutionLocation institutionLocation);
	}

	@AssistedInject
	public InstitutionLocationApiResponse(@Nonnull AddressService addressService,
																				@Nonnull Formatter formatter,
																				@Assisted @Nonnull InstitutionLocation institutionLocation) {
		this(institutionLocation, requireNonNull(addressService).findAddressById(requireNonNull(institutionLocation).getAddressId()).orElse(null), formatter);
	}

	public InstitutionLocationApiResponse(@Nonnull InstitutionLocation institutionLocation,
																				@Nullable Address address,
																				@Nonnull Formatter formatter) {
		requireNonNull(institutionLocation);
		requireNonNull(formatter);

		this.institutionLocationId = institutionLocation.getInstitutionLocationId();
		this.institutionId = institutionLocation.getInstitutionId();
		this.name = institutionLocation.getName();
		this.shortName = institutionLocation.getShortName();
		this.address = address;
		this.phoneNumber = institutionLocation.getPhoneNumber();
		this.formattedPhoneNumber = formatter.formatPhoneNumber(institutionLocation.getPhoneNumber());
		this.websiteUrl = institutionLocation.getWebsiteUrl();
		this.emailAddress = institutionLocation.getEmailAddress();
	}

	@Nonnull
	public UUID getInstitutionLocationId() {
		return this.institutionLocationId;
	}

	@Nonnull
	public InstitutionId getInstitutionId() {
		return this.institutionId;
	}

	@Nonnull
	public String getName() {
		return this.name;
	}

	@Nonnull
	public Optional<String> getShortName() {
		return Optional.ofNullable(this.shortName);
	}

	@Nullable
	public Address getAddress() {
		return this.address;
	}

	@Nullable
	public String getPhoneNumber() {
		return this.phoneNumber;
	}

	@Nullable
	public String getFormattedPhoneNumber() {
		return this.formattedPhoneNumber;
	}

	@Nullable
	public String getWebsiteUrl() {
		return this.websiteUrl;
	}

	@Nullable
	public String getEmailAddress() {
		return this.emailAddress;
	}
}
