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

import com.cobaltplatform.api.model.db.ScreeningType.ScreeningTypeId;
import com.cobaltplatform.api.model.db.ScreeningVersion;
import com.cobaltplatform.api.util.Formatter;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lokalized.Strings;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.time.Instant;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class ScreeningVersionApiResponse {
	@Nonnull
	private final UUID screeningVersionId;
	@Nonnull
	private final UUID screeningId;
	@Nonnull
	private final ScreeningTypeId screeningTypeId;
	@Nonnull
	private final Integer versionNumber;
	@Nonnull
	private final String versionNumberDescription;
	@Nonnull
	private final Instant created;
	@Nonnull
	private final String createdDescription;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface ScreeningVersionApiResponseFactory {
		@Nonnull
		ScreeningVersionApiResponse create(@Nonnull ScreeningVersion screeningVersion);
	}

	@AssistedInject
	public ScreeningVersionApiResponse(@Nonnull Formatter formatter,
																		 @Nonnull Strings strings,
																		 @Assisted @Nonnull ScreeningVersion screeningVersion) {
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(screeningVersion);

		this.screeningVersionId = screeningVersion.getScreeningVersionId();
		this.screeningId = screeningVersion.getScreeningId();
		this.screeningTypeId = screeningVersion.getScreeningTypeId();
		this.versionNumber = screeningVersion.getVersionNumber();
		this.versionNumberDescription = formatter.formatNumber(screeningVersion.getVersionNumber());
		this.created = screeningVersion.getCreated();
		this.createdDescription = formatter.formatTimestamp(screeningVersion.getCreated());
	}

	@Nonnull
	public UUID getScreeningVersionId() {
		return this.screeningVersionId;
	}

	@Nonnull
	public UUID getScreeningId() {
		return this.screeningId;
	}

	@Nonnull
	public ScreeningTypeId getScreeningTypeId() {
		return this.screeningTypeId;
	}

	@Nonnull
	public Integer getVersionNumber() {
		return this.versionNumber;
	}

	@Nonnull
	public String getVersionNumberDescription() {
		return this.versionNumberDescription;
	}

	@Nonnull
	public Instant getCreated() {
		return this.created;
	}

	@Nonnull
	public String getCreatedDescription() {
		return this.createdDescription;
	}
}