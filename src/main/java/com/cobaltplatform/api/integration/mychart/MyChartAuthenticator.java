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

package com.cobaltplatform.api.integration.mychart;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

/**
 * @author Transmogrify, LLC.
 */
public interface MyChartAuthenticator {
	@Nonnull
	MyChartAccessToken obtainAccessTokenFromCode(@Nonnull String code) throws MyChartException;

	@Nonnull
	default String generateAuthorizationRedirectUrl() {
		return generateAuthorizationRedirectUrl(null, null);
	}

	@Nonnull
	default String generateAuthorizationRedirectUrl(@Nullable String state) {
		return generateAuthorizationRedirectUrl(state, null);
	}

	@Nonnull
	default String generateAuthorizationRedirectUrl(@Nullable Map<String, String> additionalParameters) {
		return generateAuthorizationRedirectUrl(null, additionalParameters);
	}

	@Nonnull
	String generateAuthorizationRedirectUrl(@Nullable String state,
																					@Nullable Map<String, String> additionalParameters);
}
