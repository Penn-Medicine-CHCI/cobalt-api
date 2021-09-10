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

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lokalized.Strings;
import com.cobaltplatform.api.model.db.ExternalGroupEventType;
import com.cobaltplatform.api.util.Formatter;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class ExternalGroupEventTypeApiResponse {
	@Nonnull
	private final UUID externalGroupEventTypeId;
	@Nonnull
	private final String name;
	@Nonnull
	private final String description;
	@Nonnull
	private final String urlName;
	@Nonnull
	private final String signupUrl;
	@Nonnull
	private final String imageUrl;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface ExternalGroupEventTypeApiResponseFactory {
		@Nonnull
		ExternalGroupEventTypeApiResponse create(@Nonnull ExternalGroupEventType externalGroupEventType);
	}

	@AssistedInject
	public ExternalGroupEventTypeApiResponse(@Nonnull Formatter formatter,
																					 @Nonnull Strings strings,
																					 @Assisted @Nonnull ExternalGroupEventType externalGroupEventType) {
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(externalGroupEventType);

		this.externalGroupEventTypeId = externalGroupEventType.getExternalGroupEventTypeId();
		this.name = externalGroupEventType.getName();
		this.description = externalGroupEventType.getDescription();
		this.urlName = externalGroupEventType.getUrlName();
		this.signupUrl = externalGroupEventType.getSignupUrl();
		this.imageUrl = externalGroupEventType.getImageUrl();
	}

	@Nonnull
	public UUID getExternalGroupEventTypeId() {
		return externalGroupEventTypeId;
	}

	@Nonnull
	public String getName() {
		return name;
	}

	@Nonnull
	public String getDescription() {
		return description;
	}

	@Nonnull
	public String getUrlName() {
		return urlName;
	}

	@Nonnull
	public String getSignupUrl() {
		return signupUrl;
	}

	@Nonnull
	public String getImageUrl() {
		return imageUrl;
	}
}
