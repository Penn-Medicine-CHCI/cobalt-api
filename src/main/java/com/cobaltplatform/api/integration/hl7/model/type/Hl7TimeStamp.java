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

import ca.uhn.hl7v2.model.DataTypeException;
import ca.uhn.hl7v2.model.v251.datatype.TS;
import com.cobaltplatform.api.integration.hl7.UncheckedHl7ParsingException;
import com.cobaltplatform.api.integration.hl7.model.Hl7Object;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;

import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * See https://hl7-definition.caristix.com/v2/hl7v2.5.1/DataTypes/TS
 *
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class Hl7TimeStamp extends Hl7Object {
	@Nullable
	private Instant time;
	@Nullable
	private String degreeOfPrecision;

	@Nonnull
	public static Boolean isPresent(@Nullable TS ts) {
		if (ts == null)
			return false;

		try {
			return ts.getTime().getValueAsDate() != null
					|| trimToNull(ts.getDegreeOfPrecision().getValueOrEmpty()) != null;
		} catch (DataTypeException e) {
			throw new UncheckedHl7ParsingException("Unable to parse time value", e);
		}
	}

	public Hl7TimeStamp() {
		// Nothing to do
	}

	public Hl7TimeStamp(@Nullable TS ts) {
		if (ts != null) {
			try {
				this.time = ts == null ? null : ts.getTime().getValueAsDate().toInstant();
				this.degreeOfPrecision = trimToNull(ts.getDegreeOfPrecision().getValueOrEmpty());
			} catch (DataTypeException e) {
				throw new UncheckedHl7ParsingException("Unable to parse time value", e);
			}
		}
	}

	@Nullable
	public Instant getTime() {
		return this.time;
	}

	public void setTime(@Nullable Instant time) {
		this.time = time;
	}

	@Nullable
	public String getDegreeOfPrecision() {
		return this.degreeOfPrecision;
	}

	public void setDegreeOfPrecision(@Nullable String degreeOfPrecision) {
		this.degreeOfPrecision = degreeOfPrecision;
	}
}