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

package com.cobaltplatform.api.integration.hl7.model.type;

import ca.uhn.hl7v2.model.v251.datatype.FC;
import com.cobaltplatform.api.integration.hl7.model.Hl7Object;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * See https://hl7-definition.caristix.com/v2/hl7v2.5.1/Fields/PV1.20
 *
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class Hl7FinancialClass extends Hl7Object {
	@Nullable
	private String financialClassCode; // PV1.20.1 - Financial Class Code
	@Nullable
	private Hl7TimeStamp effectiveDate; // PV1.20.2 - Effective Date

	@Nonnull
	public static Boolean isPresent(@Nullable FC fc) {
		if (fc == null)
			return false;

		return trimToNull(fc.getFinancialClassCode().getValueOrEmpty()) != null
				|| Hl7TimeStamp.isPresent(fc.getEffectiveDate());
	}

	public Hl7FinancialClass() {
		// Nothing to do
	}

	public Hl7FinancialClass(@Nullable FC fc) {
		if (fc != null) {
			this.financialClassCode = trimToNull(fc.getFinancialClassCode().getValueOrEmpty());

			if (Hl7TimeStamp.isPresent(fc.getEffectiveDate()))
				this.effectiveDate = new Hl7TimeStamp(fc.getEffectiveDate());
		}
	}

	@Nullable
	public String getFinancialClassCode() {
		return this.financialClassCode;
	}

	public void setFinancialClassCode(@Nullable String financialClassCode) {
		this.financialClassCode = financialClassCode;
	}

	@Nullable
	public Hl7TimeStamp getEffectiveDate() {
		return this.effectiveDate;
	}

	public void setEffectiveDate(@Nullable Hl7TimeStamp effectiveDate) {
		this.effectiveDate = effectiveDate;
	}
}