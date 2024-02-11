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

import ca.uhn.hl7v2.model.v251.segment.PID;
import com.cobaltplatform.api.integration.hl7.model.Hl7Object;
import com.cobaltplatform.api.integration.hl7.model.type.Hl7ExtendedCompositeIdWithCheckDigit;
import com.cobaltplatform.api.integration.hl7.model.type.Hl7ExtendedPersonName;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * See https://hl7-definition.caristix.com/v2/hl7v2.5.1/Segments/PID
 *
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class Hl7PatientIdentificationSegment extends Hl7Object {
	@Nullable
	private Integer setId; // PID.1 - Set ID - PID
	@Nullable
	private Hl7ExtendedCompositeIdWithCheckDigit patientId; // PID.2 - Patient ID
	@Nullable
	private List<Hl7ExtendedCompositeIdWithCheckDigit> patientIdentifierList; // PID.3 - Patient Identifier List
	@Nullable
	private List<Hl7ExtendedCompositeIdWithCheckDigit> alternatePatientId; // PID.4 - Alternate Patient ID - PID
	@Nullable
	private List<Hl7ExtendedPersonName> patientName; // PID.5 - Patient Name
	@Nullable
	private List<Hl7ExtendedPersonName> mothersMaidenName; // PID.6 - Mother's Maiden Name

	@Nonnull
	public static Boolean isPresent(@Nullable PID pid) {
		if (pid == null)
			return false;

		return trimToNull(pid.getSetIDPID().getValue()) != null
				|| Hl7ExtendedCompositeIdWithCheckDigit.isPresent(pid.getPatientID())
				|| pid.getPatientIdentifierList() != null && pid.getPatientIdentifierList().length > 0
				|| pid.getAlternatePatientIDPID() != null && pid.getAlternatePatientIDPID().length > 0
				|| pid.getPatientName() != null && pid.getPatientName().length > 0
				|| pid.getMotherSMaidenName() != null && pid.getMotherSMaidenName().length > 0
				;
	}

	public Hl7PatientIdentificationSegment() {
		// Nothing to do
	}

	public Hl7PatientIdentificationSegment(@Nullable PID pid) {
		if (pid != null) {
			String setIdAsString = trimToNull(pid.getSetIDPID().getValue());
			if (setIdAsString != null)
				this.setId = Integer.parseInt(setIdAsString, 10);

			this.patientId = Hl7ExtendedCompositeIdWithCheckDigit.isPresent(pid.getPatientID()) ? new Hl7ExtendedCompositeIdWithCheckDigit(pid.getPatientID()) : null;

			if (pid.getPatientIdentifierList() != null && pid.getPatientIdentifierList().length > 0)
				this.patientIdentifierList = Arrays.stream(pid.getPatientIdentifierList())
						.map(cx -> Hl7ExtendedCompositeIdWithCheckDigit.isPresent(cx) ? new Hl7ExtendedCompositeIdWithCheckDigit(cx) : null)
						.filter(patientIdentifier -> patientIdentifier != null)
						.collect(Collectors.toList());

			if (pid.getAlternatePatientIDPID() != null && pid.getAlternatePatientIDPID().length > 0)
				this.alternatePatientId = Arrays.stream(pid.getAlternatePatientIDPID())
						.map(cx -> Hl7ExtendedCompositeIdWithCheckDigit.isPresent(cx) ? new Hl7ExtendedCompositeIdWithCheckDigit(cx) : null)
						.filter(patientIdentifier -> patientIdentifier != null)
						.collect(Collectors.toList());

			if (pid.getPatientName() != null && pid.getPatientName().length > 0)
				this.patientName = Arrays.stream(pid.getPatientName())
						.map(xpn -> Hl7ExtendedPersonName.isPresent(xpn) ? new Hl7ExtendedPersonName(xpn) : null)
						.filter(patientName -> patientName != null)
						.collect(Collectors.toList());

			if (pid.getMotherSMaidenName() != null && pid.getMotherSMaidenName().length > 0)
				this.mothersMaidenName = Arrays.stream(pid.getMotherSMaidenName())
						.map(xpn -> Hl7ExtendedPersonName.isPresent(xpn) ? new Hl7ExtendedPersonName(xpn) : null)
						.filter(mothersMaidenName -> mothersMaidenName != null)
						.collect(Collectors.toList());
		}
	}
}