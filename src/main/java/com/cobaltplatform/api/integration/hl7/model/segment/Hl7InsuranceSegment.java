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

package com.cobaltplatform.api.integration.hl7.model.segment;

import ca.uhn.hl7v2.model.v251.segment.IN1;
import com.cobaltplatform.api.integration.hl7.model.Hl7Object;
import com.cobaltplatform.api.integration.hl7.model.type.Hl7CodedElement;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * See https://hl7-definition.caristix.com/v2/hl7v2.5.1/Segments/IN1
 *
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class Hl7InsuranceSegment extends Hl7Object {
	@Nullable
	private Integer setId; // IN1.1 - Set ID
	@Nullable
	private Hl7CodedElement insurancePlanId; // IN1.2 - Insurance Plan ID

	@Nonnull
	public static Boolean isPresent(@Nullable IN1 in1) {
		if (in1 == null)
			return false;

		return trimToNull(in1.getSetIDIN1().getValue()) != null;
	}

	public Hl7InsuranceSegment() {
		// Nothing to do
	}

	public Hl7InsuranceSegment(@Nullable IN1 in1) {
		if (in1 != null) {
			String setIdAsString = trimToNull(in1.getSetIDIN1().getValue());
			if (setIdAsString != null)
				this.setId = Integer.parseInt(setIdAsString, 10);

			if (Hl7CodedElement.isPresent(in1.getInsurancePlanID()))
				this.insurancePlanId = new Hl7CodedElement(in1.getInsurancePlanID());
		}
	}
}