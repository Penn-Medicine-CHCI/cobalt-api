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

package com.cobaltplatform.api.model.service;

import com.cobaltplatform.api.integration.mychart.MyChartAccessToken;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class MyChartAccessTokenWithClaims {
	@Nonnull
	private final MyChartAccessToken myChartAccessToken;
	@Nonnull
	private final Map<String, Object> claims;

	public MyChartAccessTokenWithClaims(@Nonnull MyChartAccessToken myChartAccessToken,
																			@Nonnull Map<String, Object> claims) {
		requireNonNull(myChartAccessToken);
		requireNonNull(claims);

		this.myChartAccessToken = myChartAccessToken;
		this.claims = claims == null ? Collections.emptyMap() : Collections.unmodifiableMap(new HashMap<>(claims));
	}

	@Override
	public String toString() {
		return format("%s{myChartAccessToken=%s, claims=%s}", getClass().getSimpleName(),
				getMyChartAccessToken(), getClaims());
	}

	@Nonnull
	public MyChartAccessToken getMyChartAccessToken() {
		return this.myChartAccessToken;
	}

	@Nonnull
	public Map<String, Object> getClaims() {
		return this.claims;
	}
}
