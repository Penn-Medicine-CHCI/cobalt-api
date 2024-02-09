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

import ca.uhn.hl7v2.model.v251.datatype.PT;
import com.cobaltplatform.api.integration.hl7.model.Hl7Object;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * See https://hl7-definition.caristix.com/v2/hl7v2.5.1/DataTypes/PT
 *
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class Hl7ProcessingType extends Hl7Object {
	@Nullable
	private String processingId;
	@Nullable
	private String processingMode;

	@Nonnull
	public static Boolean isPresent(@Nullable PT pt) {
		if (pt == null)
			return false;

		return trimToNull(pt.getProcessingID().getValueOrEmpty()) != null
				|| trimToNull(pt.getProcessingMode().getValueOrEmpty()) != null;
	}

	public Hl7ProcessingType() {
		// Nothing to do
	}

	public Hl7ProcessingType(@Nullable PT pt) {
		if (pt != null) {
			this.processingId = trimToNull(pt.getProcessingID().getValueOrEmpty());
			this.processingMode = trimToNull(pt.getProcessingMode().getValueOrEmpty());
		}
	}

	@Nullable
	public String getProcessingId() {
		return this.processingId;
	}

	public void setProcessingId(@Nullable String processingId) {
		this.processingId = processingId;
	}

	@Nullable
	public String getProcessingMode() {
		return this.processingMode;
	}

	public void setProcessingMode(@Nullable String processingMode) {
		this.processingMode = processingMode;
	}
}