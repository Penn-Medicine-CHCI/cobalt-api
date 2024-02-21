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

import ca.uhn.hl7v2.model.v251.datatype.SPS;
import com.cobaltplatform.api.integration.hl7.model.Hl7Object;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * See https://hl7-definition.caristix.com/v2/HL7v2.5.1/DataTypes/SPS
 *
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class Hl7SpecimenSource extends Hl7Object {
	@Nullable
	private Hl7CodedWithExceptions specimenSourceNameOrCode; // SPS.1 - Specimen Source Name Or Code
	@Nullable
	private Hl7CodedWithExceptions additives; // SPS.2 - Additives
	@Nullable
	private String specimenCollectionMethod; // SPS.3 - Specimen Collection Method
	@Nullable
	private Hl7CodedWithExceptions bodySite; // SPS.4 - Body Site
	@Nullable
	private Hl7CodedWithExceptions siteModifier; // SPS.5 - Site Modifier
	@Nullable
	private Hl7CodedWithExceptions collectionMethodModifierCode; // SPS.6 - Collection Method Modifier Code
	@Nullable
	private Hl7CodedWithExceptions specimenRole; // SPS.7 - Specimen Role

	@Nonnull
	public static Boolean isPresent(@Nullable SPS sps) {
		if (sps == null)
			return false;

		return Hl7CodedWithExceptions.isPresent(sps.getSpecimenSourceNameOrCode())
				|| Hl7CodedWithExceptions.isPresent(sps.getAdditives())
				|| trimToNull(sps.getSpecimenCollectionMethod().getValue()) != null
				|| Hl7CodedWithExceptions.isPresent(sps.getBodySite())
				|| Hl7CodedWithExceptions.isPresent(sps.getSiteModifier())
				|| Hl7CodedWithExceptions.isPresent(sps.getCollectionMethodModifierCode())
				|| Hl7CodedWithExceptions.isPresent(sps.getSpecimenRole());
	}

	public Hl7SpecimenSource() {
		// Nothing to do
	}

	public Hl7SpecimenSource(@Nullable SPS sps) {
		if (sps != null) {
			if (Hl7CodedWithExceptions.isPresent(sps.getSpecimenSourceNameOrCode()))
				this.specimenSourceNameOrCode = new Hl7CodedWithExceptions(sps.getSpecimenSourceNameOrCode());

			if (Hl7CodedWithExceptions.isPresent(sps.getAdditives()))
				this.additives = new Hl7CodedWithExceptions(sps.getAdditives());

			this.specimenCollectionMethod = trimToNull(sps.getSpecimenCollectionMethod().getValue());

			if (Hl7CodedWithExceptions.isPresent(sps.getBodySite()))
				this.bodySite = new Hl7CodedWithExceptions(sps.getBodySite());

			if (Hl7CodedWithExceptions.isPresent(sps.getSiteModifier()))
				this.siteModifier = new Hl7CodedWithExceptions(sps.getSiteModifier());

			if (Hl7CodedWithExceptions.isPresent(sps.getCollectionMethodModifierCode()))
				this.collectionMethodModifierCode = new Hl7CodedWithExceptions(sps.getCollectionMethodModifierCode());

			if (Hl7CodedWithExceptions.isPresent(sps.getSpecimenRole()))
				this.specimenRole = new Hl7CodedWithExceptions(sps.getSpecimenRole());
		}
	}
}