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

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.LocalDate;
import java.util.List;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class PatientFhirR4Response {
	@Nullable
	private String id;
	@Nullable
	private String resourceType;
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

	@NotThreadSafe
	private static class Extension {
		@Nullable
		private String url;
		@Nullable
		private List<Extension2> extension;

		@Nullable
		public String getUrl() {
			return this.url;
		}

		public void setUrl(@Nullable String url) {
			this.url = url;
		}

		@Nullable
		public List<Extension2> getExtension() {
			return this.extension;
		}

		public void setExtension(@Nullable List<Extension2> extension) {
			this.extension = extension;
		}

		@NotThreadSafe
		private static class Extension2 {
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
			private static class ValueCoding {
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
	private static class Identifier {
		@Nullable
		private String use;
		@Nullable
		private String system;
		@Nullable
		private String value;
		@Nullable
		private Type type;

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
	private static class Name {
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
	private static class Telecom {
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
	private static class Address {
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
	private static class MaritalStatus {
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
	private static class Communication {
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
		private static class Language {
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

			@NotThreadSafe
			private static class Coding {
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
	private static class ManagingOrganization {
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
}
