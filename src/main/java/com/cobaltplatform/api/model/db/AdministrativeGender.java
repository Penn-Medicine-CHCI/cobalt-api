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

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class AdministrativeGender {
	@Nullable
	private AdministrativeGenderId administrativeGenderId;
	@Nullable
	private String description;
	@Nullable
	private Integer displayOrder;

	// See https://hl7.org/fhir/R4/valueset-administrative-gender.html
	public enum AdministrativeGenderId {
		NOT_ASKED,
		MALE,
		FEMALE,
		OTHER,
		UNKNOWN,
		NOT_DISCLOSED
	}

	@Override
	public String toString() {
		return String.format("%s{administrativeGenderId=%s, description=%s}", getClass().getSimpleName(), getAdministrativeGenderId(), getDescription());
	}

	@Nullable
	public AdministrativeGenderId getAdministrativeGenderId() {
		return this.administrativeGenderId;
	}

	public void setAdministrativeGenderId(@Nullable AdministrativeGenderId administrativeGenderId) {
		this.administrativeGenderId = administrativeGenderId;
	}

	@Nullable
	public String getDescription() {
		return this.description;
	}

	public void setDescription(@Nullable String description) {
		this.description = description;
	}

	@Nullable
	public Integer getDisplayOrder() {
		return this.displayOrder;
	}

	public void setDisplayOrder(@Nullable Integer displayOrder) {
		this.displayOrder = displayOrder;
	}
}
