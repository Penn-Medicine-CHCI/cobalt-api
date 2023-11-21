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
import java.util.UUID;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;

/**
 * @author Transmogrify LLC.
 */
@NotThreadSafe
public class InstitutionContent {
	@Nullable
	private InstitutionId institutionId;

	@Nullable
	private UUID institutionContentId;

	@Nullable
	private UUID contentId;

	@Nullable
	private String institutionName;

	@Nullable
	public InstitutionId getInstitutionId() {
		return institutionId;
	}

	public void setInstitutionId(@Nullable InstitutionId institutionId) {
		this.institutionId = institutionId;
	}

	@Nullable
	public UUID getInstitutionContentId() {
		return institutionContentId;
	}

	public void setInstitutionContentId(@Nullable UUID institutionContentId) {
		this.institutionContentId = institutionContentId;
	}

	@Nullable
	public UUID getContentId() {
		return contentId;
	}

	public void setContentId(@Nullable UUID contentId) {
		this.contentId = contentId;
	}

	@Nullable
	public String getInstitutionName() {
		return institutionName;
	}

	public void setInstitutionName(@Nullable String institutionName) {
		this.institutionName = institutionName;
	}
}