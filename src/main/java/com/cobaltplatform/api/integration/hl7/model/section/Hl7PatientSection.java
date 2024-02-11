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

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.v251.group.ORM_O01_PATIENT;
import com.cobaltplatform.api.integration.hl7.UncheckedHl7ParsingException;
import com.cobaltplatform.api.integration.hl7.model.Hl7Object;
import com.cobaltplatform.api.integration.hl7.model.segment.Hl7GuarantorSegment;
import com.cobaltplatform.api.integration.hl7.model.segment.Hl7NotesAndCommentsSegment;
import com.cobaltplatform.api.integration.hl7.model.segment.Hl7PatientAdditionalDemographicSegment;
import com.cobaltplatform.api.integration.hl7.model.segment.Hl7PatientAllergyInformationSegment;
import com.cobaltplatform.api.integration.hl7.model.segment.Hl7PatientIdentificationSegment;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.List;
import java.util.stream.Collectors;

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
	private Hl7PatientVisitSection patientVisit;
	@Nullable
	private List<Hl7InsuranceSection> insurance;
	@Nullable
	private Hl7GuarantorSegment guarantor;
	@Nullable
	private List<Hl7PatientAllergyInformationSegment> patientAllergyInformation;

	@Nonnull
	public static Boolean isPresent(@Nullable ORM_O01_PATIENT patient) {
		if (patient == null)
			return false;

		try {
			return patient.getPID() != null
					|| patient.getPD1() != null
					|| patient.getNTEAll() != null
					|| patient.getPATIENT_VISIT() != null
					|| patient.getINSURANCEAll() != null
					|| patient.getGT1() != null
					|| patient.getAL1All() != null;
		} catch (HL7Exception e) {
			throw new UncheckedHl7ParsingException(e);
		}
	}

	public Hl7PatientSection() {
		// Nothing to do
	}

	public Hl7PatientSection(@Nullable ORM_O01_PATIENT patient) {
		if (patient != null) {
			this.patientIdentification = Hl7PatientIdentificationSegment.isPresent(patient.getPID()) ? new Hl7PatientIdentificationSegment(patient.getPID()) : null;

			// TODO: Hl7PatientAdditionalDemographicSegment patientAdditionalDemographic

			try {
				if (patient.getNTEAll() != null && patient.getNTEAll().size() > 0)
					this.notesAndComments = patient.getNTEAll().stream()
							.map(nte -> Hl7NotesAndCommentsSegment.isPresent(nte) ? new Hl7NotesAndCommentsSegment(nte) : null)
							.filter(notesAndComments -> notesAndComments != null)
							.collect(Collectors.toList());
			} catch (HL7Exception e) {
				throw new UncheckedHl7ParsingException(e);
			}

			if (Hl7PatientVisitSection.isPresent(patient.getPATIENT_VISIT()))
				this.patientVisit = new Hl7PatientVisitSection(patient.getPATIENT_VISIT());

			//	@Nullable
			//	private List<Hl7InsuranceSection> insurance;
			//	@Nullable
			//	private Hl7GuarantorSegment guarantor;
			//	@Nullable
			//	private List<Hl7PatientAllergyInformationSegment> patientAllergyInformation;
		}
	}

	@Nullable
	public Hl7PatientIdentificationSegment getPatientIdentification() {
		return this.patientIdentification;
	}

	public void setPatientIdentification(@Nullable Hl7PatientIdentificationSegment patientIdentification) {
		this.patientIdentification = patientIdentification;
	}

	@Nullable
	public Hl7PatientAdditionalDemographicSegment getPatientAdditionalDemographic() {
		return this.patientAdditionalDemographic;
	}

	public void setPatientAdditionalDemographic(@Nullable Hl7PatientAdditionalDemographicSegment patientAdditionalDemographic) {
		this.patientAdditionalDemographic = patientAdditionalDemographic;
	}

	@Nullable
	public List<Hl7NotesAndCommentsSegment> getNotesAndComments() {
		return this.notesAndComments;
	}

	public void setNotesAndComments(@Nullable List<Hl7NotesAndCommentsSegment> notesAndComments) {
		this.notesAndComments = notesAndComments;
	}

	@Nullable
	public Hl7PatientVisitSection getPatientVisit() {
		return this.patientVisit;
	}

	public void setPatientVisit(@Nullable Hl7PatientVisitSection patientVisit) {
		this.patientVisit = patientVisit;
	}

	@Nullable
	public List<Hl7InsuranceSection> getInsurance() {
		return this.insurance;
	}

	public void setInsurance(@Nullable List<Hl7InsuranceSection> insurance) {
		this.insurance = insurance;
	}

	@Nullable
	public Hl7GuarantorSegment getGuarantor() {
		return this.guarantor;
	}

	public void setGuarantor(@Nullable Hl7GuarantorSegment guarantor) {
		this.guarantor = guarantor;
	}

	@Nullable
	public List<Hl7PatientAllergyInformationSegment> getPatientAllergyInformation() {
		return this.patientAllergyInformation;
	}

	public void setPatientAllergyInformation(@Nullable List<Hl7PatientAllergyInformationSegment> patientAllergyInformation) {
		this.patientAllergyInformation = patientAllergyInformation;
	}
}