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

package com.cobaltplatform.api.integration.enterprise;

import com.cobaltplatform.api.integration.epic.EpicClient;
import com.cobaltplatform.api.integration.mychart.MyChartAccessToken;
import com.cobaltplatform.api.integration.mychart.MyChartAuthenticator;
import com.cobaltplatform.api.messaging.email.EmailMessage;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.service.CallToAction;
import com.cobaltplatform.api.model.service.CallToActionDisplayAreaId;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
public interface EnterprisePlugin {
	@Nonnull
	InstitutionId getInstitutionId();

	@Nonnull
	default EmailMessage customizeEmailMessage(@Nonnull EmailMessage emailMessage) {
		// No customization by default
		return emailMessage;
	}

	@Nonnull
	default Optional<String> federatedLogoutUrl(@Nullable Account account) {
		return Optional.empty();
	}

	@Nonnull
	default List<CallToAction> determineCallsToAction(@Nullable Account account,
																										@Nullable CallToActionDisplayAreaId callToActionDisplayAreaId) {
		return Collections.emptyList();
	}

	@Nonnull
	default Optional<EpicClient> epicClient() {
		return Optional.empty();
	}

	@Nonnull
	default Optional<MyChartAuthenticator> myChartAuthenticator() {
		return Optional.empty();
	}

	@Nonnull
	default Optional<String> extractPatientIdFromMyChartAccessToken(@Nullable MyChartAccessToken myChartAccessToken) {
		if (myChartAccessToken == null)
			return Optional.empty();
		
		return Optional.ofNullable((String) myChartAccessToken.getMetadata().get("patient"));
	}

	@Nonnull
	default Boolean isInstantWithinBusinessHours(@Nonnull Instant instant) {
		requireNonNull(instant);
		return instant.equals(nextInstantWithinBusinessHours(instant));
	}

	@Nonnull
	default Instant nextInstantWithinBusinessHours(@Nonnull Instant instant) {
		requireNonNull(instant);
		return instant;
	}
}
