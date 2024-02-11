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

import ca.uhn.hl7v2.model.v251.segment.PV1;
import com.cobaltplatform.api.integration.hl7.model.Hl7Object;
import com.cobaltplatform.api.integration.hl7.model.type.Hl7ExtendedCompositeIdNumberAndNameForPersons;
import com.cobaltplatform.api.integration.hl7.model.type.Hl7ExtendedCompositeIdWithCheckDigit;
import com.cobaltplatform.api.integration.hl7.model.type.Hl7PersonLocation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * See https://hl7-definition.caristix.com/v2/hl7v2.5.1/Segments/PV1
 *
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class Hl7PatientVisitSegment extends Hl7Object {
	@Nullable
	private Integer setId; //  PV1.1 - Set ID - PV1
	@Nullable
	private String patientClass; // PV1.2 - Patient Class
	@Nullable
	private Hl7PersonLocation assignedPatientLocation; // PV1.3 - Assigned Patient Location
	@Nullable
	private String admissionType; // PV1.4 - Admission Type
	@Nullable
	private Hl7ExtendedCompositeIdWithCheckDigit preadmitNumber; // PV1.5 - Preadmit Number
	@Nullable
	private Hl7PersonLocation priorPatientLocation; // PV1.6 - Prior Patient Location
	@Nullable
	private List<Hl7ExtendedCompositeIdNumberAndNameForPersons> attendingDoctor; // PV1.7 - Attending Doctor
	@Nullable
	private List<Hl7ExtendedCompositeIdNumberAndNameForPersons> referringDoctor; // PV1.8 - Referring Doctor
	@Nullable
	private List<Hl7ExtendedCompositeIdNumberAndNameForPersons> consultingDoctor; // PV1.9 - Consulting Doctor
	@Nullable
	private String hospitalService; // PV1.10 - Hospital Service

	@Nonnull
	public static Boolean isPresent(@Nullable PV1 pv1) {
		if (pv1 == null)
			return false;

		return trimToNull(pv1.getSetIDPV1().getValue()) != null
				|| trimToNull(pv1.getPatientClass().getValueOrEmpty()) != null
				|| Hl7PersonLocation.isPresent(pv1.getAssignedPatientLocation())
				|| trimToNull(pv1.getAdmissionType().getValueOrEmpty()) != null
				|| Hl7ExtendedCompositeIdWithCheckDigit.isPresent(pv1.getPreadmitNumber())
				|| Hl7PersonLocation.isPresent(pv1.getPriorPatientLocation())
				|| (pv1.getAttendingDoctor() != null && pv1.getAttendingDoctor().length > 0)
				|| (pv1.getReferringDoctor() != null && pv1.getReferringDoctor().length > 0)
				|| (pv1.getConsultingDoctor() != null && pv1.getConsultingDoctor().length > 0)
				|| trimToNull(pv1.getHospitalService().getValueOrEmpty()) != null
				;
	}

	public Hl7PatientVisitSegment() {
		// Nothing to do
	}

	public Hl7PatientVisitSegment(@Nullable PV1 pv1) {
		if (pv1 != null) {
			String setIdAsString = trimToNull(pv1.getSetIDPV1().getValue());
			if (setIdAsString != null)
				this.setId = Integer.parseInt(setIdAsString, 10);

			this.patientClass = trimToNull(pv1.getPatientClass().getValueOrEmpty());

			if (Hl7PersonLocation.isPresent(pv1.getAssignedPatientLocation()))
				this.assignedPatientLocation = new Hl7PersonLocation(pv1.getAssignedPatientLocation());

			this.admissionType = trimToNull(pv1.getAdmissionType().getValueOrEmpty());

			if (Hl7ExtendedCompositeIdWithCheckDigit.isPresent(pv1.getPreadmitNumber()))
				this.preadmitNumber = new Hl7ExtendedCompositeIdWithCheckDigit(pv1.getPreadmitNumber());

			if (Hl7PersonLocation.isPresent(pv1.getPriorPatientLocation()))
				this.priorPatientLocation = new Hl7PersonLocation(pv1.getPriorPatientLocation());

			if (pv1.getAttendingDoctor() != null && pv1.getAttendingDoctor().length > 0)
				this.attendingDoctor = Arrays.stream(pv1.getAttendingDoctor())
						.map(xcn -> Hl7ExtendedCompositeIdNumberAndNameForPersons.isPresent(xcn) ? new Hl7ExtendedCompositeIdNumberAndNameForPersons(xcn) : null)
						.filter(attendingDoctor -> attendingDoctor != null)
						.collect(Collectors.toList());

			if (pv1.getReferringDoctor() != null && pv1.getReferringDoctor().length > 0)
				this.referringDoctor = Arrays.stream(pv1.getReferringDoctor())
						.map(xcn -> Hl7ExtendedCompositeIdNumberAndNameForPersons.isPresent(xcn) ? new Hl7ExtendedCompositeIdNumberAndNameForPersons(xcn) : null)
						.filter(referringDoctor -> referringDoctor != null)
						.collect(Collectors.toList());

			if (pv1.getConsultingDoctor() != null && pv1.getConsultingDoctor().length > 0)
				this.consultingDoctor = Arrays.stream(pv1.getConsultingDoctor())
						.map(xcn -> Hl7ExtendedCompositeIdNumberAndNameForPersons.isPresent(xcn) ? new Hl7ExtendedCompositeIdNumberAndNameForPersons(xcn) : null)
						.filter(consultingDoctor -> consultingDoctor != null)
						.collect(Collectors.toList());

			this.hospitalService = trimToNull(pv1.getHospitalService().getValueOrEmpty());

			
		}
	}
}