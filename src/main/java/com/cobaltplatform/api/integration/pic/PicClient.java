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

package com.cobaltplatform.api.integration.pic;

import com.cobaltplatform.api.integration.pic.model.PicAppointmentCanceledRequest;
import com.cobaltplatform.api.integration.pic.model.PicAppointmentCreatedRequest;
import com.cobaltplatform.api.integration.pic.model.PicEpicPatient;
import com.cobaltplatform.api.integration.pic.model.PicEpicPatientNormalized;

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
public interface PicClient {
	void notifyOfAppointmentCreation(@Nonnull PicAppointmentCreatedRequest request) throws PicException;

	void notifyOfAppointmentCancelation(@Nonnull PicAppointmentCanceledRequest request) throws PicException;

	@Nonnull
	PicEpicPatient parseEpicPatientPayload(@Nonnull String epicPatientPayload) throws PicException;

	@Nonnull
	Boolean verifyPicSigningToken(@Nonnull String picSigningToken);

	@Nonnull
	default String getMockRawEpicPatientPayload() {
		try {
			return Files.readString(Paths.get("src/main/resources/mock-pic-epic-patient-payload.json"));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Nonnull
	default PicEpicPatientNormalized extractPicEpicPatientNormalized(@Nonnull PicEpicPatient picEpicPatient) {
		requireNonNull(picEpicPatient);

		String ssoId;
		String uid = null;
		String hupMrn = null;
		String firstName = null;
		String lastName = null;
		String displayName = null;
		String emailAddress = null;
		String phoneNumber = null;

		// UID
		List<PicEpicPatient.Identifier> identifiers = picEpicPatient.getIdentifier();

		if (identifiers != null && identifiers.size() > 0) {
			for (PicEpicPatient.Identifier identifier : identifiers) {
				if (identifier.getType() != null && Objects.equals(identifier.getType().getText(), "UID"))
					uid = identifier.getValue();
				else if (identifier.getType() != null && Objects.equals(identifier.getType().getText(), "HUP MRN"))
					hupMrn = identifier.getValue();
			}
		}

		// Names
		List<PicEpicPatient.Name> names = picEpicPatient.getName();

		if (names != null && names.size() > 0) {
			PicEpicPatient.Name name = names.get(0);

			firstName = name.getGiven() != null && name.getGiven().size() > 0 ? name.getGiven().get(0) : null;
			lastName = name.getFamily();
			displayName = name.getText();
		}

		// Phone Number + Email Address
		List<PicEpicPatient.Telecom> telecoms = picEpicPatient.getTelecom();

		if (telecoms != null && telecoms.size() > 0) {
			for (PicEpicPatient.Telecom telecom : telecoms) {
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

		PicEpicPatientNormalized normalizedPatient = new PicEpicPatientNormalized();

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
