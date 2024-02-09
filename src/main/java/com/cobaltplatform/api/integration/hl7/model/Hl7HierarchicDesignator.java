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

package com.cobaltplatform.api.integration.hl7.model;

import ca.uhn.hl7v2.model.v251.datatype.HD;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * See https://hl7-definition.caristix.com/v2/hl7v2.5.1/DataTypes/HD
 *
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class Hl7HierarchicDesignator extends Hl7Object {
	@Nullable
	private String namespaceId;
	@Nullable
	private String universalId;
	@Nullable
	private String universalIdType;

	@Nonnull
	public static Boolean isPresent(@Nullable HD hd) {
		if (hd == null)
			return false;

		return trimToNull(hd.getNamespaceID().getValueOrEmpty()) != null
				|| trimToNull(hd.getUniversalID().getValueOrEmpty()) != null
				|| trimToNull(hd.getUniversalIDType().getValueOrEmpty()) != null;
	}

	public Hl7HierarchicDesignator() {
		// Nothing to do
	}

	public Hl7HierarchicDesignator(@Nullable HD hd) {
		if (hd != null) {
			this.namespaceId = trimToNull(hd.getNamespaceID().getValueOrEmpty());
			this.universalId = trimToNull(hd.getUniversalID().getValueOrEmpty());
			this.universalIdType = trimToNull(hd.getUniversalIDType().getValueOrEmpty());
		}
	}

	@Nullable
	public String getNamespaceId() {
		return this.namespaceId;
	}

	public void setNamespaceId(@Nullable String namespaceId) {
		this.namespaceId = namespaceId;
	}

	@Nullable
	public String getUniversalId() {
		return this.universalId;
	}

	public void setUniversalId(@Nullable String universalId) {
		this.universalId = universalId;
	}

	@Nullable
	public String getUniversalIdType() {
		return this.universalIdType;
	}

	public void setUniversalIdType(@Nullable String universalIdType) {
		this.universalIdType = universalIdType;
	}
}