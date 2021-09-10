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

package com.cobaltplatform.api.integration.epic.matching;

import javax.annotation.Nonnull;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
public enum EpicPatientMatchRule {
	EXACT_NAME(10),
	EXACT_NAME_WITHOUT_MIDDLE_INITIAL(9),
	EXACT_SEX(1),
	UNKNOWN_SEX(0),
	EXACT_DOB(7),
	DOB_ONE_DIGIT_DIFFERENCE(4),
	UNKNOWN_DOB(0),
	EXACT_SSN(5),
	SSN_LAST_4(4), // Custom rule for Cobalt
	SSN_ONE_DIGIT_DIFFERENCE(3),
	UNKNOWN_SSN(0),
	EXACT_ADDRESS(3),
	SIMILAR_ADDRESS(2),
	EXACT_PHONE_NUMBER(2),
	SIMILAR_PHONE_NUMBER(1),
	EXACT_EMAIL(2),
	SIMILAR_EMAIL(0);

	@Nonnull
	private final Integer weight;

	EpicPatientMatchRule(@Nonnull Integer weight) {
		requireNonNull(weight);
		this.weight = weight;
	}

	@Nonnull
	public Integer getWeight() {
		return weight;
	}
}
