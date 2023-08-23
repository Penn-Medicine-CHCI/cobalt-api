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
import com.cobaltplatform.api.model.db.InstitutionResource;
import com.cobaltplatform.api.util.Formatter;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lokalized.Strings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class InstitutionResourceApiResponse {
	@Nonnull
	private final UUID institutionResourceId;
	@Nonnull
	private final InstitutionId institutionId;
	@Nonnull
	private final String name;
	@Nonnull
	private final String urlName;
	@Nonnull
	private final String description;
	@Nullable
	private final String url;
	@Nullable
	private final String imageUrl;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface InstitutionResourceApiResponseFactory {
		@Nonnull
		InstitutionResourceApiResponse create(@Nonnull InstitutionResource institutionResource);
	}

	@AssistedInject
	public InstitutionResourceApiResponse(@Nonnull Formatter formatter,
																				@Nonnull Strings strings,
																				@Assisted @Nonnull InstitutionResource institutionResource) {
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(institutionResource);

		this.institutionResourceId = institutionResource.getInstitutionResourceId();
		this.institutionId = institutionResource.getInstitutionId();
		this.name = institutionResource.getName();
		this.urlName = institutionResource.getUrlName();
		this.imageUrl = institutionResource.getImageUrl();
		this.description = institutionResource.getDescription();
		this.url = institutionResource.getUrl();
	}

	@Nonnull
	public UUID getInstitutionResourceId() {
		return this.institutionResourceId;
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

	@Nullable
	public String getUrl() {
		return this.url;
	}

	@Nullable
	public String getImageUrl() {
		return this.imageUrl;
	}
}