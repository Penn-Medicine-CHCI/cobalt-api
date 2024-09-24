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
import com.cobaltplatform.api.model.db.ContentAudienceType;
import com.cobaltplatform.api.model.db.ContentAudienceType.ContentAudienceTypeId;
import com.cobaltplatform.api.model.db.ContentAudienceTypeGroup.ContentAudienceTypeGroupId;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Immutable
public class ContentAudienceTypeApiResponse {
	@Nonnull
	private final ContentAudienceTypeId contentAudienceTypeId;
	@Nonnull
	private final ContentAudienceTypeGroupId contentAudienceTypeGroupId;
	@Nonnull
	private final String description;
	@Nonnull
	private final String patientRepresentation;
	@Nonnull
	private final String urlName;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface ContentAudienceTypeApiResponseFactory {
		@Nonnull
		ContentAudienceTypeApiResponse create(@Nonnull ContentAudienceType contentAudienceType);
	}

	@AssistedInject
	public ContentAudienceTypeApiResponse(@Nonnull Provider<CurrentContext> currentContextProvider,
																				@Assisted @Nonnull ContentAudienceType contentAudienceType) {
		requireNonNull(currentContextProvider);
		requireNonNull(contentAudienceType);

		this.contentAudienceTypeId = contentAudienceType.getContentAudienceTypeId();
		this.contentAudienceTypeGroupId = contentAudienceType.getContentAudienceTypeGroupId();
		this.description = contentAudienceType.getDescription();
		this.patientRepresentation = contentAudienceType.getPatientRepresentation();
		this.urlName = contentAudienceType.getUrlName();
	}

	@Nonnull
	public ContentAudienceTypeId getContentAudienceTypeId() {
		return this.contentAudienceTypeId;
	}

	@Nonnull
	public ContentAudienceTypeGroupId getContentAudienceTypeGroupId() {
		return this.contentAudienceTypeGroupId;
	}

	@Nonnull
	public String getDescription() {
		return this.description;
	}

	@Nonnull
	public String getPatientRepresentation() {
		return this.patientRepresentation;
	}

	@Nonnull
	public String getUrlName() {
		return this.urlName;
	}
}