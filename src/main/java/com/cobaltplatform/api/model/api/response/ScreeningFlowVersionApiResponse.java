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

import com.cobaltplatform.api.model.db.ScreeningFlowVersion;
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
public class ScreeningFlowVersionApiResponse {
	@Nonnull
	private final UUID screeningFlowVersionId;
	@Nonnull
	private final UUID screeningFlowId;
	@Nonnull
	private final UUID initialScreeningId;
	@Nonnull
	private final Boolean phoneNumberRequired;
	@Nonnull
	private final Boolean skippable;
	@Nonnull
	private final Integer versionNumber;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface ScreeningFlowVersionApiResponseFactory {
		@Nonnull
		ScreeningFlowVersionApiResponse create(@Nonnull ScreeningFlowVersion screeningFlowVersion);
	}

	@AssistedInject
	public ScreeningFlowVersionApiResponse(@Nonnull Formatter formatter,
																				 @Nonnull Strings strings,
																				 @Assisted @Nonnull ScreeningFlowVersion screeningFlowVersion) {
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(screeningFlowVersion);

		this.screeningFlowVersionId = screeningFlowVersion.getScreeningFlowVersionId();
		this.screeningFlowId = screeningFlowVersion.getScreeningFlowId();
		this.initialScreeningId = screeningFlowVersion.getInitialScreeningId();
		this.phoneNumberRequired = screeningFlowVersion.getPhoneNumberRequired();
		this.skippable = screeningFlowVersion.getSkippable();
		this.versionNumber = screeningFlowVersion.getVersionNumber();
	}

	@Nonnull
	public UUID getScreeningFlowVersionId() {
		return this.screeningFlowVersionId;
	}

	@Nonnull
	public UUID getScreeningFlowId() {
		return this.screeningFlowId;
	}

	@Nonnull
	public UUID getInitialScreeningId() {
		return this.initialScreeningId;
	}

	@Nonnull
	public Boolean getPhoneNumberRequired() {
		return this.phoneNumberRequired;
	}

	@Nonnull
	public Boolean getSkippable() {
		return this.skippable;
	}

	@Nonnull
	public Integer getVersionNumber() {
		return this.versionNumber;
	}
}