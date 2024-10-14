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
public enum PreferredPronounCode {
	// See https://loinc.org/LL5144-2
	// http://open.epic.com/FHIR/StructureDefinition/extension/calculated-pronouns-to-use-for-text
	HE_HIM_HIS_HIS_HIMSELF("LA29518-0", "he/him/his/his/himself"),
	SHE_HER_HER_HERS_HERSELF("LA29519-8", "she/her/her/hers/herself"),
	THEY_THEM_THEIR_THEIRS_THEMSELVES("LA29520-6", "they/them/their/theirs/themselves"),
	ZE_ZIR_ZIR_ZIRS_ZIRSELF("LA29523-0", "ze/zir/zir/zirs/zirself"),
	XIE_HIR_HERE_HIR_HIRS_HIRSELF("LA29521-4", "xie/hir (\"here\")/hir/hirs/hirself"),
	CO_CO_COS_COS_COSELF("LA29515-6", "co/co/cos/cos/coself"),
	EN_EN_ENS_ENS_ENSELF("LA29516-4", "en/en/ens/ens/enself"),
	EY_EM_EIR_EIRS_EMSELF("LA29517-2", "ey/em/eir/eirs/emself"),
	YO_YO_YOS_YOS_YOSELF("LA29522-2", "yo/yo/yos/yos/yoself"),
	VE_VIS_VER_VER_VERSELF("LA29524-8", "ve/vis/ver/ver/verself");

	@Nonnull
	public static final String EXTENSION_URL;
	@Nonnull
	private static final Map<String, PreferredPronounCode> PREFERRED_PRONOUN_CODES_BY_FHIR_VALUE;

	@Nonnull
	private final String fhirValue;
	@Nonnull
	private final String display;

	static {
		EXTENSION_URL = "http://open.epic.com/FHIR/StructureDefinition/extension/calculated-pronouns-to-use-for-text";

		Map<String, PreferredPronounCode> preferredPronounCodesByFhirValue = new HashMap<>();

		for (PreferredPronounCode preferredPronounCode : PreferredPronounCode.values())
			preferredPronounCodesByFhirValue.put(preferredPronounCode.getFhirValue(), preferredPronounCode);

		PREFERRED_PRONOUN_CODES_BY_FHIR_VALUE = Collections.unmodifiableMap(preferredPronounCodesByFhirValue);
	}

	private PreferredPronounCode(@Nonnull String fhirValue,
															 @Nonnull String display) {
		requireNonNull(fhirValue);
		requireNonNull(display);

		this.fhirValue = fhirValue;
		this.display = display;
	}

	@Nonnull
	public String getFhirValue() {
		return this.fhirValue;
	}

	@Nonnull
	public String getDisplay() {
		return this.display;
	}

	@Nonnull
	public static Optional<PreferredPronounCode> fromFhirValue(@Nullable String fhirValue) {
		fhirValue = trimToNull(fhirValue);
		return Optional.ofNullable(PREFERRED_PRONOUN_CODES_BY_FHIR_VALUE.get(fhirValue));
	}
}
