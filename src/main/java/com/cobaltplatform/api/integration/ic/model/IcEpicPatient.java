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

package com.cobaltplatform.api.integration.ic.model;

import com.cobaltplatform.api.util.JsonMapper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.List;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class IcEpicPatient {
	@Nonnull
	private static final transient JsonMapper JSON_MAPPER;

	static {
		JSON_MAPPER = new JsonMapper();
	}

	@Nullable
	private String id;
	@Nullable
	private String resourceType;
	@Nullable
	private List<Extension> extension;
	@Nullable
	private List<Identifier> identifier;
	@Nullable
	private Boolean active;
	@Nullable
	private List<Name> name;
	@Nullable
	private List<Telecom> telecom;
	@Nullable
	private String gender;
	@Nullable
	private String birthDate;
	@Nullable
	private Boolean deceasedBoolean;
	@Nullable
	private List<Address> address;
	@Nullable
	private MaritalStatus maritalStatus;
	@Nullable
	private List<Communication> communication;
	@Nullable
	private List<GeneralPractitioner> generalPractitioner;
	@Nullable
	private ManagingOrganization managingOrganization;

	@Override
	public String toString() {
		return JSON_MAPPER.toJson(this);
	}

	@NotThreadSafe
	public static class ManagingOrganization {
		@Nullable
		private String reference;
		@Nullable
		private String display;

		@Nullable
		public String getReference() {
			return reference;
		}

		public void setReference(@Nullable String reference) {
			this.reference = reference;
		}

		@Nullable
		public String getDisplay() {
			return display;
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
		private String display;

		@Nullable
		public String getReference() {
			return reference;
		}

		public void setReference(@Nullable String reference) {
			this.reference = reference;
		}

		@Nullable
		public String getDisplay() {
			return display;
		}

		public void setDisplay(@Nullable String display) {
			this.display = display;
		}
	}

	@NotThreadSafe
	public static class Communication {
		@Nullable
		private Boolean preferred;
		@Nullable
		private Language language;

		@NotThreadSafe
		public static class Language {
			@Nullable
			private String text;
			@Nullable
			private List<Coding> coding;

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
					return system;
				}

				public void setSystem(@Nullable String system) {
					this.system = system;
				}

				@Nullable
				public String getCode() {
					return code;
				}

				public void setCode(@Nullable String code) {
					this.code = code;
				}

				@Nullable
				public String getDisplay() {
					return display;
				}

				public void setDisplay(@Nullable String display) {
					this.display = display;
				}
			}

			@Nullable
			public String getText() {
				return text;
			}

			public void setText(@Nullable String text) {
				this.text = text;
			}

			@Nullable
			public List<Coding> getCoding() {
				return coding;
			}

			public void setCoding(@Nullable List<Coding> coding) {
				this.coding = coding;
			}
		}

		@Nullable
		public Boolean getPreferred() {
			return preferred;
		}

		public void setPreferred(@Nullable Boolean preferred) {
			this.preferred = preferred;
		}

		@Nullable
		public Language getLanguage() {
			return language;
		}

		public void setLanguage(@Nullable Language language) {
			this.language = language;
		}
	}

	@NotThreadSafe
	public static class MaritalStatus {
		@Nullable
		private String text;

		@Nullable
		public String getText() {
			return text;
		}

		public void setText(@Nullable String text) {
			this.text = text;
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
			return use;
		}

		public void setUse(@Nullable String use) {
			this.use = use;
		}

		@Nullable
		public List<String> getLine() {
			return line;
		}

		public void setLine(@Nullable List<String> line) {
			this.line = line;
		}

		@Nullable
		public String getCity() {
			return city;
		}

		public void setCity(@Nullable String city) {
			this.city = city;
		}

		@Nullable
		public String getDistrict() {
			return district;
		}

		public void setDistrict(@Nullable String district) {
			this.district = district;
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

	@NotThreadSafe
	public static class Telecom {
		@Nullable
		private String system;
		@Nullable
		private String value;
		@Nullable
		private String use;

		@Nullable
		public String getSystem() {
			return system;
		}

		public void setSystem(@Nullable String system) {
			this.system = system;
		}

		@Nullable
		public String getValue() {
			return value;
		}

		public void setValue(@Nullable String value) {
			this.value = value;
		}

		@Nullable
		public String getUse() {
			return use;
		}

		public void setUse(@Nullable String use) {
			this.use = use;
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
			return use;
		}

		public void setUse(@Nullable String use) {
			this.use = use;
		}

		@Nullable
		public String getText() {
			return text;
		}

		public void setText(@Nullable String text) {
			this.text = text;
		}

		@Nullable
		public String getFamily() {
			return family;
		}

		public void setFamily(@Nullable String family) {
			this.family = family;
		}

		@Nullable
		public List<String> getGiven() {
			return given;
		}

		public void setGiven(@Nullable List<String> given) {
			this.given = given;
		}
	}

	@NotThreadSafe
	public static class Identifier {
		@Nullable
		private String use;
		@Nullable
		private Type type;
		@Nullable
		private String system;
		@Nullable
		private String value;

		@NotThreadSafe
		public static class Type {
			@Nullable
			private String text;

			@Nullable
			public String getText() {
				return text;
			}

			public void setText(@Nullable String text) {
				this.text = text;
			}
		}

		@Nullable
		public String getUse() {
			return use;
		}

		public void setUse(@Nullable String use) {
			this.use = use;
		}

		@Nullable
		public Type getType() {
			return type;
		}

		public void setType(@Nullable Type type) {
			this.type = type;
		}

		@Nullable
		public String getSystem() {
			return system;
		}

		public void setSystem(@Nullable String system) {
			this.system = system;
		}

		@Nullable
		public String getValue() {
			return value;
		}

		public void setValue(@Nullable String value) {
			this.value = value;
		}
	}

	@NotThreadSafe
	public static class Extension {
		@Nullable
		private String url;
		@Nullable
		private List<InnerExtension> extension;

		@NotThreadSafe
		public static class InnerExtension {
			@Nullable
			private String url;
			@Nullable
			private String valueString;
			@Nullable
			private ValueCoding valueCoding;


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
					return system;
				}

				public void setSystem(@Nullable String system) {
					this.system = system;
				}

				@Nullable
				public String getCode() {
					return code;
				}

				public void setCode(@Nullable String code) {
					this.code = code;
				}

				@Nullable
				public String getDisplay() {
					return display;
				}

				public void setDisplay(@Nullable String display) {
					this.display = display;
				}
			}

			@Nullable
			public String getUrl() {
				return url;
			}

			public void setUrl(@Nullable String url) {
				this.url = url;
			}

			@Nullable
			public String getValueString() {
				return valueString;
			}

			public void setValueString(@Nullable String valueString) {
				this.valueString = valueString;
			}

			@Nullable
			public ValueCoding getValueCoding() {
				return valueCoding;
			}

			public void setValueCoding(@Nullable ValueCoding valueCoding) {
				this.valueCoding = valueCoding;
			}
		}

		@Nullable
		public String getUrl() {
			return url;
		}

		public void setUrl(@Nullable String url) {
			this.url = url;
		}

		@Nullable
		public List<InnerExtension> getExtension() {
			return extension;
		}

		public void setExtension(@Nullable List<InnerExtension> extension) {
			this.extension = extension;
		}
	}

	@Nullable
	public String getId() {
		return id;
	}

	public void setId(@Nullable String id) {
		this.id = id;
	}

	@Nullable
	public String getResourceType() {
		return resourceType;
	}

	public void setResourceType(@Nullable String resourceType) {
		this.resourceType = resourceType;
	}

	@Nullable
	public List<Extension> getExtension() {
		return extension;
	}

	public void setExtension(@Nullable List<Extension> extension) {
		this.extension = extension;
	}

	@Nullable
	public List<Identifier> getIdentifier() {
		return identifier;
	}

	public void setIdentifier(@Nullable List<Identifier> identifier) {
		this.identifier = identifier;
	}

	@Nullable
	public Boolean getActive() {
		return active;
	}

	public void setActive(@Nullable Boolean active) {
		this.active = active;
	}

	@Nullable
	public List<Name> getName() {
		return name;
	}

	public void setName(@Nullable List<Name> name) {
		this.name = name;
	}

	@Nullable
	public List<Telecom> getTelecom() {
		return telecom;
	}

	public void setTelecom(@Nullable List<Telecom> telecom) {
		this.telecom = telecom;
	}

	@Nullable
	public String getGender() {
		return gender;
	}

	public void setGender(@Nullable String gender) {
		this.gender = gender;
	}

	@Nullable
	public String getBirthDate() {
		return birthDate;
	}

	public void setBirthDate(@Nullable String birthDate) {
		this.birthDate = birthDate;
	}

	@Nullable
	public Boolean getDeceasedBoolean() {
		return deceasedBoolean;
	}

	public void setDeceasedBoolean(@Nullable Boolean deceasedBoolean) {
		this.deceasedBoolean = deceasedBoolean;
	}

	@Nullable
	public List<Address> getAddress() {
		return address;
	}

	public void setAddress(@Nullable List<Address> address) {
		this.address = address;
	}

	@Nullable
	public MaritalStatus getMaritalStatus() {
		return maritalStatus;
	}

	public void setMaritalStatus(@Nullable MaritalStatus maritalStatus) {
		this.maritalStatus = maritalStatus;
	}

	@Nullable
	public List<Communication> getCommunication() {
		return communication;
	}

	public void setCommunication(@Nullable List<Communication> communication) {
		this.communication = communication;
	}

	@Nullable
	public List<GeneralPractitioner> getGeneralPractitioner() {
		return generalPractitioner;
	}

	public void setGeneralPractitioner(@Nullable List<GeneralPractitioner> generalPractitioner) {
		this.generalPractitioner = generalPractitioner;
	}

	@Nullable
	public ManagingOrganization getManagingOrganization() {
		return managingOrganization;
	}

	public void setManagingOrganization(@Nullable ManagingOrganization managingOrganization) {
		this.managingOrganization = managingOrganization;
	}
}