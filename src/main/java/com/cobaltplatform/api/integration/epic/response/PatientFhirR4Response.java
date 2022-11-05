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

package com.cobaltplatform.api.integration.epic.response;

import com.cobaltplatform.api.integration.epic.code.AddressUseCode;
import com.cobaltplatform.api.integration.epic.code.BirthSexCode;
import com.cobaltplatform.api.integration.epic.code.EthnicityCode;
import com.cobaltplatform.api.integration.epic.code.GenderIdentityCode;
import com.cobaltplatform.api.integration.epic.code.NameUseCode;
import com.cobaltplatform.api.integration.epic.code.RaceCode;
import com.cobaltplatform.api.integration.epic.code.TelecomUseCode;
import com.cobaltplatform.api.integration.epic.response.PatientFhirR4Response.Extension.ExtensionInner;
import com.cobaltplatform.api.integration.epic.response.PatientFhirR4Response.Identifier.Type;
import com.cobaltplatform.api.model.db.BirthSex.BirthSexId;
import com.cobaltplatform.api.model.db.Ethnicity.EthnicityId;
import com.cobaltplatform.api.model.db.GenderIdentity.GenderIdentityId;
import com.cobaltplatform.api.model.db.Race.RaceId;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class PatientFhirR4Response {
	@Nullable
	private String rawJson;

	@Nullable
	private String id;
	@Nullable
	private String resourceType;
	@Nullable
	private List<GeneralPractitioner> generalPractitioner;
	@Nullable
	private ManagingOrganization managingOrganization;
	@Nullable
	private List<Communication> communication;
	@Nullable
	private MaritalStatus maritalStatus;
	@Nullable
	private String gender;
	@Nullable
	private LocalDate birthDate;
	@Nullable
	private Boolean deceasedBoolean;
	@Nullable
	private List<Address> address;
	@Nullable
	private List<Telecom> telecom;
	@Nullable
	private List<Name> name;
	@Nullable
	private Boolean active;
	@Nullable
	private List<Identifier> identifier;
	@Nullable
	private List<Extension> extension;
	@Nullable
	private List<Contact> contact;

	@Nonnull
	public Optional<String> extractIdentifierByType(@Nullable String type) {
		type = trimToNull(type);

		if (type == null)
			return Optional.empty();

		for (Identifier identifier : getIdentifier()) {
			Type identifierType = identifier.getType();

			if (identifierType != null && Objects.equals(identifierType.getText(), type))
				return Optional.ofNullable(identifier.getValue());
		}

		return Optional.empty();
	}

	@Nonnull
	public Optional<GenderIdentityId> extractGenderIdentityId() {
		if (getExtension() == null)
			return Optional.empty();

		Extension matchingExtension = getExtension().stream()
				.filter(extension -> Objects.equals(GenderIdentityCode.EXTENSION_URL, extension.getUrl()))
				.findFirst().orElse(null);

		if (matchingExtension == null || matchingExtension.getValueCodeableConcept() == null)
			return Optional.empty();

		Coding coding = matchingExtension.getValueCodeableConcept().getCoding().stream().findFirst().orElse(null);

		if (coding == null)
			return Optional.empty();

		String code = coding.getCode();

		if (code == null)
			return Optional.empty();

		GenderIdentityCode genderIdentityCode = GenderIdentityCode.fromFhirValue(code).orElse(null);

		if (genderIdentityCode != null) {
			if (genderIdentityCode == GenderIdentityCode.FEMALE)
				return Optional.of(GenderIdentityId.FEMALE);
			if (genderIdentityCode == GenderIdentityCode.MALE)
				return Optional.of(GenderIdentityId.MALE);
			if (genderIdentityCode == GenderIdentityCode.TRANSGENDER_FEMALE)
				return Optional.of(GenderIdentityId.TRANSGENDER_MTF);
			if (genderIdentityCode == GenderIdentityCode.TRANSGENDER_MALE)
				return Optional.of(GenderIdentityId.TRANSGENDER_FTM);
			if (genderIdentityCode == GenderIdentityCode.NON_BINARY)
				return Optional.of(GenderIdentityId.NON_BINARY);
			if (genderIdentityCode == GenderIdentityCode.OTHER)
				return Optional.of(GenderIdentityId.OTHER);
		}

		return Optional.empty();
	}

	@Nonnull
	public Optional<RaceId> extractRaceId() {
		if (getExtension() == null)
			return Optional.empty();

		Extension matchingExtension = getExtension().stream()
				.filter(extension -> Objects.equals(RaceCode.EXTENSION_URL, extension.getUrl()))
				.findFirst().orElse(null);

		if (matchingExtension == null || matchingExtension.getExtension() == null)
			return Optional.empty();

		for (ExtensionInner extensionInner : matchingExtension.getExtension()) {
			if (extensionInner.getValueString() != null) {
				RaceCode raceCode = RaceCode.fromFhirValue(extensionInner.getValueString()).orElse(null);

				if (raceCode != null) {
					if (raceCode == RaceCode.WHITE)
						return Optional.of(RaceId.WHITE);
					if (raceCode == RaceCode.AMERICAN_INDIAN_OR_ALASKA_NATIVE)
						return Optional.of(RaceId.AMERICAN_INDIAN_OR_ALASKA_NATIVE);
					if (raceCode == RaceCode.ASIAN)
						return Optional.of(RaceId.ASIAN);
					if (raceCode == RaceCode.BLACK_OR_AFRICAN_AMERICAN)
						return Optional.of(RaceId.BLACK_OR_AFRICAN_AMERICAN);
					if (raceCode == RaceCode.NATIVE_HAWAIIAN_OR_PACIFIC_ISLANDER)
						return Optional.of(RaceId.HAWAIIAN_OR_PACIFIC_ISLANDER);
				}
			}
		}

		return Optional.empty();
	}

	@Nonnull
	public Optional<EthnicityId> extractEthnicityId() {
		if (getExtension() == null)
			return Optional.empty();

		Extension matchingExtension = getExtension().stream()
				.filter(extension -> Objects.equals(EthnicityCode.EXTENSION_URL, extension.getUrl()))
				.findFirst().orElse(null);

		if (matchingExtension == null || matchingExtension.getExtension() == null)
			return Optional.empty();

		for (ExtensionInner extensionInner : matchingExtension.getExtension()) {
			if (extensionInner.getValueString() != null) {
				EthnicityCode ethnicityCode = EthnicityCode.fromFhirValue(extensionInner.getValueString()).orElse(null);

				if (ethnicityCode != null) {
					if (ethnicityCode == EthnicityCode.HISPANIC_OR_LATINO)
						return Optional.of(EthnicityId.HISPANIC_OR_LATINO);
					if (ethnicityCode == EthnicityCode.NOT_HISPANIC_OR_LATINO)
						return Optional.of(EthnicityId.NOT_HISPANIC_OR_LATINO);
				}
			}
		}

		return Optional.empty();
	}

	@Nonnull
	public Optional<BirthSexId> extractBirthSexId() {
		if (getExtension() == null)
			return Optional.empty();

		Extension matchingExtension = getExtension().stream()
				.filter(extension -> Objects.equals(BirthSexCode.EXTENSION_URL, extension.getUrl()))
				.findFirst().orElse(null);

		if (matchingExtension == null || matchingExtension.getValueCode() == null)
			return Optional.empty();

		BirthSexCode birthSexCode = BirthSexCode.fromFhirValue(matchingExtension.getValueCode()).orElse(null);

		if (birthSexCode != null) {
			if (birthSexCode == BirthSexCode.FEMALE)
				return Optional.of(BirthSexId.FEMALE);
			if (birthSexCode == BirthSexCode.MALE)
				return Optional.of(BirthSexId.MALE);
			if (birthSexCode == BirthSexCode.OTHER)
				return Optional.of(BirthSexId.OTHER);
			if (birthSexCode == BirthSexCode.UNKNOWN)
				return Optional.of(BirthSexId.UNKNOWN);
		}

		return Optional.empty();
	}

	@Nonnull
	public Optional<Name> extractFirstMatchingName(@Nullable NameUseCode... nameUseCodes) {
		if (nameUseCodes == null)
			return Optional.empty();

		Map<NameUseCode, List<Name>> namesByUseCode = new HashMap<>();

		if (getName() != null) {
			for (Name name : getName()) {
				NameUseCode nameUseCode = NameUseCode.fromFhirValue(name.getUse()).orElse(null);

				if (nameUseCode != null) {
					List<Name> names = namesByUseCode.get(nameUseCode);

					if (names == null) {
						names = new ArrayList<>();
						namesByUseCode.put(nameUseCode, names);
					}

					names.add(name);
				}
			}
		}

		for (NameUseCode nameUseCode : nameUseCodes) {
			List<Name> names = namesByUseCode.get(nameUseCode);

			if (names != null && names.size() > 0)
				return Optional.of(names.get(0));
		}

		return Optional.empty();
	}

	@Nonnull
	public Optional<Address> extractFirstMatchingAddress(@Nullable AddressUseCode... addressUseCodes) {
		if (addressUseCodes == null)
			return Optional.empty();

		Map<AddressUseCode, List<Address>> addressesByUseCode = new HashMap<>();

		if (getAddress() != null) {
			for (Address address : getAddress()) {
				AddressUseCode addressUseCode = AddressUseCode.fromFhirValue(address.getUse()).orElse(null);

				if (addressUseCode != null) {
					List<Address> addresses = addressesByUseCode.get(addressUseCode);

					if (addresses == null) {
						addresses = new ArrayList<>();
						addressesByUseCode.put(addressUseCode, addresses);
					}

					addresses.add(address);
				}
			}
		}

		for (AddressUseCode addressUseCode : addressUseCodes) {
			List<Address> addresses = addressesByUseCode.get(addressUseCode);

			if (addresses != null && addresses.size() > 0)
				return Optional.of(addresses.get(0));
		}

		return Optional.empty();
	}

	@Nonnull
	public Optional<String> extractFirstMatchingPhoneNumber(@Nullable TelecomUseCode... telecomUseCodes) {
		if (telecomUseCodes == null)
			return Optional.empty();

		Map<TelecomUseCode, List<String>> phoneNumbersByUseCode = new HashMap<>();

		if (getTelecom() != null) {
			for (Telecom telecom : getTelecom()) {
				if (Objects.equals("phone", telecom.getSystem())) {
					TelecomUseCode telecomUseCode = TelecomUseCode.fromFhirValue(telecom.getUse()).orElse(null);
					String phoneNumber = trimToNull(telecom.getValue());

					if (telecomUseCode != null && phoneNumber != null) {
						List<String> phoneNumbers = phoneNumbersByUseCode.get(telecomUseCode);

						if (phoneNumbers == null) {
							phoneNumbers = new ArrayList<>();
							phoneNumbersByUseCode.put(telecomUseCode, phoneNumbers);
						}

						phoneNumbers.add(phoneNumber);
					}
				}
			}
		}

		for (TelecomUseCode telecomUseCode : telecomUseCodes) {
			List<String> phoneNumbers = phoneNumbersByUseCode.get(telecomUseCode);

			if (phoneNumbers != null && phoneNumbers.size() > 0)
				return Optional.of(phoneNumbers.get(0));
		}

		return Optional.empty();
	}

	@Nonnull
	public List<String> extractEmailAddresses() {
		List<String> emailAddresses = new ArrayList<>();

		if (getTelecom() != null) {
			for (Telecom telecom : getTelecom()) {
				if (Objects.equals("email", telecom.getSystem())) {
					String emailAddress = trimToNull(telecom.getValue());

					if (emailAddress != null)
						emailAddresses.add(emailAddress);
				}
			}
		}

		return emailAddresses;
	}

	@Nullable
	public String getRawJson() {
		return this.rawJson;
	}

	public void setRawJson(@Nullable String rawJson) {
		this.rawJson = rawJson;
	}

	@Nullable
	public String getId() {
		return this.id;
	}

	public void setId(@Nullable String id) {
		this.id = id;
	}

	@Nullable
	public String getResourceType() {
		return this.resourceType;
	}

	public void setResourceType(@Nullable String resourceType) {
		this.resourceType = resourceType;
	}

	@Nullable
	public ManagingOrganization getManagingOrganization() {
		return this.managingOrganization;
	}

	public void setManagingOrganization(@Nullable ManagingOrganization managingOrganization) {
		this.managingOrganization = managingOrganization;
	}

	@Nullable
	public List<Communication> getCommunication() {
		return this.communication;
	}

	public void setCommunication(@Nullable List<Communication> communication) {
		this.communication = communication;
	}

	@Nullable
	public MaritalStatus getMaritalStatus() {
		return this.maritalStatus;
	}

	public void setMaritalStatus(@Nullable MaritalStatus maritalStatus) {
		this.maritalStatus = maritalStatus;
	}

	@Nullable
	public String getGender() {
		return this.gender;
	}

	public void setGender(@Nullable String gender) {
		this.gender = gender;
	}

	@Nullable
	public LocalDate getBirthDate() {
		return this.birthDate;
	}

	public void setBirthDate(@Nullable LocalDate birthDate) {
		this.birthDate = birthDate;
	}

	@Nullable
	public Boolean getDeceasedBoolean() {
		return this.deceasedBoolean;
	}

	public void setDeceasedBoolean(@Nullable Boolean deceasedBoolean) {
		this.deceasedBoolean = deceasedBoolean;
	}

	@Nullable
	public List<Address> getAddress() {
		return this.address;
	}

	public void setAddress(@Nullable List<Address> address) {
		this.address = address;
	}

	@Nullable
	public List<Telecom> getTelecom() {
		return this.telecom;
	}

	public void setTelecom(@Nullable List<Telecom> telecom) {
		this.telecom = telecom;
	}

	@Nullable
	public List<Name> getName() {
		return this.name;
	}

	public void setName(@Nullable List<Name> name) {
		this.name = name;
	}

	@Nullable
	public Boolean getActive() {
		return this.active;
	}

	public void setActive(@Nullable Boolean active) {
		this.active = active;
	}

	@Nullable
	public List<Identifier> getIdentifier() {
		return this.identifier;
	}

	public void setIdentifier(@Nullable List<Identifier> identifier) {
		this.identifier = identifier;
	}

	@Nullable
	public List<Extension> getExtension() {
		return this.extension;
	}

	public void setExtension(@Nullable List<Extension> extension) {
		this.extension = extension;
	}

	@Nullable
	public List<GeneralPractitioner> getGeneralPractitioner() {
		return this.generalPractitioner;
	}

	public void setGeneralPractitioner(@Nullable List<GeneralPractitioner> generalPractitioner) {
		this.generalPractitioner = generalPractitioner;
	}

	@Nullable
	public List<Contact> getContact() {
		return this.contact;
	}

	public void setContact(@Nullable List<Contact> contact) {
		this.contact = contact;
	}

	@NotThreadSafe
	public static class Extension {
		@Nullable
		private String url;
		@Nullable
		private String valueCode;
		@Nullable
		private ValueCodeableConcept valueCodeableConcept;
		@Nullable
		private List<ExtensionInner> extension;

		@Nullable
		public String getUrl() {
			return this.url;
		}

		public void setUrl(@Nullable String url) {
			this.url = url;
		}

		@Nullable
		public String getValueCode() {
			return this.valueCode;
		}

		public void setValueCode(@Nullable String valueCode) {
			this.valueCode = valueCode;
		}

		@Nullable
		public List<ExtensionInner> getExtension() {
			return this.extension;
		}

		public void setExtension(@Nullable List<ExtensionInner> extension) {
			this.extension = extension;
		}

		@Nullable
		public ValueCodeableConcept getValueCodeableConcept() {
			return this.valueCodeableConcept;
		}

		public void setValueCodeableConcept(@Nullable ValueCodeableConcept valueCodeableConcept) {
			this.valueCodeableConcept = valueCodeableConcept;
		}

		@NotThreadSafe
		public static class ValueCodeableConcept {
			@Nullable
			private List<Coding> coding;

			@Nullable
			public List<Coding> getCoding() {
				return this.coding;
			}

			public void setCoding(@Nullable List<Coding> coding) {
				this.coding = coding;
			}
		}

		@NotThreadSafe
		public static class ExtensionInner {
			@Nullable
			private String valueString;
			@Nullable
			private String url;
			@Nullable
			private ValueCoding valueCoding;

			@Nullable
			public String getValueString() {
				return this.valueString;
			}

			public void setValueString(@Nullable String valueString) {
				this.valueString = valueString;
			}

			@Nullable
			public String getUrl() {
				return this.url;
			}

			public void setUrl(@Nullable String url) {
				this.url = url;
			}

			@Nullable
			public ValueCoding getValueCoding() {
				return this.valueCoding;
			}

			public void setValueCoding(@Nullable ValueCoding valueCoding) {
				this.valueCoding = valueCoding;
			}

			@NotThreadSafe
			public static class ValueCoding {
				@Nullable
				private String system;
				@Nullable
				private String code;
				@Nullable
				private String display;

				@Nullable
				public String getSystem() {
					return this.system;
				}

				public void setSystem(@Nullable String system) {
					this.system = system;
				}

				@Nullable
				public String getCode() {
					return this.code;
				}

				public void setCode(@Nullable String code) {
					this.code = code;
				}

				@Nullable
				public String getDisplay() {
					return this.display;
				}

				public void setDisplay(@Nullable String display) {
					this.display = display;
				}
			}
		}
	}

	@NotThreadSafe
	public static class Identifier {
		@Nullable
		private String use;
		@Nullable
		private String system;
		@Nullable
		private String value;
		@Nullable
		private Type type;

		@Nullable
		public String getUse() {
			return this.use;
		}

		public void setUse(@Nullable String use) {
			this.use = use;
		}

		@Nullable
		public String getSystem() {
			return this.system;
		}

		public void setSystem(@Nullable String system) {
			this.system = system;
		}

		@Nullable
		public String getValue() {
			return this.value;
		}

		public void setValue(@Nullable String value) {
			this.value = value;
		}

		@Nullable
		public Type getType() {
			return this.type;
		}

		public void setType(@Nullable Type type) {
			this.type = type;
		}

		@NotThreadSafe
		public static class Type {
			@Nullable
			private String text;

			@Nullable
			public String getText() {
				return this.text;
			}

			public void setText(@Nullable String text) {
				this.text = text;
			}
		}
	}

	@NotThreadSafe
	public static class Name {
		@Nullable
		private String use;
		@Nullable
		private String text;
		@Nullable
		private String family;
		@Nullable
		private List<String> given;

		@Nullable
		public String getUse() {
			return this.use;
		}

		public void setUse(@Nullable String use) {
			this.use = use;
		}

		@Nullable
		public String getText() {
			return this.text;
		}

		public void setText(@Nullable String text) {
			this.text = text;
		}

		@Nullable
		public String getFamily() {
			return this.family;
		}

		public void setFamily(@Nullable String family) {
			this.family = family;
		}

		@Nullable
		public List<String> getGiven() {
			return this.given;
		}

		public void setGiven(@Nullable List<String> given) {
			this.given = given;
		}
	}

	@NotThreadSafe
	public static class Telecom {
		@Nullable
		private String use;
		@Nullable
		private String value;
		@Nullable
		private String system;

		@Nullable
		public String getUse() {
			return this.use;
		}

		public void setUse(@Nullable String use) {
			this.use = use;
		}

		@Nullable
		public String getValue() {
			return this.value;
		}

		public void setValue(@Nullable String value) {
			this.value = value;
		}

		@Nullable
		public String getSystem() {
			return this.system;
		}

		public void setSystem(@Nullable String system) {
			this.system = system;
		}
	}

	@NotThreadSafe
	public static class Address {
		@Nullable
		private String use;
		@Nullable
		private List<String> line;
		@Nullable
		private String city;
		@Nullable
		private String district;
		@Nullable
		private String state;
		@Nullable
		private String postalCode;
		@Nullable
		private String country;

		@Nullable
		public String getUse() {
			return this.use;
		}

		public void setUse(@Nullable String use) {
			this.use = use;
		}

		@Nullable
		public List<String> getLine() {
			return this.line;
		}

		public void setLine(@Nullable List<String> line) {
			this.line = line;
		}

		@Nullable
		public String getCity() {
			return this.city;
		}

		public void setCity(@Nullable String city) {
			this.city = city;
		}

		@Nullable
		public String getDistrict() {
			return this.district;
		}

		public void setDistrict(@Nullable String district) {
			this.district = district;
		}

		@Nullable
		public String getState() {
			return this.state;
		}

		public void setState(@Nullable String state) {
			this.state = state;
		}

		@Nullable
		public String getPostalCode() {
			return this.postalCode;
		}

		public void setPostalCode(@Nullable String postalCode) {
			this.postalCode = postalCode;
		}

		@Nullable
		public String getCountry() {
			return this.country;
		}

		public void setCountry(@Nullable String country) {
			this.country = country;
		}
	}

	@NotThreadSafe
	public static class MaritalStatus {
		@Nullable
		private String text;

		@Nullable
		public String getText() {
			return this.text;
		}

		public void setText(@Nullable String text) {
			this.text = text;
		}
	}

	@NotThreadSafe
	public static class Contact {
		@Nullable
		private List<Telecom> telecom;
		@Nullable
		private Name name;
		@Nullable
		private List<Relationship> relationship;

		@Nullable
		public List<Telecom> getTelecom() {
			return this.telecom;
		}

		public void setTelecom(@Nullable List<Telecom> telecom) {
			this.telecom = telecom;
		}

		@Nullable
		public Name getName() {
			return this.name;
		}

		public void setName(@Nullable Name name) {
			this.name = name;
		}

		@Nullable
		public List<Relationship> getRelationship() {
			return this.relationship;
		}

		public void setRelationship(@Nullable List<Relationship> relationship) {
			this.relationship = relationship;
		}

		@NotThreadSafe
		public static class Relationship {
			@Nullable
			private String text;
			@Nullable
			private List<Coding> coding;

			@Nullable
			public String getText() {
				return this.text;
			}

			public void setText(@Nullable String text) {
				this.text = text;
			}

			@Nullable
			public List<Coding> getCoding() {
				return this.coding;
			}

			public void setCoding(@Nullable List<Coding> coding) {
				this.coding = coding;
			}
		}

		@NotThreadSafe
		public static class Name {
			@Nullable
			private String use;
			@Nullable
			private String text;

			@Nullable
			public String getUse() {
				return this.use;
			}

			public void setUse(@Nullable String use) {
				this.use = use;
			}

			@Nullable
			public String getText() {
				return this.text;
			}

			public void setText(@Nullable String text) {
				this.text = text;
			}
		}
	}

	@NotThreadSafe
	public static class Communication {
		@Nullable
		private Language language;
		@Nullable
		private Boolean preferred;

		@Nullable
		public Language getLanguage() {
			return this.language;
		}

		public void setLanguage(@Nullable Language language) {
			this.language = language;
		}

		@Nullable
		public Boolean getPreferred() {
			return this.preferred;
		}

		public void setPreferred(@Nullable Boolean preferred) {
			this.preferred = preferred;
		}

		@NotThreadSafe
		public static class Language {
			@Nullable
			private List<Coding> coding;
			@Nullable
			private String text;

			@Nullable
			public List<Coding> getCoding() {
				return this.coding;
			}

			public void setCoding(@Nullable List<Coding> coding) {
				this.coding = coding;
			}

			@Nullable
			public String getText() {
				return this.text;
			}

			public void setText(@Nullable String text) {
				this.text = text;
			}
		}
	}

	@NotThreadSafe
	public static class Coding {
		@Nullable
		private String system;
		@Nullable
		private String code;
		@Nullable
		private String display;

		@Nullable
		public String getSystem() {
			return this.system;
		}

		public void setSystem(@Nullable String system) {
			this.system = system;
		}

		@Nullable
		public String getCode() {
			return this.code;
		}

		public void setCode(@Nullable String code) {
			this.code = code;
		}

		@Nullable
		public String getDisplay() {
			return this.display;
		}

		public void setDisplay(@Nullable String display) {
			this.display = display;
		}
	}

	@NotThreadSafe
	public static class ManagingOrganization {
		@Nullable
		private String reference;
		@Nullable
		private String display;

		@Nullable
		public String getReference() {
			return this.reference;
		}

		public void setReference(@Nullable String reference) {
			this.reference = reference;
		}

		@Nullable
		public String getDisplay() {
			return this.display;
		}

		public void setDisplay(@Nullable String display) {
			this.display = display;
		}
	}

	@NotThreadSafe
	public static class GeneralPractitioner {
		@Nullable
		private String reference;
		@Nullable
		private String type;
		@Nullable
		private String display;

		@Nullable
		public String getReference() {
			return this.reference;
		}

		public void setReference(@Nullable String reference) {
			this.reference = reference;
		}

		@Nullable
		public String getType() {
			return this.type;
		}

		public void setType(@Nullable String type) {
			this.type = type;
		}

		@Nullable
		public String getDisplay() {
			return this.display;
		}

		public void setDisplay(@Nullable String display) {
			this.display = display;
		}
	}
}
