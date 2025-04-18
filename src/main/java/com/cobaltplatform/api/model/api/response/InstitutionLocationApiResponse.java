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

import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.InstitutionLocation;
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

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface InstitutionLocationApiResponseFactory {
		@Nonnull
		InstitutionLocationApiResponse create(@Nonnull InstitutionLocation institutionLocation);
	}

	@AssistedInject
	public InstitutionLocationApiResponse(@Assisted @Nonnull InstitutionLocation institutionLocation) {
		requireNonNull(institutionLocation);

		this.institutionLocationId = institutionLocation.getInstitutionLocationId();
		this.institutionId = institutionLocation.getInstitutionId();
		this.name = institutionLocation.getName();
		this.shortName = institutionLocation.getShortName();
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
}