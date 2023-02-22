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

package com.cobaltplatform.api.model.api.request;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class CreatePatientOrderOutreachRequest {
	@Nullable
	private UUID patientOrderId;
	@Nullable
	private UUID accountId;
	@Nullable
	private LocalDate outreachDate;
	@Nullable
	private String outreachTime; // Manually parse string from the UI
	@Nullable
	private String note;

	@Nullable
	public UUID getPatientOrderId() {
		return this.patientOrderId;
	}

	public void setPatientOrderId(@Nullable UUID patientOrderId) {
		this.patientOrderId = patientOrderId;
	}

	@Nullable
	public UUID getAccountId() {
		return this.accountId;
	}

	public void setAccountId(@Nullable UUID accountId) {
		this.accountId = accountId;
	}

	@Nullable
	public LocalDate getOutreachDate() {
		return this.outreachDate;
	}

	public void setOutreachDate(@Nullable LocalDate outreachDate) {
		this.outreachDate = outreachDate;
	}

	@Nullable
	public String getOutreachTime() {
		return this.outreachTime;
	}

	public void setOutreachTime(@Nullable String outreachTime) {
		this.outreachTime = outreachTime;
	}

	@Nullable
	public String getNote() {
		return this.note;
	}

	public void setNote(@Nullable String note) {
		this.note = note;
	}
}
