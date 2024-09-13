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

import com.cobaltplatform.api.model.db.ContentAudienceTypeGroup.ContentAudienceTypeGroupId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import static java.lang.String.format;

/**
 * @author Transmogrify LLC.
 */
@NotThreadSafe
public class ContentAudienceType {
	@Nullable
	private ContentAudienceTypeId contentAudienceTypeId;
	@Nullable
	private ContentAudienceTypeGroupId contentAudienceTypeGroupId;
	@Nullable
	private String description;
	@Nullable
	private String patientRepresentation;
	@Nullable
	private String urlName;
	@Nullable
	private Integer displayOrder;

	public enum ContentAudienceTypeId {
		MYSELF,
		CHILD,
		TEEN,
		ADULT_CHILD,
		SPOUSE,
		PARENT
	}

	@Override
	public String toString() {
		return format("%s{contentAudienceTypeId=%s, description=%s}", getClass().getSimpleName(), getContentAudienceTypeId(), getDescription());
	}

	@Nullable
	public ContentAudienceTypeId getContentAudienceTypeId() {
		return this.contentAudienceTypeId;
	}

	public void setContentAudienceTypeId(@Nullable ContentAudienceTypeId contentAudienceTypeId) {
		this.contentAudienceTypeId = contentAudienceTypeId;
	}

	@Nullable
	public ContentAudienceTypeGroupId getContentAudienceTypeGroupId() {
		return this.contentAudienceTypeGroupId;
	}

	public void setContentAudienceTypeGroupId(@Nullable ContentAudienceTypeGroupId contentAudienceTypeGroupId) {
		this.contentAudienceTypeGroupId = contentAudienceTypeGroupId;
	}

	@Nullable
	public String getDescription() {
		return this.description;
	}

	public void setDescription(@Nullable String description) {
		this.description = description;
	}

	@Nullable
	public String getPatientRepresentation() {
		return this.patientRepresentation;
	}

	public void setPatientRepresentation(@Nullable String patientRepresentation) {
		this.patientRepresentation = patientRepresentation;
	}

	@Nullable
	public String getUrlName() {
		return this.urlName;
	}

	public void setUrlName(@Nullable String urlName) {
		this.urlName = urlName;
	}

	@Nullable
	public Integer getDisplayOrder() {
		return this.displayOrder;
	}

	public void setDisplayOrder(@Nullable Integer displayOrder) {
		this.displayOrder = displayOrder;
	}
}