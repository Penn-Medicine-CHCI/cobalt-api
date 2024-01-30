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

package com.cobaltplatform.api.model.db;

import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.PatientOrderImportType.PatientOrderImportTypeId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class PatientOrderImport {
	@Nullable
	private UUID patientOrderImportId;
	@Nullable
	private PatientOrderImportTypeId patientOrderImportTypeId;
	@Nullable
	private InstitutionId institutionId;
	@Nullable
	private UUID accountId;
	@Nullable
	private String rawOrder;
	@Nullable
	private String rawOrderChecksum;
	@Nullable
	private String rawOrderFilename;
	@Nullable
	private Instant created;
	@Nullable
	private Instant lastUpdated;

	@Nullable
	public UUID getPatientOrderImportId() {
		return this.patientOrderImportId;
	}

	public void setPatientOrderImportId(@Nullable UUID patientOrderImportId) {
		this.patientOrderImportId = patientOrderImportId;
	}

	@Nullable
	public PatientOrderImportTypeId getPatientOrderImportTypeId() {
		return this.patientOrderImportTypeId;
	}

	public void setPatientOrderImportTypeId(@Nullable PatientOrderImportTypeId patientOrderImportTypeId) {
		this.patientOrderImportTypeId = patientOrderImportTypeId;
	}

	@Nullable
	public InstitutionId getInstitutionId() {
		return this.institutionId;
	}

	public void setInstitutionId(@Nullable InstitutionId institutionId) {
		this.institutionId = institutionId;
	}

	@Nullable
	public UUID getAccountId() {
		return this.accountId;
	}

	public void setAccountId(@Nullable UUID accountId) {
		this.accountId = accountId;
	}

	@Nullable
	public String getRawOrder() {
		return this.rawOrder;
	}

	public void setRawOrder(@Nullable String rawOrder) {
		this.rawOrder = rawOrder;
	}

	@Nullable
	public String getRawOrderChecksum() {
		return this.rawOrderChecksum;
	}

	public void setRawOrderChecksum(@Nullable String rawOrderChecksum) {
		this.rawOrderChecksum = rawOrderChecksum;
	}

	@Nullable
	public String getRawOrderFilename() {
		return this.rawOrderFilename;
	}

	public void setRawOrderFilename(@Nullable String rawOrderFilename) {
		this.rawOrderFilename = rawOrderFilename;
	}

	@Nullable
	public Instant getCreated() {
		return this.created;
	}

	public void setCreated(@Nullable Instant created) {
		this.created = created;
	}

	@Nullable
	public Instant getLastUpdated() {
		return this.lastUpdated;
	}

	public void setLastUpdated(@Nullable Instant lastUpdated) {
		this.lastUpdated = lastUpdated;
	}
}
