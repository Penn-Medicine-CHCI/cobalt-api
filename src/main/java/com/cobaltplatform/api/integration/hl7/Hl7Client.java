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

package com.cobaltplatform.api.integration.hl7;

import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v251.group.ORM_O01_ORDER;
import ca.uhn.hl7v2.model.v251.group.ORM_O01_PATIENT;
import ca.uhn.hl7v2.model.v251.message.ORM_O01;
import ca.uhn.hl7v2.model.v251.segment.ORC;
import ca.uhn.hl7v2.parser.Parser;
import com.cobaltplatform.api.integration.hl7.model.Hl7PatientOrder;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Singleton;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * See https://github.com/hapifhir/hapi-hl7v2 for details.
 *
 * @author Transmogrify, LLC.
 */
@Singleton
@ThreadSafe
public class Hl7Client {
	@Nonnull
	public Hl7PatientOrder parsePatientOrderMessage(@Nonnull String patientOrderMessage) throws Hl7ParsingException {
		requireNonNull(patientOrderMessage);

		// TODO: determine if these are threadsafe, would be nice to share them across threads
		HapiContext hapiContext = new DefaultHapiContext();
		Parser parser = hapiContext.getGenericParser();

		Message hapiMessage;

		try {
			hapiMessage = parser.parse(patientOrderMessage);
		} catch (Exception e) {
			throw new Hl7ParsingException(format("Unable to parse HL7 message:\n%s", patientOrderMessage), e);
		}

		try {
			ORM_O01 ormMessage = (ORM_O01) hapiMessage;

			ORM_O01_ORDER order = ormMessage.getORDER();
			ORC orc = order.getORC();

			String orderId = orc.getOrc2_PlacerOrderNumber().getEi1_EntityIdentifier().getValue();

			if (orderId == null)
				throw new Hl7ParsingException(format("HL7 message did not contain an order ID:\n%s", patientOrderMessage));

			ORM_O01_PATIENT patient = ormMessage.getPATIENT();

			String patientId = patient.getPID().getPatientID().getIDNumber().getValue();

			if (patientId == null)
				throw new Hl7ParsingException(format("HL7 message did not contain a patient ID:\n%s", patientOrderMessage));

			String patientIdType = patient.getPID().getPatientID().getIdentifierTypeCode().getValue();

			if (patientIdType == null)
				throw new Hl7ParsingException(format("HL7 message did not contain a patient ID type:\n%s", patientOrderMessage));

			Hl7PatientOrder hl7PatientOrder = new Hl7PatientOrder();
			hl7PatientOrder.setOrderId(orderId);
			hl7PatientOrder.setPatientId(patientId);
			hl7PatientOrder.setPatientIdType(patientIdType);

			return hl7PatientOrder;
		} catch (Hl7ParsingException e) {
			throw e;
		} catch (Exception e) {
			throw new Hl7ParsingException(format("Encountered an unexpected issue while processing HL7 message:\n%s", patientOrderMessage), e);
		}
	}
}
