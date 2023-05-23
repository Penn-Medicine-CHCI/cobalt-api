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

package com.cobaltplatform.api.integration.hl7.model;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class Hl7PatientOrder {
	@Nullable
	private String orderId; // e.g. "881102"
	@Nullable
	private String patientId; // e.g. "555555555"
	@Nullable
	private String patientIdType; // e.g. "UID"

	@Nullable
	public String getOrderId() {
		return this.orderId;
	}

	public void setOrderId(@Nullable String orderId) {
		this.orderId = orderId;
	}

	@Nullable
	public String getPatientId() {
		return this.patientId;
	}

	public void setPatientId(@Nullable String patientId) {
		this.patientId = patientId;
	}

	@Nullable
	public String getPatientIdType() {
		return this.patientIdType;
	}

	public void setPatientIdType(@Nullable String patientIdType) {
		this.patientIdType = patientIdType;
	}
}