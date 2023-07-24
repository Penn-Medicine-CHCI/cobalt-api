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
public enum BirthSexCode {
	// https://build.fhir.org/ig/HL7/US-Core/ValueSet-birthsex.html
	FEMALE("F"),
	MALE("M"),
	ASKED_BUT_UNKNOWN("ASKU"),
	OTHER("OTH"),
	UNKNOWN("UNK");

	@Nonnull
	public static final String EXTENSION_URL;
	@Nonnull
	public static final String DSTU2_EXTENSION_URL;
	@Nonnull
	private static final Map<String, BirthSexCode> BIRTH_SEX_CODES_BY_FHIR_VALUE;

	@Nonnull
	private final String fhirValue;

	static {
		EXTENSION_URL = "http://hl7.org/fhir/us/core/StructureDefinition/us-core-birthsex";
		DSTU2_EXTENSION_URL = "http://hl7.org/fhir/StructureDefinition/us-core-birth-sex";

		Map<String, BirthSexCode> birthSexCodesByFhirValue = new HashMap<>();

		for (BirthSexCode birthSexCode : BirthSexCode.values())
			birthSexCodesByFhirValue.put(birthSexCode.getFhirValue(), birthSexCode);

		BIRTH_SEX_CODES_BY_FHIR_VALUE = Collections.unmodifiableMap(birthSexCodesByFhirValue);
	}

	private BirthSexCode(@Nonnull String fhirValue) {
		requireNonNull(fhirValue);
		this.fhirValue = fhirValue;
	}

	@Nonnull
	public String getFhirValue() {
		return this.fhirValue;
	}

	@Nonnull
	public static Optional<BirthSexCode> fromFhirValue(@Nullable String fhirValue) {
		fhirValue = trimToNull(fhirValue);
		return Optional.ofNullable(BIRTH_SEX_CODES_BY_FHIR_VALUE.get(fhirValue));
	}

	@Nonnull
	public String getDstu2Value() {
		return this.fhirValue;
	}

	@Nonnull
	public static Optional<BirthSexCode> fromDstu2Value(@Nullable String dstu2Value) {
		dstu2Value = trimToNull(dstu2Value);
		return Optional.ofNullable(BIRTH_SEX_CODES_BY_FHIR_VALUE.get(dstu2Value));
	}
}
