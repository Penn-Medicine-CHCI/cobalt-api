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
import com.cobaltplatform.api.model.db.ClinicLocation;
import com.cobaltplatform.api.model.db.ProviderLocation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class LocationApiResponse {
	@Nullable
	private final UUID locationId;
	@Nullable
	private final String name;
	@Nullable
	private final String shortName;
	@Nullable
	private final Address address;

	public LocationApiResponse(@Nonnull ProviderLocation providerLocation,
														 @Nullable Address address) {
		requireNonNull(providerLocation);

		this.locationId = providerLocation.getProviderLocationId();
		this.name = providerLocation.getName();
		this.shortName = providerLocation.getShortName();
		this.address = address;
	}

	public LocationApiResponse(@Nonnull ClinicLocation clinicLocation,
														 @Nullable Address address) {
		requireNonNull(clinicLocation);

		this.locationId = clinicLocation.getClinicLocationId();
		this.name = clinicLocation.getName();
		this.shortName = clinicLocation.getShortName();
		this.address = address;
	}

	@Nullable
	public UUID getLocationId() {
		return this.locationId;
	}

	@Nullable
	public String getName() {
		return this.name;
	}

	@Nullable
	public String getShortName() {
		return this.shortName;
	}

	@Nullable
	public Address getAddress() {
		return this.address;
	}
}
