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

package com.cobaltplatform.api.integration.hl7.model.section;

import ca.uhn.hl7v2.model.v251.segment.OBR;
import com.cobaltplatform.api.integration.hl7.model.Hl7Object;
import com.cobaltplatform.api.integration.hl7.model.segment.Hl7DietTrayInstructionsSegment;
import com.cobaltplatform.api.integration.hl7.model.segment.Hl7ObservationRequestSegment;
import com.cobaltplatform.api.integration.hl7.model.segment.Hl7PharmacyTreatmentOrderSegment;
import com.cobaltplatform.api.integration.hl7.model.segment.Hl7RequisitionDetail1Segment;
import com.cobaltplatform.api.integration.hl7.model.segment.Hl7RequisitionDetailSegment;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * See https://hl7-definition.caristix.com/v2/hl7v2.5.1/TriggerEvents/ORM_O01
 *
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class Hl7OrderDetailSegmentSection extends Hl7Object {
	@Nullable
	private Hl7ObservationRequestSegment observationRequest;
	@Nullable
	private Hl7RequisitionDetailSegment requisitionDetail;
	@Nullable
	private Hl7RequisitionDetail1Segment requisitionDetail1;
	@Nullable
	private Hl7PharmacyTreatmentOrderSegment pharmacyTreatmentOrder;
	@Nullable
	private Hl7DietTrayInstructionsSegment dietTrayInstructions;

	@Nonnull
	public static Boolean isPresent(@Nullable OBR obr) {
		if (obr == null)
			return false;

		// TODO

		return true;
	}

	public Hl7OrderDetailSegmentSection() {
		// Nothing to do
	}

	public Hl7OrderDetailSegmentSection(@Nullable OBR obr) {
		if (obr != null) {
			// TODO
		}
	}

	@Nullable
	public Hl7ObservationRequestSegment getObservationRequest() {
		return this.observationRequest;
	}

	public void setObservationRequest(@Nullable Hl7ObservationRequestSegment observationRequest) {
		this.observationRequest = observationRequest;
	}

	@Nullable
	public Hl7RequisitionDetailSegment getRequisitionDetail() {
		return this.requisitionDetail;
	}

	public void setRequisitionDetail(@Nullable Hl7RequisitionDetailSegment requisitionDetail) {
		this.requisitionDetail = requisitionDetail;
	}

	@Nullable
	public Hl7RequisitionDetail1Segment getRequisitionDetail1() {
		return this.requisitionDetail1;
	}

	public void setRequisitionDetail1(@Nullable Hl7RequisitionDetail1Segment requisitionDetail1) {
		this.requisitionDetail1 = requisitionDetail1;
	}

	@Nullable
	public Hl7PharmacyTreatmentOrderSegment getPharmacyTreatmentOrder() {
		return this.pharmacyTreatmentOrder;
	}

	public void setPharmacyTreatmentOrder(@Nullable Hl7PharmacyTreatmentOrderSegment pharmacyTreatmentOrder) {
		this.pharmacyTreatmentOrder = pharmacyTreatmentOrder;
	}

	@Nullable
	public Hl7DietTrayInstructionsSegment getDietTrayInstructions() {
		return this.dietTrayInstructions;
	}

	public void setDietTrayInstructions(@Nullable Hl7DietTrayInstructionsSegment dietTrayInstructions) {
		this.dietTrayInstructions = dietTrayInstructions;
	}
}