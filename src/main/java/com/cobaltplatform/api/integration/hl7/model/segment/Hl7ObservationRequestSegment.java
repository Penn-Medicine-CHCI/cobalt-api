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

import ca.uhn.hl7v2.model.v251.segment.OBR;
import com.cobaltplatform.api.integration.hl7.model.Hl7Object;
import com.cobaltplatform.api.integration.hl7.model.type.Hl7CodedElement;
import com.cobaltplatform.api.integration.hl7.model.type.Hl7CompositeQuantityWithUnits;
import com.cobaltplatform.api.integration.hl7.model.type.Hl7EntityIdentifier;
import com.cobaltplatform.api.integration.hl7.model.type.Hl7ExtendedCompositeIdNumberAndNameForPersons;
import com.cobaltplatform.api.integration.hl7.model.type.Hl7SpecimenSource;
import com.cobaltplatform.api.integration.hl7.model.type.Hl7TimeStamp;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * See https://hl7-definition.caristix.com/v2/hl7v2.5.1/Segments/OBR
 *
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class Hl7ObservationRequestSegment extends Hl7Object {
	@Nullable
	private Integer setId; // OBR.1 - Set ID - OBR
	@Nullable
	private Hl7EntityIdentifier placerOrderNumber; // OBR.2 - Placer Order Number
	@Nullable
	private Hl7EntityIdentifier fillerOrderNumber; /// OBR.3 - Filler Order Number
	@Nullable
	private Hl7CodedElement universalServiceIdentifier; // OBR.4 - Universal Service Identifier
	@Nullable
	private String priority; // OBR.5 - Priority - OBR
	@Nullable
	private Hl7TimeStamp requestedDateTime; // OBR.6 - Requested Date/Time
	@Nullable
	private Hl7TimeStamp observationDateTime; // OBR.7 - Observation Date/Time
	@Nullable
	private Hl7TimeStamp observationEndDateTime; // OBR.8 - Observation End Date/Time
	@Nullable
	private Hl7CompositeQuantityWithUnits collectionVolume; // OBR.9 - Collection Volume
	@Nullable
	private List<Hl7ExtendedCompositeIdNumberAndNameForPersons> collectorIdentifier; // OBR.10 - Collector Identifier
	@Nullable
	private String specimenActionCode; // OBR.11 - Specimen Action Code
	@Nullable
	private Hl7CodedElement dangerCode; // OBR.12 - Danger Code
	@Nullable
	private String relevantClinicalInformation; // OBR.13 - Relevant Clinical Information
	@Nullable
	private Hl7TimeStamp specimenReceivedDateTime; // OBR.14 - Specimen Received Date/Time
	@Nullable
	private Hl7SpecimenSource specimenSource; // OBR.15 - Specimen Source

	@Nonnull
	public static Boolean isPresent(@Nullable OBR obr) {
		if (obr == null)
			return false;

		return Hl7CodedElement.isPresent(obr.getUniversalServiceIdentifier());
	}

	public Hl7ObservationRequestSegment() {
		// Nothing to do
	}

	public Hl7ObservationRequestSegment(@Nullable OBR obr) {
		if (obr != null) {
			String setIdAsString = trimToNull(obr.getSetIDOBR().getValue());
			if (setIdAsString != null)
				this.setId = Integer.parseInt(setIdAsString, 10);

			if (Hl7EntityIdentifier.isPresent(obr.getPlacerOrderNumber()))
				this.placerOrderNumber = new Hl7EntityIdentifier(obr.getPlacerOrderNumber());

			if (Hl7EntityIdentifier.isPresent(obr.getFillerOrderNumber()))
				this.fillerOrderNumber = new Hl7EntityIdentifier(obr.getFillerOrderNumber());

			if (Hl7CodedElement.isPresent(obr.getUniversalServiceIdentifier()))
				this.universalServiceIdentifier = new Hl7CodedElement(obr.getUniversalServiceIdentifier());

			this.priority = trimToNull(obr.getPriorityOBR().getValueOrEmpty());

			if (Hl7TimeStamp.isPresent(obr.getRequestedDateTime()))
				this.requestedDateTime = new Hl7TimeStamp(obr.getRequestedDateTime());

			if (Hl7TimeStamp.isPresent(obr.getObservationDateTime()))
				this.observationDateTime = new Hl7TimeStamp(obr.getObservationDateTime());

			if (Hl7TimeStamp.isPresent(obr.getObservationEndDateTime()))
				this.observationEndDateTime = new Hl7TimeStamp(obr.getObservationEndDateTime());

			if (Hl7CompositeQuantityWithUnits.isPresent(obr.getCollectionVolume()))
				this.collectionVolume = new Hl7CompositeQuantityWithUnits(obr.getCollectionVolume());

			if (obr.getCollectorIdentifier() != null && obr.getCollectorIdentifier().length > 0)
				this.collectorIdentifier = Arrays.stream(obr.getCollectorIdentifier())
						.map(xcn -> Hl7ExtendedCompositeIdNumberAndNameForPersons.isPresent(xcn) ? new Hl7ExtendedCompositeIdNumberAndNameForPersons(xcn) : null)
						.filter(collectorIdentifier -> collectorIdentifier != null)
						.collect(Collectors.toList());

			this.specimenActionCode = trimToNull(obr.getSpecimenActionCode().getValueOrEmpty());

			if (Hl7CodedElement.isPresent(obr.getDangerCode()))
				this.dangerCode = new Hl7CodedElement(obr.getDangerCode());

			this.relevantClinicalInformation = trimToNull(obr.getRelevantClinicalInformation().getValueOrEmpty());

			if (Hl7TimeStamp.isPresent(obr.getSpecimenReceivedDateTime()))
				this.specimenReceivedDateTime = new Hl7TimeStamp(obr.getSpecimenReceivedDateTime());

			if (Hl7SpecimenSource.isPresent(obr.getSpecimenSource()))
				this.specimenSource = new Hl7SpecimenSource(obr.getSpecimenSource());
		}
	}
}