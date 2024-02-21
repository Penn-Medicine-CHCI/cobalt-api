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

import ca.uhn.hl7v2.model.v251.datatype.DR;
import com.cobaltplatform.api.integration.hl7.model.Hl7Object;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * See https://hl7-definition.caristix.com/v2/HL7v2.5.1/DataTypes/DR
 *
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class Hl7DateTimeRange extends Hl7Object {
	@Nullable
	private Hl7TimeStamp rangeStartDateTime; // DR.1 - Range Start Date/Time
	@Nullable
	private Hl7TimeStamp rangeEndDateTime; // DR.2 - Range End Date/Time

	@Nonnull
	public static Boolean isPresent(@Nullable DR dr) {
		if (dr == null)
			return false;

		return Hl7TimeStamp.isPresent(dr.getRangeStartDateTime())
				|| Hl7TimeStamp.isPresent(dr.getRangeEndDateTime());
	}

	public Hl7DateTimeRange() {
		// Nothing to do
	}

	public Hl7DateTimeRange(@Nullable DR dr) {
		if (dr != null) {
			this.rangeStartDateTime = Hl7TimeStamp.isPresent(dr.getRangeStartDateTime()) ? new Hl7TimeStamp(dr.getRangeStartDateTime()) : null;
			this.rangeEndDateTime = Hl7TimeStamp.isPresent(dr.getRangeEndDateTime()) ? new Hl7TimeStamp(dr.getRangeEndDateTime()) : null;
		}
	}

	@Nullable
	public Hl7TimeStamp getRangeStartDateTime() {
		return this.rangeStartDateTime;
	}

	public void setRangeStartDateTime(@Nullable Hl7TimeStamp rangeStartDateTime) {
		this.rangeStartDateTime = rangeStartDateTime;
	}

	@Nullable
	public Hl7TimeStamp getRangeEndDateTime() {
		return this.rangeEndDateTime;
	}

	public void setRangeEndDateTime(@Nullable Hl7TimeStamp rangeEndDateTime) {
		this.rangeEndDateTime = rangeEndDateTime;
	}
}