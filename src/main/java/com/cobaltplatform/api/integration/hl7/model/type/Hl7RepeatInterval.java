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

import ca.uhn.hl7v2.model.v251.datatype.RI;
import com.cobaltplatform.api.integration.hl7.model.Hl7Object;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * See https://hl7-definition.caristix.com/v2/hl7v2.5.1/DataTypes/RI
 *
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class Hl7RepeatInterval extends Hl7Object {
	@Nullable
	private String repeatPattern;
	@Nullable
	private String explicitTimeInterval;

	@Nonnull
	public static Boolean isPresent(@Nullable RI ri) {
		if (ri == null)
			return false;

		return trimToNull(ri.getRepeatPattern().getValueOrEmpty()) != null
				|| trimToNull(ri.getExplicitTimeInterval().getValueOrEmpty()) != null;
	}

	public Hl7RepeatInterval() {
		// Nothing to do
	}

	public Hl7RepeatInterval(@Nullable RI ri) {
		if (ri != null) {
			this.repeatPattern = trimToNull(ri.getRepeatPattern().getValueOrEmpty());
			this.explicitTimeInterval = trimToNull(ri.getExplicitTimeInterval().getValueOrEmpty());
		}
	}

	@Nullable
	public String getRepeatPattern() {
		return this.repeatPattern;
	}

	public void setRepeatPattern(@Nullable String repeatPattern) {
		this.repeatPattern = repeatPattern;
	}

	@Nullable
	public String getExplicitTimeInterval() {
		return this.explicitTimeInterval;
	}

	public void setExplicitTimeInterval(@Nullable String explicitTimeInterval) {
		this.explicitTimeInterval = explicitTimeInterval;
	}
}