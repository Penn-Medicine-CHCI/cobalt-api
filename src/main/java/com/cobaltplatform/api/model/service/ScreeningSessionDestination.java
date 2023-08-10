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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
public class ScreeningSessionDestination {
	@Nonnull
	private final ScreeningSessionDestinationId screeningSessionDestinationId;
	@Nonnull
	private final Map<String, Object> context;

	public enum ScreeningSessionDestinationId {
		CRISIS,
		ONE_ON_ONE_PROVIDER_LIST,
		CONTENT_LIST,
		GROUP_SESSION_LIST,
		MENTAL_HEALTH_PROVIDER_RECOMMENDATIONS,
		IC_PATIENT_SCREENING_SESSION_RESULTS,
		IC_MHIC_SCREENING_SESSION_RESULTS,
		IC_PATIENT_CLINICAL_SCREENING,
		IC_MHIC_CLINICAL_SCREENING,
		HOME
	}

	public ScreeningSessionDestination(@Nonnull ScreeningSessionDestinationId screeningSessionDestinationId) {
		this(screeningSessionDestinationId, null);
	}

	public ScreeningSessionDestination(@Nonnull ScreeningSessionDestinationId screeningSessionDestinationId,
																		 @Nullable Map<String, Object> context) {
		requireNonNull(screeningSessionDestinationId);

		this.screeningSessionDestinationId = screeningSessionDestinationId;
		this.context = context == null ? Collections.emptyMap() : new HashMap<>(context);
	}

	@Override
	public String toString() {
		return format("%s{screeningSessionDestinationId=%s, context=%s}", getClass().getSimpleName(), getScreeningSessionDestinationId(), getContext());
	}

	@Nonnull
	public ScreeningSessionDestinationId getScreeningSessionDestinationId() {
		return this.screeningSessionDestinationId;
	}

	@Nonnull
	public Map<String, Object> getContext() {
		return this.context;
	}
}
