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

package com.cobaltplatform.api.integration.way2health;

import com.cobaltplatform.api.integration.way2health.model.entity.Way2HealthError;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class Way2HealthResponseException extends Way2HealthException {
	@Nonnull
	private final Way2HealthError way2HealthError;
	@Nonnull
	private final String rawResponseBody;

	public Way2HealthResponseException(@Nonnull Way2HealthError way2HealthError,
																		 @Nonnull String rawResponseBody) {
		this(way2HealthError.getErrors() == null ? null :
				way2HealthError.getErrors().stream().collect(Collectors.joining(", ")), way2HealthError, rawResponseBody);
	}

	public Way2HealthResponseException(@Nullable String message,
																		 @Nonnull Way2HealthError way2HealthError,
																		 @Nonnull String rawResponseBody) {
		super(message);

		requireNonNull(way2HealthError);
		requireNonNull(rawResponseBody);

		this.way2HealthError = way2HealthError;
		this.rawResponseBody = rawResponseBody;
	}

	@Nonnull
	public Way2HealthError getWay2HealthError() {
		return way2HealthError;
	}

	@Nonnull
	public String getRawResponseBody() {
		return rawResponseBody;
	}
}
