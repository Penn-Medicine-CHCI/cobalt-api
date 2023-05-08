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
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.PatientOrderInsurancePayor;
import com.cobaltplatform.api.model.db.PatientOrderInsurancePayorType.PatientOrderInsurancePayorTypeId;
import com.cobaltplatform.api.util.Formatter;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Immutable
public class PatientOrderInsurancePayorApiResponse {
	@Nonnull
	private final UUID patientOrderInsurancePayorId;
	@Nonnull
	private final PatientOrderInsurancePayorTypeId patientOrderInsurancePayorTypeId;
	@Nonnull
	private final InstitutionId institutionId;
	@Nonnull
	private final String name;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface PatientOrderInsurancePayorApiResponseFactory {
		@Nonnull
		PatientOrderInsurancePayorApiResponse create(@Nonnull PatientOrderInsurancePayor patientOrderInsurancePayor);
	}

	@AssistedInject
	public PatientOrderInsurancePayorApiResponse(@Nonnull Formatter formatter,
																							 @Nonnull Provider<CurrentContext> currentContextProvider,
																							 @Assisted @Nonnull PatientOrderInsurancePayor patientOrderInsurancePayor) {
		requireNonNull(formatter);
		requireNonNull(currentContextProvider);
		requireNonNull(patientOrderInsurancePayor);

		this.patientOrderInsurancePayorId = patientOrderInsurancePayor.getPatientOrderInsurancePayorId();
		this.patientOrderInsurancePayorTypeId = patientOrderInsurancePayor.getPatientOrderInsurancePayorTypeId();
		this.institutionId = patientOrderInsurancePayor.getInstitutionId();
		this.name = patientOrderInsurancePayor.getName();
	}

	@Nonnull
	public UUID getPatientOrderInsurancePayorId() {
		return this.patientOrderInsurancePayorId;
	}

	@Nonnull
	public PatientOrderInsurancePayorTypeId getPatientOrderInsurancePayorTypeId() {
		return this.patientOrderInsurancePayorTypeId;
	}

	@Nonnull
	public InstitutionId getInstitutionId() {
		return this.institutionId;
	}

	@Nonnull
	public String getName() {
		return this.name;
	}
}