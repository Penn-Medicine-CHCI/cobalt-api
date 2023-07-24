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

import com.cobaltplatform.api.model.service.PatientOrderAutocompleteResult;
import com.cobaltplatform.api.util.Formatter;
import com.cobaltplatform.api.util.Normalizer;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lokalized.Strings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class PatientOrderAutocompleteResultApiResponse {
	@Nonnull
	private final String patientMrn;
	@Nonnull
	private final String patientUniqueId;
	@Nonnull
	private final String patientUniqueIdType;
	@Nullable
	private final UUID patientAccountId;
	@Nonnull
	private final String patientFirstName;
	@Nonnull
	private final String patientLastName;
	@Nonnull
	private final String patientDisplayName;
	@Nonnull
	private final String patientDisplayNameWithLastFirst;
	@Nonnull
	private final String patientPhoneNumber;
	@Nonnull
	private final String patientPhoneNumberDescription;
	@Nullable
	private final String patientEmailAddress;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface PatientOrderAutocompleteResultApiResponseFactory {
		@Nonnull
		PatientOrderAutocompleteResultApiResponse create(@Nonnull PatientOrderAutocompleteResult result);
	}

	@AssistedInject
	public PatientOrderAutocompleteResultApiResponse(@Nonnull Formatter formatter,
																									 @Nonnull Strings strings,
																									 @Assisted @Nonnull PatientOrderAutocompleteResult result) {
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(result);

		this.patientMrn = result.getPatientMrn();
		this.patientUniqueId = result.getPatientUniqueId();
		this.patientUniqueIdType = result.getPatientUniqueIdType();
		this.patientAccountId = result.getPatientAccountId();
		this.patientFirstName = result.getPatientFirstName();
		this.patientLastName = result.getPatientLastName();
		this.patientDisplayName = Normalizer.normalizeName(result.getPatientFirstName(), result.getPatientLastName()).get();
		this.patientDisplayNameWithLastFirst = Normalizer.normalizeNameWithLastFirst(result.getPatientFirstName(), result.getPatientLastName()).get();
		this.patientPhoneNumber = result.getPatientPhoneNumber();
		this.patientPhoneNumberDescription = formatter.formatPhoneNumber(result.getPatientPhoneNumber());
		this.patientEmailAddress = result.getPatientEmailAddress();
	}

	@Nonnull
	public String getPatientMrn() {
		return this.patientMrn;
	}

	@Nonnull
	public String getPatientUniqueId() {
		return this.patientUniqueId;
	}

	@Nonnull
	public String getPatientUniqueIdType() {
		return this.patientUniqueIdType;
	}

	@Nullable
	public UUID getPatientAccountId() {
		return this.patientAccountId;
	}

	@Nonnull
	public String getPatientFirstName() {
		return this.patientFirstName;
	}

	@Nonnull
	public String getPatientLastName() {
		return this.patientLastName;
	}

	@Nonnull
	public String getPatientDisplayName() {
		return this.patientDisplayName;
	}

	@Nonnull
	public String getPatientDisplayNameWithLastFirst() {
		return this.patientDisplayNameWithLastFirst;
	}

	@Nonnull
	public String getPatientPhoneNumber() {
		return this.patientPhoneNumber;
	}

	@Nonnull
	public String getPatientPhoneNumberDescription() {
		return this.patientPhoneNumberDescription;
	}

	@Nullable
	public String getPatientEmailAddress() {
		return this.patientEmailAddress;
	}
}