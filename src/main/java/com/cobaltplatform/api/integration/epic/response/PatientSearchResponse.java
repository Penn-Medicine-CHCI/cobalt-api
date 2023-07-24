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

import com.cobaltplatform.api.integration.epic.code.BirthSexCode;
import com.cobaltplatform.api.integration.epic.code.EthnicityCode;
import com.cobaltplatform.api.integration.epic.code.RaceCode;
import com.cobaltplatform.api.integration.epic.response.PatientSearchResponse.Entry.Resource.Extension;
import com.cobaltplatform.api.model.db.BirthSex.BirthSexId;
import com.cobaltplatform.api.model.db.Ethnicity.EthnicityId;
import com.cobaltplatform.api.model.db.Race.RaceId;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class PatientSearchResponse {
	@Nullable
	private String resourceType;
	@Nullable
	private String type;
	@Nullable
	private Integer total;
	@Nullable
	private List<Link> link;
	@Nullable
	private List<Entry> entry;

	@Nonnull
	public Optional<RaceId> extractRaceId() {
		if (getEntry() == null || getEntry().size() == 0)
			return Optional.empty();

		if (getEntry().size() > 1)
			throw new IllegalStateException("Multiple patient results; not sure which one to extract data from");

		Entry entry = getEntry().get(0);

		Extension matchingExtension = entry.getResource().getExtension().stream()
				.filter(extension -> Objects.equals(RaceCode.DSTU2_EXTENSION_URL, extension.getUrl()))
				.findFirst().orElse(null);

		if (matchingExtension == null || matchingExtension.getValueCodeableConcept() == null)
			return Optional.empty();

		RaceCode raceCode = RaceCode.fromDstu2Value(matchingExtension.getValueCodeableConcept().getCoding().get(0).getCode()).orElse(null);

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

		return Optional.empty();
	}

	@Nonnull
	public Optional<EthnicityId> extractEthnicityId() {
		if (getEntry() == null || getEntry().size() == 0)
			return Optional.empty();

		if (getEntry().size() > 1)
			throw new IllegalStateException("Multiple patient results; not sure which one to extract data from");

		Entry entry = getEntry().get(0);

		Extension matchingExtension = entry.getResource().getExtension().stream()
				.filter(extension -> Objects.equals(EthnicityCode.DSTU2_EXTENSION_URL, extension.getUrl()))
				.findFirst().orElse(null);

		if (matchingExtension == null || matchingExtension.getValueCodeableConcept() == null)
			return Optional.empty();

		EthnicityCode ethnicityCode = EthnicityCode.fromDstu2Value(matchingExtension.getValueCodeableConcept().getCoding().get(0).getCode()).orElse(null);

		if (ethnicityCode != null) {
			if (ethnicityCode == EthnicityCode.HISPANIC_OR_LATINO)
				return Optional.of(EthnicityId.HISPANIC_OR_LATINO);
			if (ethnicityCode == EthnicityCode.NOT_HISPANIC_OR_LATINO)
				return Optional.of(EthnicityId.NOT_HISPANIC_OR_LATINO);
		}

		return Optional.empty();
	}

	@Nonnull
	public Optional<BirthSexId> extractBirthSexId() {
		if (getEntry() == null || getEntry().size() == 0)
			return Optional.empty();

		if (getEntry().size() > 1)
			throw new IllegalStateException("Multiple patient results; not sure which one to extract data from");

		Entry entry = getEntry().get(0);

		Extension matchingExtension = entry.getResource().getExtension().stream()
				.filter(extension -> Objects.equals(BirthSexCode.DSTU2_EXTENSION_URL, extension.getUrl()))
				.findFirst().orElse(null);

		if (matchingExtension == null || matchingExtension.getValueCodeableConcept() == null)
			return Optional.empty();

		BirthSexCode birthSexCode = BirthSexCode.fromDstu2Value(matchingExtension.getValueCodeableConcept().getCoding().get(0).getCode()).orElse(null);

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


	@NotThreadSafe
	public static class Link {
		@Nullable
		private String relation;
		@Nullable
		private String url;

		@Nullable
		public String getRelation() {
			return relation;
		}

		public void setRelation(@Nullable String relation) {
			this.relation = relation;
		}

		@Nullable
		public String getUrl() {
			return url;
		}

		public void setUrl(@Nullable String url) {
			this.url = url;
		}
	}

	@NotThreadSafe
	public static class Entry {
		@Nullable
		private String fullUrl;
		@Nullable
		private Resource resource;
		@Nullable
		private Search search;
		@Nullable
		private List<Link> link;

		@NotThreadSafe
		public static class Search {
			@Nullable
			private String mode;
		}

		@NotThreadSafe
		public static class Resource {
			@Nullable
			private String resourceType;
			@Nullable
			private String id;
			@Nullable
			private Boolean active;
			@Nullable
			private String gender;
			@Nullable
			private String birthDate;
			@Nullable
			private Boolean deceasedBoolean;
			@Nullable
			private List<Extension> extension;
			@Nullable
			private List<Identifier> identifier;
			@Nullable
			private List<Name> name;
			@Nullable
			private List<Telecom> telecom;
			@Nullable
			private List<Address> address;
			@Nullable
			private MaritalStatus maritalStatus;
			@Nullable
			private List<Communication> communication;
			@Nullable
			private List<CareProvider> careProvider;

			@NotThreadSafe
			public static class CareProvider {
				@Nullable
				private String display;
				@Nullable
				private String reference;

				@Nullable
				public String getDisplay() {
					return display;
				}

				public void setDisplay(@Nullable String display) {
					this.display = display;
				}

				@Nullable
				public String getReference() {
					return reference;
				}

				public void setReference(@Nullable String reference) {
					this.reference = reference;
				}
			}

			@NotThreadSafe
			public static class Communication {
				@Nullable
				private Language language;
				@Nullable
				private Boolean preferred;

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
				public Language getLanguage() {
					return language;
				}

				public void setLanguage(@Nullable Language language) {
					this.language = language;
				}

				@Nullable
				public Boolean getPreferred() {
					return preferred;
				}

				public void setPreferred(@Nullable Boolean preferred) {
					this.preferred = preferred;
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
				private List<String> family;
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
				public List<String> getFamily() {
					return family;
				}

				public void setFamily(@Nullable List<String> family) {
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
			public static class Extension {
				@Nullable
				private String url;
				@Nullable
				private String valueString;
				@Nullable
				private ValueCodeableConcept valueCodeableConcept;

				@NotThreadSafe
				public static class ValueCodeableConcept {
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
				public ValueCodeableConcept getValueCodeableConcept() {
					return valueCodeableConcept;
				}

				public void setValueCodeableConcept(@Nullable ValueCodeableConcept valueCodeableConcept) {
					this.valueCodeableConcept = valueCodeableConcept;
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
				private List<Extension> extension;

				@Nullable
				public String getUse() {
					return use;
				}

				public void setUse(@Nullable String use) {
					this.use = use;
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

				@Nullable
				public List<Extension> getExtension() {
					return extension;
				}

				public void setExtension(@Nullable List<Extension> extension) {
					this.extension = extension;
				}
			}

			@Nullable
			public String getResourceType() {
				return resourceType;
			}

			public void setResourceType(@Nullable String resourceType) {
				this.resourceType = resourceType;
			}

			@Nullable
			public String getId() {
				return id;
			}

			public void setId(@Nullable String id) {
				this.id = id;
			}

			@Nullable
			public Boolean getActive() {
				return active;
			}

			public void setActive(@Nullable Boolean active) {
				this.active = active;
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
			public List<CareProvider> getCareProvider() {
				return careProvider;
			}

			public void setCareProvider(@Nullable List<CareProvider> careProvider) {
				this.careProvider = careProvider;
			}
		}

		@Nullable
		public String getFullUrl() {
			return fullUrl;
		}

		public void setFullUrl(@Nullable String fullUrl) {
			this.fullUrl = fullUrl;
		}

		@Nullable
		public Resource getResource() {
			return resource;
		}

		public void setResource(@Nullable Resource resource) {
			this.resource = resource;
		}

		@Nullable
		public Search getSearch() {
			return search;
		}

		public void setSearch(@Nullable Search search) {
			this.search = search;
		}

		@Nullable
		public List<Link> getLink() {
			return link;
		}

		public void setLink(@Nullable List<Link> link) {
			this.link = link;
		}

	}

	@Nullable
	public String getResourceType() {
		return resourceType;
	}

	public void setResourceType(@Nullable String resourceType) {
		this.resourceType = resourceType;
	}

	@Nullable
	public String getType() {
		return type;
	}

	public void setType(@Nullable String type) {
		this.type = type;
	}

	@Nullable
	public Integer getTotal() {
		return total;
	}

	public void setTotal(@Nullable Integer total) {
		this.total = total;
	}

	@Nullable
	public List<Link> getLink() {
		return link;
	}

	public void setLink(@Nullable List<Link> link) {
		this.link = link;
	}

	@Nullable
	public List<Entry> getEntry() {
		return entry;
	}

	public void setEntry(@Nullable List<Entry> entry) {
		this.entry = entry;
	}
}
