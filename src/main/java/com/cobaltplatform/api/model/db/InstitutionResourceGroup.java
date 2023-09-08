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

import com.cobaltplatform.api.model.db.Color.ColorId;
import com.cobaltplatform.api.model.db.ColorValue.ColorValueId;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class InstitutionResourceGroup {
	@Nullable
	private UUID institutionResourceGroupId;
	@Nullable
	private InstitutionId institutionId;
	@Nullable
	private ColorValueId backgroundColorValueId;
	@Nullable
	private ColorValueId textColorValueId;
	@Nullable
	private String name;
	@Nullable
	private String urlName;
	@Nullable
	private String imageUrl;
	@Nullable
	private String description;
	@Nullable
	private Integer displayOrder;
	@Nullable
	private Instant created;
	@Nullable
	private Instant lastUpdated;

	// Provided by v_institution_resource_group

	@Nullable
	private ColorId backgroundColorId;
	@Nullable
	private String backgroundColorValueName;
	@Nullable
	private String backgroundColorValueCssRepresentation;
	@Nullable
	private ColorId textColorId;
	@Nullable
	private String textColorValueName;
	@Nullable
	private String textColorValueCssRepresentation;

	@Nullable
	public UUID getInstitutionResourceGroupId() {
		return this.institutionResourceGroupId;
	}

	public void setInstitutionResourceGroupId(@Nullable UUID institutionResourceGroupId) {
		this.institutionResourceGroupId = institutionResourceGroupId;
	}

	@Nullable
	public InstitutionId getInstitutionId() {
		return this.institutionId;
	}

	public void setInstitutionId(@Nullable InstitutionId institutionId) {
		this.institutionId = institutionId;
	}

	@Nullable
	public String getName() {
		return this.name;
	}

	public void setName(@Nullable String name) {
		this.name = name;
	}

	@Nullable
	public String getUrlName() {
		return this.urlName;
	}

	public void setUrlName(@Nullable String urlName) {
		this.urlName = urlName;
	}

	@Nullable
	public String getImageUrl() {
		return this.imageUrl;
	}

	public void setImageUrl(@Nullable String imageUrl) {
		this.imageUrl = imageUrl;
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

	@Nullable
	public ColorValueId getBackgroundColorValueId() {
		return this.backgroundColorValueId;
	}

	public void setBackgroundColorValueId(@Nullable ColorValueId backgroundColorValueId) {
		this.backgroundColorValueId = backgroundColorValueId;
	}

	@Nullable
	public ColorValueId getTextColorValueId() {
		return this.textColorValueId;
	}

	public void setTextColorValueId(@Nullable ColorValueId textColorValueId) {
		this.textColorValueId = textColorValueId;
	}

	@Nullable
	public ColorId getBackgroundColorId() {
		return this.backgroundColorId;
	}

	public void setBackgroundColorId(@Nullable ColorId backgroundColorId) {
		this.backgroundColorId = backgroundColorId;
	}

	@Nullable
	public String getBackgroundColorValueName() {
		return this.backgroundColorValueName;
	}

	public void setBackgroundColorValueName(@Nullable String backgroundColorValueName) {
		this.backgroundColorValueName = backgroundColorValueName;
	}

	@Nullable
	public String getBackgroundColorValueCssRepresentation() {
		return this.backgroundColorValueCssRepresentation;
	}

	public void setBackgroundColorValueCssRepresentation(@Nullable String backgroundColorValueCssRepresentation) {
		this.backgroundColorValueCssRepresentation = backgroundColorValueCssRepresentation;
	}

	@Nullable
	public ColorId getTextColorId() {
		return this.textColorId;
	}

	public void setTextColorId(@Nullable ColorId textColorId) {
		this.textColorId = textColorId;
	}

	@Nullable
	public String getTextColorValueName() {
		return this.textColorValueName;
	}

	public void setTextColorValueName(@Nullable String textColorValueName) {
		this.textColorValueName = textColorValueName;
	}

	@Nullable
	public String getTextColorValueCssRepresentation() {
		return this.textColorValueCssRepresentation;
	}

	public void setTextColorValueCssRepresentation(@Nullable String textColorValueCssRepresentation) {
		this.textColorValueCssRepresentation = textColorValueCssRepresentation;
	}
}