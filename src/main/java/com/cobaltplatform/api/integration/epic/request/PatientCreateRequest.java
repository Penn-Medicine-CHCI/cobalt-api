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

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.List;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class PatientCreateRequest {
	@Nullable
	private String MaritalStatus;
	@Nullable
	private String NationalIdentifier;
	@Nullable
	private String Race;
	@Nullable
	private String Religion;
	@Nullable
	private String DepartmentID;
	@Nullable
	private String DepartmentIDType;
	@Nullable
	private String Gender;
	@Nullable
	private String DateOfBirth;
	@Nullable
	private Address Address;
	@Nullable
	private Name Name;

	@NotThreadSafe
	public static class Address {
		@Nullable
		private String City;
		@Nullable
		private String Country;
		@Nullable
		private String County;
		@Nullable
		private String Email;
		@Nullable
		private String HouseNumber;
		@Nullable
		private String State;
		@Nullable
		private String Street;
		@Nullable
		private String StreetLine2;
		@Nullable
		private String ZipCode;
		@Nullable
		private List<Phone> Phones;

		@Nullable
		public String getCity() {
			return City;
		}

		public void setCity(@Nullable String city) {
			City = city;
		}

		@Nullable
		public String getCountry() {
			return Country;
		}

		public void setCountry(@Nullable String country) {
			Country = country;
		}

		@Nullable
		public String getCounty() {
			return County;
		}

		public void setCounty(@Nullable String county) {
			County = county;
		}

		@Nullable
		public String getEmail() {
			return Email;
		}

		public void setEmail(@Nullable String email) {
			Email = email;
		}

		@Nullable
		public String getHouseNumber() {
			return HouseNumber;
		}

		public void setHouseNumber(@Nullable String houseNumber) {
			HouseNumber = houseNumber;
		}

		@Nullable
		public String getState() {
			return State;
		}

		public void setState(@Nullable String state) {
			State = state;
		}

		@Nullable
		public String getStreet() {
			return Street;
		}

		public void setStreet(@Nullable String street) {
			Street = street;
		}

		@Nullable
		public String getStreetLine2() {
			return StreetLine2;
		}

		public void setStreetLine2(@Nullable String streetLine2) {
			StreetLine2 = streetLine2;
		}

		@Nullable
		public String getZipCode() {
			return ZipCode;
		}

		public void setZipCode(@Nullable String zipCode) {
			ZipCode = zipCode;
		}

		@Nullable
		public List<Phone> getPhones() {
			return Phones;
		}

		public void setPhones(@Nullable List<Phone> phones) {
			Phones = phones;
		}
	}

	@NotThreadSafe
	public static class Phone {
		@Nullable
		private String Number;
		@Nullable
		private String Type;

		@Nullable
		public String getNumber() {
			return Number;
		}

		public void setNumber(@Nullable String number) {
			Number = number;
		}

		@Nullable
		public String getType() {
			return Type;
		}

		public void setType(@Nullable String type) {
			Type = type;
		}
	}

	@NotThreadSafe
	public static class Name {
		@Nullable
		private String Academic;
		@Nullable
		private String First;
		@Nullable
		private String GivenNameInitials;
		@Nullable
		private String LastName;
		@Nullable
		private String LastNameFromSpouse;
		@Nullable
		private String LastNamePrefix;
		@Nullable
		private String Middle;
		@Nullable
		private String PreferredName;
		@Nullable
		private String PreferredNameType;
		@Nullable
		private String SpouseLastNameFirst;
		@Nullable
		private String SpouseLastNamePrefix;
		@Nullable
		private String Suffix;
		@Nullable
		private String Title;

		@Nullable
		public String getAcademic() {
			return Academic;
		}

		public void setAcademic(@Nullable String academic) {
			Academic = academic;
		}

		@Nullable
		public String getFirst() {
			return First;
		}

		public void setFirst(@Nullable String first) {
			First = first;
		}

		@Nullable
		public String getGivenNameInitials() {
			return GivenNameInitials;
		}

		public void setGivenNameInitials(@Nullable String givenNameInitials) {
			GivenNameInitials = givenNameInitials;
		}

		@Nullable
		public String getLastName() {
			return LastName;
		}

		public void setLastName(@Nullable String lastName) {
			LastName = lastName;
		}

		@Nullable
		public String getLastNameFromSpouse() {
			return LastNameFromSpouse;
		}

		public void setLastNameFromSpouse(@Nullable String lastNameFromSpouse) {
			LastNameFromSpouse = lastNameFromSpouse;
		}

		@Nullable
		public String getLastNamePrefix() {
			return LastNamePrefix;
		}

		public void setLastNamePrefix(@Nullable String lastNamePrefix) {
			LastNamePrefix = lastNamePrefix;
		}

		@Nullable
		public String getMiddle() {
			return Middle;
		}

		public void setMiddle(@Nullable String middle) {
			Middle = middle;
		}

		@Nullable
		public String getPreferredName() {
			return PreferredName;
		}

		public void setPreferredName(@Nullable String preferredName) {
			PreferredName = preferredName;
		}

		@Nullable
		public String getPreferredNameType() {
			return PreferredNameType;
		}

		public void setPreferredNameType(@Nullable String preferredNameType) {
			PreferredNameType = preferredNameType;
		}

		@Nullable
		public String getSpouseLastNameFirst() {
			return SpouseLastNameFirst;
		}

		public void setSpouseLastNameFirst(@Nullable String spouseLastNameFirst) {
			SpouseLastNameFirst = spouseLastNameFirst;
		}

		@Nullable
		public String getSpouseLastNamePrefix() {
			return SpouseLastNamePrefix;
		}

		public void setSpouseLastNamePrefix(@Nullable String spouseLastNamePrefix) {
			SpouseLastNamePrefix = spouseLastNamePrefix;
		}

		@Nullable
		public String getSuffix() {
			return Suffix;
		}

		public void setSuffix(@Nullable String suffix) {
			Suffix = suffix;
		}

		@Nullable
		public String getTitle() {
			return Title;
		}

		public void setTitle(@Nullable String title) {
			Title = title;
		}
	}

	@Nullable
	public String getMaritalStatus() {
		return MaritalStatus;
	}

	public void setMaritalStatus(@Nullable String maritalStatus) {
		MaritalStatus = maritalStatus;
	}

	@Nullable
	public String getNationalIdentifier() {
		return NationalIdentifier;
	}

	public void setNationalIdentifier(@Nullable String nationalIdentifier) {
		NationalIdentifier = nationalIdentifier;
	}

	@Nullable
	public String getRace() {
		return Race;
	}

	public void setRace(@Nullable String race) {
		Race = race;
	}

	@Nullable
	public String getReligion() {
		return Religion;
	}

	public void setReligion(@Nullable String religion) {
		Religion = religion;
	}

	@Nullable
	public String getDepartmentID() {
		return DepartmentID;
	}

	public void setDepartmentID(@Nullable String departmentID) {
		DepartmentID = departmentID;
	}

	@Nullable
	public String getDepartmentIDType() {
		return DepartmentIDType;
	}

	public void setDepartmentIDType(@Nullable String departmentIDType) {
		DepartmentIDType = departmentIDType;
	}

	@Nullable
	public String getGender() {
		return Gender;
	}

	public void setGender(@Nullable String gender) {
		Gender = gender;
	}

	@Nullable
	public String getDateOfBirth() {
		return DateOfBirth;
	}

	public void setDateOfBirth(@Nullable String dateOfBirth) {
		DateOfBirth = dateOfBirth;
	}

	@Nullable
	public PatientCreateRequest.Address getAddress() {
		return Address;
	}

	public void setAddress(@Nullable PatientCreateRequest.Address address) {
		Address = address;
	}

	@Nullable
	public PatientCreateRequest.Name getName() {
		return Name;
	}

	public void setName(@Nullable PatientCreateRequest.Name name) {
		Name = name;
	}
}
