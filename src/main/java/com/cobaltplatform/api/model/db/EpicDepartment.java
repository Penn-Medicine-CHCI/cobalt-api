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

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class EpicDepartment {
	@Nullable
	private UUID epicDepartmentId;
	@Nullable
	private InstitutionId institutionId;
	@Nullable
	private String departmentId; // e.g. "603"
	@Nullable
	private String departmentIdType; // e.g. "EXTERNAL"
	@Nullable
	private String name; // e.g. "PBH OPC 3535 MARKET ST 2ND FLOOR" (user-facing, can be whatever we like)
	@Nullable
	private Instant created;
	@Nullable
	private Instant lastUpdated;

	@Nullable
	public UUID getEpicDepartmentId() {
		return epicDepartmentId;
	}

	public void setEpicDepartmentId(@Nullable UUID epicDepartmentId) {
		this.epicDepartmentId = epicDepartmentId;
	}

	@Nullable
	public InstitutionId getInstitutionId() {
		return institutionId;
	}

	public void setInstitutionId(@Nullable InstitutionId institutionId) {
		this.institutionId = institutionId;
	}

	@Nullable
	public String getDepartmentId() {
		return departmentId;
	}

	public void setDepartmentId(@Nullable String departmentId) {
		this.departmentId = departmentId;
	}

	@Nullable
	public String getDepartmentIdType() {
		return departmentIdType;
	}

	public void setDepartmentIdType(@Nullable String departmentIdType) {
		this.departmentIdType = departmentIdType;
	}

	@Nullable
	public String getName() {
		return name;
	}

	public void setName(@Nullable String name) {
		this.name = name;
	}

	@Nullable
	public Instant getCreated() {
		return created;
	}

	public void setCreated(@Nullable Instant created) {
		this.created = created;
	}

	@Nullable
	public Instant getLastUpdated() {
		return lastUpdated;
	}

	public void setLastUpdated(@Nullable Instant lastUpdated) {
		this.lastUpdated = lastUpdated;
	}
}