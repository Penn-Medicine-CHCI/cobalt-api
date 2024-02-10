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

import com.cobaltplatform.api.integration.hl7.model.Hl7Object;
import com.cobaltplatform.api.integration.hl7.model.segment.Hl7PatientVisitSegment;
import com.cobaltplatform.api.integration.hl7.model.segment.Hl7PatientVisitAdditionalInformationSegment;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * See https://hl7-definition.caristix.com/v2/hl7v2.5.1/TriggerEvents/ORM_O01
 *
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class Hl7PatientVisitSection extends Hl7Object {
	@Nullable
	private Hl7PatientVisitSegment patientVisit;
	@Nullable
	private Hl7PatientVisitAdditionalInformationSegment patientVisitAdditionalInformation;

	@Nullable
	public Hl7PatientVisitSegment getPatientVisit() {
		return this.patientVisit;
	}

	public void setPatientVisit(@Nullable Hl7PatientVisitSegment patientVisit) {
		this.patientVisit = patientVisit;
	}

	@Nullable
	public Hl7PatientVisitAdditionalInformationSegment getPatientVisitAdditionalInformation() {
		return this.patientVisitAdditionalInformation;
	}

	public void setPatientVisitAdditionalInformation(@Nullable Hl7PatientVisitAdditionalInformationSegment patientVisitAdditionalInformation) {
		this.patientVisitAdditionalInformation = patientVisitAdditionalInformation;
	}
}