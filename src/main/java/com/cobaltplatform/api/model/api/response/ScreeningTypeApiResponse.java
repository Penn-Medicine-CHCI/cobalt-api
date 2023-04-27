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

import com.cobaltplatform.api.model.db.ScreeningType;
import com.cobaltplatform.api.model.db.ScreeningType.ScreeningTypeId;
import com.cobaltplatform.api.util.Formatter;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lokalized.Strings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class ScreeningTypeApiResponse {
	@Nonnull
	private final ScreeningTypeId screeningTypeId;
	@Nonnull
	private final String description;
	@Nullable
	private final Integer overallScoreMaximum;
	@Nullable
	private final String overallScoreMaximumDescription;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface ScreeningTypeApiResponseFactory {
		@Nonnull
		ScreeningTypeApiResponse create(@Nonnull ScreeningType screeningType);
	}

	@AssistedInject
	public ScreeningTypeApiResponse(@Nonnull Formatter formatter,
																	@Nonnull Strings strings,
																	@Assisted @Nonnull ScreeningType screeningType) {
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(screeningType);

		this.screeningTypeId = screeningType.getScreeningTypeId();
		this.description = screeningType.getDescription();
		this.overallScoreMaximum = screeningType.getOverallScoreMaximum();
		this.overallScoreMaximumDescription = screeningType.getOverallScoreMaximum() == null ? null : formatter.formatNumber(screeningType.getOverallScoreMaximum());
	}

	@Nonnull
	public ScreeningTypeId getScreeningTypeId() {
		return this.screeningTypeId;
	}

	@Nonnull
	public String getDescription() {
		return this.description;
	}

	@Nonnull
	public Optional<Integer> getOverallScoreMaximum() {
		return Optional.ofNullable(this.overallScoreMaximum);
	}

	@Nonnull
	public Optional<String> getOverallScoreMaximumDescription() {
		return Optional.ofNullable(this.overallScoreMaximumDescription);
	}
}