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
import com.cobaltplatform.api.integration.hl7.model.segment.Hl7GuarantorSegment;
import com.cobaltplatform.api.integration.hl7.model.segment.Hl7NotesAndCommentsSegment;
import com.cobaltplatform.api.integration.hl7.model.segment.Hl7PatientAdditionalDemographicSegment;
import com.cobaltplatform.api.integration.hl7.model.segment.Hl7PatientAllergyInformationSegment;
import com.cobaltplatform.api.integration.hl7.model.segment.Hl7PatientIdentificationSegment;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.List;

/**
 * See https://hl7-definition.caristix.com/v2/hl7v2.5.1/TriggerEvents/ORM_O01
 *
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class Hl7PatientSection extends Hl7Object {
	@Nullable
	private Hl7PatientIdentificationSegment patientIdentification;
	@Nullable
	private Hl7PatientAdditionalDemographicSegment patientAdditionalDemographic;
	@Nullable
	private List<Hl7NotesAndCommentsSegment> notesAndComments;
	@Nullable
	private Hl7GuarantorSegment guarantor;
	@Nullable
	private List<Hl7PatientAllergyInformationSegment> patientAllergyInformation;
	@Nullable
	private Hl7PatientVisitSection patientVisit;
	@Nullable
	private List<Hl7InsuranceSection> insurance;
}