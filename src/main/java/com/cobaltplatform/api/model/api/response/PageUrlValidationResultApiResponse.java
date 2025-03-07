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

import com.cobaltplatform.api.model.service.PageUrlValidationResult;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class PageUrlValidationResultApiResponse {
	@Nonnull
	private final Boolean available;
	@Nonnull
	private final String recommendation;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface PageAutocompleteResultApiResponseFactory {
		@Nonnull
		PageUrlValidationResultApiResponse create(@Nonnull PageUrlValidationResult result);
	}

	@AssistedInject
	public PageUrlValidationResultApiResponse(@Assisted @Nonnull PageUrlValidationResult result) {

		this.available = result.getAvailable();
		this.recommendation = result.getRecommendation();
	}

	@Nonnull
	public Boolean getAvailable() {
		return available;
	}

	@Nonnull
	public String getRecommendation() {
		return recommendation;
	}
}