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

package com.cobaltplatform.api.integration.epic.matching;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.LocalDate;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
public class EpicPatientMatchRequest {
	@Nullable
	private String firstName;
	@Nullable
	private String lastName;
	@Nullable
	private String middleInitial;
	@Nullable
	private LocalDate dateOfBirth;
	@Nullable
	private Address address;
	@Nullable
	private String emailAddress;
	@Nullable
	private String phoneNumber;
	@Nullable
	private String phoneType;
	@Nullable
	private String nationalIdentifier;
	@Nullable
	private Gender gender;

	@Nullable
	private UUID providerId;
	@Nullable
	private UUID epicDepartmentId;
	@Nullable
	private Boolean applyToCurrentAccount;
	@Nullable
	private MatchStep matchStep;

	public enum Gender {
		MALE,
		FEMALE
	}

	public enum MatchStep {
		STEP_1(1),
		STEP_2(2),
		STEP_3(3),
		FINISH(4);

		@Nonnull
		private final Integer value;

		MatchStep(@Nonnull Integer value) {
			this.value = value;
		}

		@Nonnull
		public Integer getValue() {
			return value;
		}
	}

	@NotThreadSafe
	public static class Address {
		@Nullable
		private String line1;
		@Nullable
		private String line2;
		@Nullable
		private String city;
		@Nullable
		private String state;
		@Nullable
		private String postalCode;
		@Nullable
		private String country;

		@Nullable
		public String getLine1() {
			return line1;
		}

		public void setLine1(@Nullable String line1) {
			this.line1 = line1;
		}

		@Nullable
		public String getLine2() {
			return line2;
		}

		public void setLine2(@Nullable String line2) {
			this.line2 = line2;
		}

		@Nullable
		public String getCity() {
			return city;
		}

		public void setCity(@Nullable String city) {
			this.city = city;
		}

		@Nullable
		public String getState() {
			return state;
		}

		public void setState(@Nullable String state) {
			this.state = state;
		}

		@Nullable
		public String getPostalCode() {
			return postalCode;
		}

		public void setPostalCode(@Nullable String postalCode) {
			this.postalCode = postalCode;
		}

		@Nullable
		public String getCountry() {
			return country;
		}

		public void setCountry(@Nullable String country) {
			this.country = country;
		}
	}

	@Nullable
	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(@Nullable String firstName) {
		this.firstName = firstName;
	}

	@Nullable
	public String getLastName() {
		return lastName;
	}

	public void setLastName(@Nullable String lastName) {
		this.lastName = lastName;
	}

	@Nullable
	public String getMiddleInitial() {
		return middleInitial;
	}

	public void setMiddleInitial(@Nullable String middleInitial) {
		this.middleInitial = middleInitial;
	}

	@Nullable
	public LocalDate getDateOfBirth() {
		return dateOfBirth;
	}

	public void setDateOfBirth(@Nullable LocalDate dateOfBirth) {
		this.dateOfBirth = dateOfBirth;
	}

	@Nullable
	public Address getAddress() {
		return address;
	}

	public void setAddress(@Nullable Address address) {
		this.address = address;
	}

	@Nullable
	public String getEmailAddress() {
		return emailAddress;
	}

	public void setEmailAddress(@Nullable String emailAddress) {
		this.emailAddress = emailAddress;
	}

	@Nullable
	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(@Nullable String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	@Nullable
	public String getPhoneType() {
		return phoneType;
	}

	public void setPhoneType(@Nullable String phoneType) {
		this.phoneType = phoneType;
	}

	@Nullable
	public String getNationalIdentifier() {
		return nationalIdentifier;
	}

	public void setNationalIdentifier(@Nullable String nationalIdentifier) {
		this.nationalIdentifier = nationalIdentifier;
	}

	@Nullable
	public Gender getGender() {
		return gender;
	}

	public void setGender(@Nullable Gender gender) {
		this.gender = gender;
	}

	@Nullable
	public Boolean getApplyToCurrentAccount() {
		return applyToCurrentAccount;
	}

	public void setApplyToCurrentAccount(@Nullable Boolean applyToCurrentAccount) {
		this.applyToCurrentAccount = applyToCurrentAccount;
	}

	@Nullable
	public UUID getProviderId() {
		return providerId;
	}

	public void setProviderId(@Nullable UUID providerId) {
		this.providerId = providerId;
	}

	@Nullable
	public UUID getEpicDepartmentId() {
		return epicDepartmentId;
	}

	public void setEpicDepartmentId(@Nullable UUID epicDepartmentId) {
		this.epicDepartmentId = epicDepartmentId;
	}

	@Nullable
	public MatchStep getMatchStep() {
		return matchStep;
	}

	public void setMatchStep(@Nullable MatchStep matchStep) {
		this.matchStep = matchStep;
	}
}
