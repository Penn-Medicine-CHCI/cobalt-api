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

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
public enum AdvisoryLock {
	WAY2HEALTH_INCIDENT_SYNCING(100),
	PROVIDER_AVAILABILITY_HISTORY_STORAGE(101),
	PATIENT_ORDER_BACKGROUND_TASK(102),
	EPIC_PROVIDER_AVAILABILITY_SYNC(103),
	EPIC_FHIR_PROVIDER_AVAILABILITY_SYNC(104),
	ANALYTICS_SYNC(105),
	PATIENT_ORDER_IMPORT_SYNC(106),
	DATA_SYNC(107);

	@Nonnull
	private final Integer key;

	AdvisoryLock(@Nonnull Integer key) {
		requireNonNull(key);
		this.key = key;
	}

	@Nonnull
	public Integer getKey() {
		return key;
	}
}
