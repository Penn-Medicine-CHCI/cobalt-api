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
public class PreferredPronoun {
	@Nullable
	private PreferredPronounId preferredPronounId;
	@Nullable
	private String description;
	@Nullable
	private Integer displayOrder;

	public enum PreferredPronounId {
		NOT_ASKED,
		HE_HIM_HIS_HIS_HIMSELF, // LA29518-0
		SHE_HER_HER_HERS_HERSELF, // LA29519-8
		THEY_THEM_THEIR_THEIRS_THEMSELVES, // LA29520-6
		ZE_ZIR_ZIR_ZIRS_ZIRSELF, // LA29523-0
		XIE_HIR_HERE_HIR_HIRS_HIRSELF, // LA29521-4
		CO_CO_COS_COS_COSELF, // LA29515-6
		EN_EN_ENS_ENS_ENSELF, // LA29516-4
		EY_EM_EIR_EIRS_EMSELF, // LA29517-2
		YO_YO_YOS_YOS_YOSELF, // LA29522-2
		VE_VIS_VER_VER_VERSELF, // LA29524-8
		DO_NOT_USE_PRONOUNS, // EP-PS-DO-NOT-USE-PRONOUNS
		OTHER,
		NOT_DISCLOSED
	}

	@Override
	public String toString() {
		return String.format("%s{preferredPronounId=%s, description=%s}", getClass().getSimpleName(), getPreferredPronounId(), getDescription());
	}

	@Nullable
	public PreferredPronounId getPreferredPronounId() {
		return this.preferredPronounId;
	}

	public void setPreferredPronounId(@Nullable PreferredPronounId preferredPronounId) {
		this.preferredPronounId = preferredPronounId;
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
