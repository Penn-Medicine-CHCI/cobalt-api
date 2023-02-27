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

package com.cobaltplatform.api.model.api.response;

import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.model.db.PatientOrderMedication;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Immutable
public class PatientOrderMedicationApiResponse {
	@Nonnull
	private final UUID patientOrderMedicationId;
	@Nonnull
	private final UUID patientOrderId;
	@Nullable
	private final String medicationId;
	@Nullable
	private final String medicationIdType;
	@Nullable
	private final String medicationName;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface PatientOrderMedicationApiResponseFactory {
		@Nonnull
		PatientOrderMedicationApiResponse create(@Nonnull PatientOrderMedication patientOrderMedication);
	}

	@AssistedInject
	public PatientOrderMedicationApiResponse(@Nonnull Provider<CurrentContext> currentContextProvider,
																					 @Assisted @Nonnull PatientOrderMedication patientOrderMedication) {
		requireNonNull(currentContextProvider);
		requireNonNull(patientOrderMedication);

		this.patientOrderMedicationId = patientOrderMedication.getPatientOrderMedicationId();
		this.patientOrderId = patientOrderMedication.getPatientOrderId();
		this.medicationId = patientOrderMedication.getMedicationId();
		this.medicationIdType = patientOrderMedication.getMedicationIdType();
		this.medicationName = patientOrderMedication.getMedicationName();
	}

	@Nonnull
	public UUID getPatientOrderMedicationId() {
		return this.patientOrderMedicationId;
	}

	@Nonnull
	public UUID getPatientOrderId() {
		return this.patientOrderId;
	}

	@Nullable
	public String getMedicationId() {
		return this.medicationId;
	}

	@Nullable
	public String getMedicationIdType() {
		return this.medicationIdType;
	}

	@Nullable
	public String getMedicationName() {
		return this.medicationName;
	}
}