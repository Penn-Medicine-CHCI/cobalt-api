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

import com.cobaltplatform.api.model.db.DepartmentAvailabilityStatus.DepartmentAvailabilityStatusId;
import com.cobaltplatform.api.model.db.EpicDepartment;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
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
public class EpicDepartmentApiResponse {
	@Nonnull
	private final UUID epicDepartmentId;
	@Nonnull
	private final InstitutionId institutionId;
	@Nonnull
	private final DepartmentAvailabilityStatusId departmentAvailabilityStatusId;
	@Nonnull
	private final String departmentId;
	@Nonnull
	private final String departmentIdType;
	@Nonnull
	private final String name;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface EpicDepartmentApiResponseFactory {
		@Nonnull
		EpicDepartmentApiResponse create(@Nonnull EpicDepartment epicDepartment);
	}

	@AssistedInject
	public EpicDepartmentApiResponse(@Assisted @Nonnull EpicDepartment epicDepartment) {
		requireNonNull(epicDepartment);

		this.epicDepartmentId = epicDepartment.getEpicDepartmentId();
		this.institutionId = epicDepartment.getInstitutionId();
		this.departmentAvailabilityStatusId = epicDepartment.getDepartmentAvailabilityStatusId();
		this.departmentId = epicDepartment.getDepartmentId();
		this.departmentIdType = epicDepartment.getDepartmentIdType();
		this.name = epicDepartment.getName();
	}

	@Nonnull
	public UUID getEpicDepartmentId() {
		return this.epicDepartmentId;
	}

	@Nonnull
	public InstitutionId getInstitutionId() {
		return this.institutionId;
	}

	@Nonnull
	public DepartmentAvailabilityStatusId getDepartmentAvailabilityStatusId() {
		return this.departmentAvailabilityStatusId;
	}

	@Nonnull
	public String getDepartmentId() {
		return this.departmentId;
	}

	@Nonnull
	public String getDepartmentIdType() {
		return this.departmentIdType;
	}

	@Nonnull
	public String getName() {
		return this.name;
	}
}