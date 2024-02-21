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

package com.cobaltplatform.api.integration.hl7.model.type;

import ca.uhn.hl7v2.model.v251.datatype.VID;
import com.cobaltplatform.api.integration.hl7.model.Hl7Object;
import com.cobaltplatform.api.integration.hl7.model.type.Hl7CodedElement;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * See https://hl7-definition.caristix.com/v2/hl7v2.5.1/DataTypes/VID
 *
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class Hl7VersionId extends Hl7Object {
	@Nullable
	private String versionId;
	@Nullable
	private Hl7CodedElement internationalizationCode;
	@Nullable
	private Hl7CodedElement internationalVersionId;

	@Nonnull
	public static Boolean isPresent(@Nullable VID vid) {
		if (vid == null)
			return false;

		return trimToNull(vid.getVersionID().getValueOrEmpty()) != null
				|| Hl7CodedElement.isPresent(vid.getInternationalizationCode())
				|| Hl7CodedElement.isPresent(vid.getInternationalVersionID());
	}

	public Hl7VersionId() {
		// Nothing to do
	}

	public Hl7VersionId(@Nullable VID vid) {
		if (vid != null) {
			this.versionId = trimToNull(vid.getVersionID().getValueOrEmpty());

			if (Hl7CodedElement.isPresent(vid.getInternationalizationCode()))
				this.internationalizationCode = new Hl7CodedElement(vid.getInternationalizationCode());

			if (Hl7CodedElement.isPresent(vid.getInternationalVersionID()))
				this.internationalVersionId = new Hl7CodedElement(vid.getInternationalVersionID());
		}
	}

	@Nullable
	public String getVersionId() {
		return this.versionId;
	}

	public void setVersionId(@Nullable String versionId) {
		this.versionId = versionId;
	}

	@Nullable
	public Hl7CodedElement getInternationalizationCode() {
		return this.internationalizationCode;
	}

	public void setInternationalizationCode(@Nullable Hl7CodedElement internationalizationCode) {
		this.internationalizationCode = internationalizationCode;
	}

	@Nullable
	public Hl7CodedElement getInternationalVersionId() {
		return this.internationalVersionId;
	}

	public void setInternationalVersionId(@Nullable Hl7CodedElement internationalVersionId) {
		this.internationalVersionId = internationalVersionId;
	}
}