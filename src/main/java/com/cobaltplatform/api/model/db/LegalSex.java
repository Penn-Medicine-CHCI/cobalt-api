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
public class LegalSex {
	@Nullable
	private LegalSexId legalSexId;
	@Nullable
	private String description;
	@Nullable
	private Integer displayOrder;

	// See See https://loinc.org/72143-1
	public enum LegalSexId {
		NOT_ASKED,
		FEMALE, // LA3-6
		MALE, // LA2-8
		UNDIFFERENTIATED, // LA18959-9
		NOT_DISCLOSED
	}

	@Override
	public String toString() {
		return String.format("%s{legalSexId=%s, description=%s}", getClass().getSimpleName(), getLegalSexId(), getDescription());
	}

	@Nullable
	public LegalSexId getLegalSexId() {
		return this.legalSexId;
	}

	public void setLegalSexId(@Nullable LegalSexId legalSexId) {
		this.legalSexId = legalSexId;
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
