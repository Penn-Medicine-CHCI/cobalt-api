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

import ca.uhn.hl7v2.model.v251.datatype.AUI;
import com.cobaltplatform.api.integration.hl7.model.Hl7Object;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import static com.soklet.util.StringUtils.trimToNull;

/**
 * See https://hl7-definition.caristix.com/v2/hl7v2.5.1/DataTypes/AUI
 *
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class Hl7AuthorizationInformation extends Hl7Object {
	@Nullable
	private String authorizationNumber; // AUI.1 - Authorization Number
	@Nullable
	private String date; // AUI.2 - Date
	@Nullable
	private String source; // AUI.3 - Source

	@Nonnull
	public static Boolean isPresent(@Nullable AUI aui) {
		if (aui == null)
			return false;

		return trimToNull(aui.getAuthorizationNumber().getValueOrEmpty()) != null
				|| trimToNull(aui.getDate().getValue()) != null
				|| trimToNull(aui.getSource().getValueOrEmpty()) != null;
	}

	public Hl7AuthorizationInformation() {
		// Nothing to do
	}

	public Hl7AuthorizationInformation(@Nullable AUI aui) {
		if (aui != null) {
			this.authorizationNumber = trimToNull(aui.getAuthorizationNumber().getValueOrEmpty());
			this.date = trimToNull(aui.getDate().getValue());
			this.source = trimToNull(aui.getSource().getValueOrEmpty());
		}
	}

	@Nullable
	public String getAuthorizationNumber() {
		return this.authorizationNumber;
	}

	public void setAuthorizationNumber(@Nullable String authorizationNumber) {
		this.authorizationNumber = authorizationNumber;
	}

	@Nullable
	public String getDate() {
		return this.date;
	}

	public void setDate(@Nullable String date) {
		this.date = date;
	}

	@Nullable
	public String getSource() {
		return this.source;
	}

	public void setSource(@Nullable String source) {
		this.source = source;
	}
}