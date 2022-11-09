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

package com.cobaltplatform.api.integration.epic.code;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;


/**
 * @author Transmogrify, LLC.
 */
public enum RaceCode {
	// http://hl7.org/fhir/us/core/STU5/StructureDefinition-us-core-race.html
	AMERICAN_INDIAN_OR_ALASKA_NATIVE("American Indian or Alaska Native"),
	ASIAN("Asian"),
	BLACK_OR_AFRICAN_AMERICAN("Black or African American"),
	NATIVE_HAWAIIAN_OR_PACIFIC_ISLANDER("Native Hawaiian or Other Pacific Islander"),
	WHITE("White");

	@Nonnull
	public static final String EXTENSION_URL;
	@Nonnull
	private static final Map<String, RaceCode> RACE_CODES_BY_FHIR_VALUE;

	@Nonnull
	private final String fhirValue;

	static {
		EXTENSION_URL = "http://hl7.org/fhir/us/core/StructureDefinition/us-core-race";

		Map<String, RaceCode> raceCodesByFhirValue = new HashMap<>();

		for (RaceCode raceCode : RaceCode.values())
			raceCodesByFhirValue.put(raceCode.getFhirValue(), raceCode);

		RACE_CODES_BY_FHIR_VALUE = Collections.unmodifiableMap(raceCodesByFhirValue);
	}

	private RaceCode(@Nonnull String fhirValue) {
		requireNonNull(fhirValue);
		this.fhirValue = fhirValue;
	}

	@Nonnull
	public String getFhirValue() {
		return this.fhirValue;
	}

	@Nonnull
	public static Optional<RaceCode> fromFhirValue(@Nullable String fhirValue) {
		fhirValue = trimToNull(fhirValue);
		return Optional.ofNullable(RACE_CODES_BY_FHIR_VALUE.get(fhirValue));
	}
}
