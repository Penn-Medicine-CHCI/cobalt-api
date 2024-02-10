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

import ca.uhn.hl7v2.model.v251.datatype.OSD;
import com.cobaltplatform.api.integration.hl7.model.Hl7Object;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * See https://hl7-definition.caristix.com/v2/hl7v2.5.1/DataTypes/OSD
 *
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class Hl7OrderSequenceDefinition extends Hl7Object {
	@Nullable
	private String sequenceResultsFlag; // OSD.1 - Sequence/Results Flag
	@Nullable
	private String placerOrderNumberEntityIdentifier; // OSD.2 - Placer Order Number: Entity Identifier
	@Nullable
	private String placerOrderNumberNamespaceId; // OSD.3 - Placer Order Number: Namespace Id
	@Nullable
	private String fillerOrderNumberEntityIdentifier; // OSD.4 - Filler Order Number: Entity Identifier
	@Nullable
	private String fillerOrderNumberNamespaceId; //OSD.5 - Filler Order Number: Namespace Id
	@Nullable
	private String sequenceConditionValue; // OSD.6 - Sequence Condition Value
	@Nullable
	private Double maximumNumberOfRepeats; // OSD.7 - Maximum Number Of Repeats
	@Nullable
	private String placerOrderNumberUniversalId; // OSD.8 - Placer Order Number: Universal Id
	@Nullable
	private String placerOrderNumberUniversalIdType; // OSD.9 - Placer Order Number: Universal Id Type
	@Nullable
	private String fillerOrderNumberUniversalId; // OSD.10 - Filler Order Number: Universal Id
	@Nullable
	private String fillerOrderNumberUniversalIdType; // OSD.11 - Filler Order Number: Universal Id Type

	@Nonnull
	public static Boolean isPresent(@Nullable OSD osd) {
		if (osd == null)
			return false;

		return trimToNull(osd.getSequenceResultsFlag().getValueOrEmpty()) != null
				|| trimToNull(osd.getPlacerOrderNumberEntityIdentifier().getValueOrEmpty()) != null
				|| trimToNull(osd.getPlacerOrderNumberNamespaceID().getValueOrEmpty()) != null
				|| trimToNull(osd.getFillerOrderNumberEntityIdentifier().getValueOrEmpty()) != null
				|| trimToNull(osd.getFillerOrderNumberNamespaceID().getValueOrEmpty()) != null
				|| trimToNull(osd.getSequenceConditionValue().getValueOrEmpty()) != null
				|| trimToNull(osd.getMaximumNumberOfRepeats().getValue()) != null
				|| trimToNull(osd.getPlacerOrderNumberUniversalID().getValueOrEmpty()) != null
				|| trimToNull(osd.getPlacerOrderNumberUniversalIDType().getValueOrEmpty()) != null
				|| trimToNull(osd.getFillerOrderNumberUniversalID().getValueOrEmpty()) != null
				|| trimToNull(osd.getFillerOrderNumberUniversalIDType().getValueOrEmpty()) != null;
	}

	public Hl7OrderSequenceDefinition() {
		// Nothing to do
	}

	public Hl7OrderSequenceDefinition(@Nullable OSD osd) {
		if (osd != null) {
			this.sequenceResultsFlag = trimToNull(osd.getSequenceResultsFlag().getValueOrEmpty());
			this.placerOrderNumberEntityIdentifier = trimToNull(osd.getPlacerOrderNumberEntityIdentifier().getValueOrEmpty());
			this.placerOrderNumberNamespaceId = trimToNull(osd.getPlacerOrderNumberNamespaceID().getValueOrEmpty());
			this.fillerOrderNumberEntityIdentifier = trimToNull(osd.getFillerOrderNumberEntityIdentifier().getValueOrEmpty());
			this.fillerOrderNumberNamespaceId = trimToNull(osd.getFillerOrderNumberNamespaceID().getValueOrEmpty());
			this.sequenceConditionValue = trimToNull(osd.getSequenceConditionValue().getValueOrEmpty());

			String maximumNumberOfRepeatsAsString = trimToNull(osd.getMaximumNumberOfRepeats().getValue());

			if (maximumNumberOfRepeatsAsString != null)
				this.maximumNumberOfRepeats = Double.valueOf(maximumNumberOfRepeatsAsString);

			this.placerOrderNumberUniversalId = trimToNull(osd.getPlacerOrderNumberUniversalID().getValueOrEmpty());
			this.placerOrderNumberUniversalIdType = trimToNull(osd.getPlacerOrderNumberUniversalIDType().getValueOrEmpty());
			this.fillerOrderNumberUniversalId = trimToNull(osd.getFillerOrderNumberUniversalID().getValueOrEmpty());
			this.fillerOrderNumberUniversalIdType = trimToNull(osd.getFillerOrderNumberUniversalIDType().getValueOrEmpty());
		}
	}

	@Nullable
	public String getSequenceResultsFlag() {
		return this.sequenceResultsFlag;
	}

	public void setSequenceResultsFlag(@Nullable String sequenceResultsFlag) {
		this.sequenceResultsFlag = sequenceResultsFlag;
	}

	@Nullable
	public String getPlacerOrderNumberEntityIdentifier() {
		return this.placerOrderNumberEntityIdentifier;
	}

	public void setPlacerOrderNumberEntityIdentifier(@Nullable String placerOrderNumberEntityIdentifier) {
		this.placerOrderNumberEntityIdentifier = placerOrderNumberEntityIdentifier;
	}

	@Nullable
	public String getPlacerOrderNumberNamespaceId() {
		return this.placerOrderNumberNamespaceId;
	}

	public void setPlacerOrderNumberNamespaceId(@Nullable String placerOrderNumberNamespaceId) {
		this.placerOrderNumberNamespaceId = placerOrderNumberNamespaceId;
	}

	@Nullable
	public String getFillerOrderNumberEntityIdentifier() {
		return this.fillerOrderNumberEntityIdentifier;
	}

	public void setFillerOrderNumberEntityIdentifier(@Nullable String fillerOrderNumberEntityIdentifier) {
		this.fillerOrderNumberEntityIdentifier = fillerOrderNumberEntityIdentifier;
	}

	@Nullable
	public String getFillerOrderNumberNamespaceId() {
		return this.fillerOrderNumberNamespaceId;
	}

	public void setFillerOrderNumberNamespaceId(@Nullable String fillerOrderNumberNamespaceId) {
		this.fillerOrderNumberNamespaceId = fillerOrderNumberNamespaceId;
	}

	@Nullable
	public String getSequenceConditionValue() {
		return this.sequenceConditionValue;
	}

	public void setSequenceConditionValue(@Nullable String sequenceConditionValue) {
		this.sequenceConditionValue = sequenceConditionValue;
	}

	@Nullable
	public Double getMaximumNumberOfRepeats() {
		return this.maximumNumberOfRepeats;
	}

	public void setMaximumNumberOfRepeats(@Nullable Double maximumNumberOfRepeats) {
		this.maximumNumberOfRepeats = maximumNumberOfRepeats;
	}

	@Nullable
	public String getPlacerOrderNumberUniversalId() {
		return this.placerOrderNumberUniversalId;
	}

	public void setPlacerOrderNumberUniversalId(@Nullable String placerOrderNumberUniversalId) {
		this.placerOrderNumberUniversalId = placerOrderNumberUniversalId;
	}

	@Nullable
	public String getPlacerOrderNumberUniversalIdType() {
		return this.placerOrderNumberUniversalIdType;
	}

	public void setPlacerOrderNumberUniversalIdType(@Nullable String placerOrderNumberUniversalIdType) {
		this.placerOrderNumberUniversalIdType = placerOrderNumberUniversalIdType;
	}

	@Nullable
	public String getFillerOrderNumberUniversalId() {
		return this.fillerOrderNumberUniversalId;
	}

	public void setFillerOrderNumberUniversalId(@Nullable String fillerOrderNumberUniversalId) {
		this.fillerOrderNumberUniversalId = fillerOrderNumberUniversalId;
	}

	@Nullable
	public String getFillerOrderNumberUniversalIdType() {
		return this.fillerOrderNumberUniversalIdType;
	}

	public void setFillerOrderNumberUniversalIdType(@Nullable String fillerOrderNumberUniversalIdType) {
		this.fillerOrderNumberUniversalIdType = fillerOrderNumberUniversalIdType;
	}
}