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

import static java.lang.String.format;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class GenderIdentity {
	@Nullable
	private GenderIdentityId genderIdentityId;
	@Nullable
	private String description;
	@Nullable
	private Integer displayOrder;

	public enum GenderIdentityId {
		NOT_ASKED,
		MALE,
		FEMALE,
		TRANSGENDER_MTF,
		TRANSGENDER_FTM,
		NON_BINARY,
		OTHER,
		NOT_DISCLOSED
	}

	@Override
	public String toString() {
		return format("%s{genderIdentityId=%s, description=%s}", getClass().getSimpleName(), getGenderIdentityId(), getDescription());
	}

	@Nullable
	public GenderIdentityId getGenderIdentityId() {
		return this.genderIdentityId;
	}

	public void setGenderIdentityId(@Nullable GenderIdentityId genderIdentityId) {
		this.genderIdentityId = genderIdentityId;
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
