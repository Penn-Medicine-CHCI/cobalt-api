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

import com.cobaltplatform.api.model.db.Feature;
import com.cobaltplatform.api.model.db.Feature.FeatureId;
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
public class FeatureApiResponse {
	@Nonnull
	private final FeatureId featureId;
	@Nonnull
	private final String name;
	@Nonnull
	private final String description;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface FeatureApiResponseFactory {
		@Nonnull
		FeatureApiResponse create(@Nonnull Feature feature);
	}

	@AssistedInject
	public FeatureApiResponse(@Assisted @Nonnull Feature feature) {
		requireNonNull(feature);

		this.featureId = feature.getFeatureId();
		this.name = feature.getName();
		this.description = feature.getDescription();
	}

	@Nonnull
	public FeatureId getFeatureId() {
		return featureId;
	}

	@Nonnull
	public String getName() {
		return name;
	}

	@Nonnull
	public String getDescription() {
		return description;
	}
}