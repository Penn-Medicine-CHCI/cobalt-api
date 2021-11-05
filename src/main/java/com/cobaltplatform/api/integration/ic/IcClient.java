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

package com.cobaltplatform.api.integration.ic;

import com.cobaltplatform.api.integration.ic.model.IcAppointmentCanceledRequest;
import com.cobaltplatform.api.integration.ic.model.IcAppointmentCreatedRequest;
import com.cobaltplatform.api.integration.ic.model.IcEpicPatient;
import com.cobaltplatform.api.integration.ic.model.IcEpicPatientNormalized;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public interface IcClient {
	void notifyOfAppointmentCreation(@Nonnull IcAppointmentCreatedRequest request) throws IcException;

	void notifyOfAppointmentCancelation(@Nonnull IcAppointmentCanceledRequest request) throws IcException;

	@Nonnull
	IcEpicPatient parseEpicPatientPayload(@Nonnull String epicPatientPayload) throws IcException;

	@Nonnull
	Boolean verifyIcSigningToken(@Nonnull String icSigningToken);

	@Nonnull
	default String getMockRawEpicPatientPayload() {
		try {
			return Files.readString(Paths.get("src/main/resources/mock-ic-epic-patient-payload.json"));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Nonnull
	default IcEpicPatientNormalized extractIcEpicPatientNormalized(@Nonnull IcEpicPatient icEpicPatient) {
		requireNonNull(icEpicPatient);

		String ssoId;
		String uid = null;
		String hupMrn = null;
		String firstName = null;
		String lastName = null;
		String displayName = null;
		String emailAddress = null;
		String phoneNumber = null;

		// UID
		List<IcEpicPatient.Identifier> identifiers = icEpicPatient.getIdentifier();

		if (identifiers != null && identifiers.size() > 0) {
			for (IcEpicPatient.Identifier identifier : identifiers) {
				if (identifier.getType() != null && Objects.equals(identifier.getType().getText(), "UID"))
					uid = identifier.getValue();
				else if (identifier.getType() != null && Objects.equals(identifier.getType().getText(), "HUP MRN"))
					hupMrn = identifier.getValue();
			}
		}

		// Names
		List<IcEpicPatient.Name> names = icEpicPatient.getName();

		if (names != null && names.size() > 0) {
			IcEpicPatient.Name name = names.get(0);

			firstName = name.getGiven() != null && name.getGiven().size() > 0 ? name.getGiven().get(0) : null;
			lastName = name.getFamily();
			displayName = name.getText();
		}

		// Phone Number + Email Address
		List<IcEpicPatient.Telecom> telecoms = icEpicPatient.getTelecom();

		if (telecoms != null && telecoms.size() > 0) {
			for (IcEpicPatient.Telecom telecom : telecoms) {
				if (phoneNumber == null && Objects.equals("phone", telecom.getSystem())) {
					if (telecom.getValue() != null)
						phoneNumber = telecom.getValue();
				}

				if (emailAddress == null && Objects.equals("email", telecom.getSystem())) {
					if (telecom.getValue() != null)
						emailAddress = telecom.getValue();
				}
			}
		}

		IcEpicPatientNormalized normalizedPatient = new IcEpicPatientNormalized();

		if (uid != null) {
			normalizedPatient.setEpicPatientId(uid);
			normalizedPatient.setEpicPatientIdType("EXTERNAL");
		} else if (hupMrn != null) {
			normalizedPatient.setEpicPatientId(hupMrn);
			normalizedPatient.setEpicPatientIdType("EXTERNAL");
		}

		ssoId = uid;

		normalizedPatient.setSsoId(ssoId);
		normalizedPatient.setFirstName(firstName);
		normalizedPatient.setLastName(lastName);
		normalizedPatient.setDisplayName(displayName);
		normalizedPatient.setEmailAddress(emailAddress);
		normalizedPatient.setPhoneNumber(phoneNumber);

		return normalizedPatient;
	}
}
