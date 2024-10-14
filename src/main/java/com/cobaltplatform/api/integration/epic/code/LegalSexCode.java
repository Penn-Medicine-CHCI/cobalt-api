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
public enum LegalSexCode {
	MALE("male"),
	FEMALE("female"),
	UNDIFFERENTIATED("undifferentiated");

	@Nonnull
	public static final String EXTENSION_URL;
	@Nonnull
	private static final Map<String, LegalSexCode> LEGAL_SEX_CODES_BY_FHIR_VALUE;

	@Nonnull
	private final String fhirValue;

	static {
		EXTENSION_URL = "http://open.epic.com/FHIR/StructureDefinition/extension/legal-sex";

		Map<String, LegalSexCode> legalSexCodesByFhirValue = new HashMap<>();

		for (LegalSexCode legalSexCode : LegalSexCode.values())
			legalSexCodesByFhirValue.put(legalSexCode.getFhirValue(), legalSexCode);

		LEGAL_SEX_CODES_BY_FHIR_VALUE = Collections.unmodifiableMap(legalSexCodesByFhirValue);
	}

	private LegalSexCode(@Nonnull String fhirValue) {
		requireNonNull(fhirValue);
		this.fhirValue = fhirValue;
	}

	@Nonnull
	public String getFhirValue() {
		return this.fhirValue;
	}

	@Nonnull
	public static Optional<LegalSexCode> fromFhirValue(@Nullable String fhirValue) {
		fhirValue = trimToNull(fhirValue);
		return Optional.ofNullable(LEGAL_SEX_CODES_BY_FHIR_VALUE.get(fhirValue));
	}
}
