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

package com.cobaltplatform.api.integration.epic.request;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.LocalDate;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class PatientSearchRequest {
	@Nullable
	private String fhirId;
	@Nullable
	private String identifier;
	@Nullable
	private String family;
	@Nullable
	private String given;
	@Nullable
	private LocalDate birthdate;
	@Nullable
	private String telecom;
	@Nullable
	private Gender gender;

	public enum Gender {
		MALE("Male"),
		FEMALE("Female");

		@Nonnull
		private final String epicValue;

		private Gender(@Nonnull String epicValue) {
			requireNonNull(epicValue);
			this.epicValue = epicValue;
		}

		@Nonnull
		public String epicValue() {
			return epicValue;
		}
	}

	@Nullable
	public String getFhirId() {
		return this.fhirId;
	}

	public void setFhirId(@Nullable String fhirId) {
		this.fhirId = fhirId;
	}

	@Nullable
	public String getIdentifier() {
		return identifier;
	}

	public void setIdentifier(@Nullable String identifier) {
		this.identifier = identifier;
	}

	@Nullable
	public String getFamily() {
		return family;
	}

	public void setFamily(@Nullable String family) {
		this.family = family;
	}

	@Nullable
	public String getGiven() {
		return given;
	}

	public void setGiven(@Nullable String given) {
		this.given = given;
	}

	@Nullable
	public LocalDate getBirthdate() {
		return birthdate;
	}

	public void setBirthdate(@Nullable LocalDate birthdate) {
		this.birthdate = birthdate;
	}

	@Nullable
	public String getTelecom() {
		return telecom;
	}

	public void setTelecom(@Nullable String telecom) {
		this.telecom = telecom;
	}

	@Nullable
	public Gender getGender() {
		return gender;
	}

	public void setGender(@Nullable Gender gender) {
		this.gender = gender;
	}
}
