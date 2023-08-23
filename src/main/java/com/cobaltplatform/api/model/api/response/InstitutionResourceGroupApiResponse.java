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

import com.cobaltplatform.api.model.db.Color.ColorId;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.InstitutionResourceGroup;
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
public class InstitutionResourceGroupApiResponse {
	@Nonnull
	private final UUID institutionResourceGroupId;
	@Nonnull
	private final InstitutionId institutionId;
	@Nonnull
	private final String name;
	@Nonnull
	private final String urlName;
	@Nonnull
	private final String description;
	@Nonnull
	private final ColorId colorId;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface InstitutionResourceGroupApiResponseFactory {
		@Nonnull
		InstitutionResourceGroupApiResponse create(@Nonnull InstitutionResourceGroup institutionResourceGroup);
	}

	@AssistedInject
	public InstitutionResourceGroupApiResponse(@Nonnull Formatter formatter,
																						 @Nonnull Strings strings,
																						 @Assisted @Nonnull InstitutionResourceGroup institutionResourceGroup) {
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(institutionResourceGroup);

		this.institutionResourceGroupId = institutionResourceGroup.getInstitutionResourceGroupId();
		this.institutionId = institutionResourceGroup.getInstitutionId();
		this.name = institutionResourceGroup.getName();
		this.urlName = institutionResourceGroup.getUrlName();
		this.description = institutionResourceGroup.getDescription();
		this.colorId = institutionResourceGroup.getColorId();
	}

	@Nonnull
	public UUID getInstitutionResourceGroupId() {
		return this.institutionResourceGroupId;
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
	public String getUrlName() {
		return this.urlName;
	}

	@Nonnull
	public String getDescription() {
		return this.description;
	}

	@Nonnull
	public ColorId getColorId() {
		return this.colorId;
	}
}