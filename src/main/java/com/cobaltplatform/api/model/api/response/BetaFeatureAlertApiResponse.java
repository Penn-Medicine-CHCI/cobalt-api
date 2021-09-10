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
import com.cobaltplatform.api.model.db.BetaFeature.BetaFeatureId;
import com.cobaltplatform.api.model.db.BetaFeatureAlert;
import com.cobaltplatform.api.model.db.BetaFeatureAlert.BetaFeatureAlertStatusId;
import com.cobaltplatform.api.util.Formatter;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class BetaFeatureAlertApiResponse {
	@Nonnull
	private final BetaFeatureId betaFeatureId;
	@Nonnull
	private final BetaFeatureAlertStatusId betaFeatureAlertStatusId;
	@Nonnull
	private final String description;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface BetaFeatureAlertApiResponseFactory {
		@Nonnull
		BetaFeatureAlertApiResponse create(@Nonnull BetaFeatureAlert betaFeatureAlert);
	}

	@AssistedInject
	public BetaFeatureAlertApiResponse(@Nonnull Formatter formatter,
																		 @Nonnull Strings strings,
																		 @Assisted @Nonnull BetaFeatureAlert betaFeatureAlert) {
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(betaFeatureAlert);

		this.betaFeatureId = betaFeatureAlert.getBetaFeatureId();
		this.betaFeatureAlertStatusId = betaFeatureAlert.getBetaFeatureAlertStatusId();
		this.description = betaFeatureAlert.getDescription();
	}

	@Nonnull
	public BetaFeatureId getBetaFeatureId() {
		return betaFeatureId;
	}

	@Nonnull
	public BetaFeatureAlertStatusId getBetaFeatureAlertStatusId() {
		return betaFeatureAlertStatusId;
	}

	@Nonnull
	public String getDescription() {
		return description;
	}
}