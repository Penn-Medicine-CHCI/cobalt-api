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
import com.cobaltplatform.api.model.db.PatientOrderDiagnosis;
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
public class PatientOrderDiagnosisApiResponse {
	@Nonnull
	private final UUID patientOrderDiagnosisId;
	@Nonnull
	private final UUID patientOrderId;
	@Nullable
	private final String diagnosisId;
	@Nullable
	private final String diagnosisIdType;
	@Nullable
	private final String diagnosisName;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface PatientOrderDiagnosisApiResponseFactory {
		@Nonnull
		PatientOrderDiagnosisApiResponse create(@Nonnull PatientOrderDiagnosis patientOrderDiagnosis);
	}

	@AssistedInject
	public PatientOrderDiagnosisApiResponse(@Nonnull Provider<CurrentContext> currentContextProvider,
																					@Assisted @Nonnull PatientOrderDiagnosis patientOrderDiagnosis) {
		requireNonNull(currentContextProvider);
		requireNonNull(patientOrderDiagnosis);

		this.patientOrderDiagnosisId = patientOrderDiagnosis.getPatientOrderDiagnosisId();
		this.patientOrderId = patientOrderDiagnosis.getPatientOrderId();
		this.diagnosisId = patientOrderDiagnosis.getDiagnosisId();
		this.diagnosisIdType = patientOrderDiagnosis.getDiagnosisIdType();
		this.diagnosisName = patientOrderDiagnosis.getDiagnosisName();
	}

	@Nonnull
	public UUID getPatientOrderDiagnosisId() {
		return this.patientOrderDiagnosisId;
	}

	@Nonnull
	public UUID getPatientOrderId() {
		return this.patientOrderId;
	}

	@Nullable
	public String getDiagnosisId() {
		return this.diagnosisId;
	}

	@Nullable
	public String getDiagnosisIdType() {
		return this.diagnosisIdType;
	}

	@Nullable
	public String getDiagnosisName() {
		return this.diagnosisName;
	}
}