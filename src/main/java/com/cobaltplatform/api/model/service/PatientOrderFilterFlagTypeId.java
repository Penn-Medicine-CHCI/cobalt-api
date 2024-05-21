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

/**
 * @author Transmogrify, LLC.
 */
public enum PatientOrderFilterFlagTypeId {
	NONE,
	PATIENT_NEVER_CONTACTED,
	PATIENT_BELOW_AGE_THRESHOLD,
	MOST_RECENT_EPISODE_CLOSED_WITHIN_DATE_THRESHOLD,
	NO_INTEREST,
	LOCATION_INVALID,
	INSURANCE_CHANGED_RECENTLY,
	INSURANCE_INVALID,
	CONSENT_REJECTED,
	NEEDS_SAFETY_PLANNING,
	NEEDS_RESOURCES,
	SESSION_ABANDONED,
	NEEDS_DOCUMENTATION
}
