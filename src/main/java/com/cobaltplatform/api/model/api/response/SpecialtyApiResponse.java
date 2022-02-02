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

import com.cobaltplatform.api.model.db.Specialty;
import com.cobaltplatform.api.util.Formatter;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lokalized.Strings;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class SpecialtyApiResponse {
	@Nonnull
	private final UUID specialtyId;
	@Nonnull
	private final String description;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface SpecialtyApiResponseFactory {
		@Nonnull
		SpecialtyApiResponse create(@Nonnull Specialty specialty);
	}

	@AssistedInject
	public SpecialtyApiResponse(@Nonnull Formatter formatter,
															@Nonnull Strings strings,
															@Assisted @Nonnull Specialty specialty) {
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(specialty);

		this.specialtyId = specialty.getSpecialtyId();
		this.description = specialty.getDescription();
	}

	@Nonnull
	public UUID getSpecialtyId() {
		return specialtyId;
	}

	@Nonnull
	public String getDescription() {
		return description;
	}
}