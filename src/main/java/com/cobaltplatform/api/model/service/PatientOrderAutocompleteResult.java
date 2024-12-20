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

package com.cobaltplatform.api.model.service;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class PatientOrderAutocompleteResult {
	@Nullable
	private UUID patientOrderId;
	@Nullable
	private Integer referenceNumber;
	@Nullable
	private String patientMrn;
	@Nullable
	private String patientUniqueId;
	@Nullable
	private String patientUniqueIdType;
	@Nullable
	private UUID patientAccountId;
	@Nullable
	private String patientFirstName;
	@Nullable
	private String patientLastName;
	@Nullable
	private String patientPhoneNumber;
	@Nullable
	private String patientEmailAddress;

	@Nullable
	public UUID getPatientOrderId() {
		return this.patientOrderId;
	}

	public void setPatientOrderId(@Nullable UUID patientOrderId) {
		this.patientOrderId = patientOrderId;
	}

	@Nullable
	public Integer getReferenceNumber() {
		return this.referenceNumber;
	}

	public void setReferenceNumber(@Nullable Integer referenceNumber) {
		this.referenceNumber = referenceNumber;
	}

	@Nullable
	public String getPatientMrn() {
		return this.patientMrn;
	}

	public void setPatientMrn(@Nullable String patientMrn) {
		this.patientMrn = patientMrn;
	}

	@Nullable
	public String getPatientUniqueId() {
		return this.patientUniqueId;
	}

	public void setPatientUniqueId(@Nullable String patientUniqueId) {
		this.patientUniqueId = patientUniqueId;
	}

	@Nullable
	public String getPatientUniqueIdType() {
		return this.patientUniqueIdType;
	}

	public void setPatientUniqueIdType(@Nullable String patientUniqueIdType) {
		this.patientUniqueIdType = patientUniqueIdType;
	}

	@Nullable
	public UUID getPatientAccountId() {
		return this.patientAccountId;
	}

	public void setPatientAccountId(@Nullable UUID patientAccountId) {
		this.patientAccountId = patientAccountId;
	}

	@Nullable
	public String getPatientFirstName() {
		return this.patientFirstName;
	}

	public void setPatientFirstName(@Nullable String patientFirstName) {
		this.patientFirstName = patientFirstName;
	}

	@Nullable
	public String getPatientLastName() {
		return this.patientLastName;
	}

	public void setPatientLastName(@Nullable String patientLastName) {
		this.patientLastName = patientLastName;
	}

	@Nullable
	public String getPatientPhoneNumber() {
		return this.patientPhoneNumber;
	}

	public void setPatientPhoneNumber(@Nullable String patientPhoneNumber) {
		this.patientPhoneNumber = patientPhoneNumber;
	}

	@Nullable
	public String getPatientEmailAddress() {
		return this.patientEmailAddress;
	}

	public void setPatientEmailAddress(@Nullable String patientEmailAddress) {
		this.patientEmailAddress = patientEmailAddress;
	}
}
