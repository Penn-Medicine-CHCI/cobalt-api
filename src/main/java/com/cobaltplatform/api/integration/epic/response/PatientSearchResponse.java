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
import java.util.List;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class PatientSearchResponse {

	//{"resourceType":"Bundle","type":"searchset","total":1,"link":[{"relation":"self","url":"https://ssproxy.pennhealth.com/PRD-FHIR/api/FHIR/DSTU2/Patient?given=Mark&birthdate=1%2F23%2F1982&telecom=215-555-1234&family=AllenTestSix"}],"entry":[{"link":[{"relation":"self","url":"https://ssproxy.pennhealth.com/PRD-FHIR/api/FHIR/DSTU2/Patient/TF4PQvzW4UH9eCOGCRKohgwrX.9evt9h0Rzvp6Ys-OGkB"}],"fullUrl":"https://ssproxy.pennhealth.com/PRD-FHIR/api/FHIR/DSTU2/Patient/TF4PQvzW4UH9eCOGCRKohgwrX.9evt9h0Rzvp6Ys-OGkB","resource":{"resourceType":"Patient","id":"TF4PQvzW4UH9eCOGCRKohgwrX.9evt9h0Rzvp6Ys-OGkB","extension":[{"url":"http://hl7.org/fhir/StructureDefinition/us-core-race","valueCodeableConcept":{"coding":[{"system":"urn:oid:2.16.840.1.113883.5.104","code":"UNK","display":"Unknown"}],"text":"Unknown"}},{"url":"http://hl7.org/fhir/StructureDefinition/us-core-ethnicity","valueCodeableConcept":{"coding":[{"system":"urn:oid:2.16.840.1.113883.5.50","code":"UNK","display":"Unknown"}],"text":"Unknown"}},{"url":"http://hl7.org/fhir/StructureDefinition/us-core-birth-sex","valueCodeableConcept":{"coding":[{"system":"http://hl7.org/fhir/v3/AdministrativeGender","code":"M","display":"Male"}],"text":"Male"}}],"identifier":[{"use":"usual","system":"urn:oid:1.2.840.114350.1.13.87.3.7.5.737384.0","value":"18557741"},{"use":"usual","system":"urn:oid:1.2.840.114350.1.13.87.3.7.5.737384.100","value":"467916953"},{"use":"usual","system":"urn:oid:1.2.840.114350.1.13.87.3.7.5.737384.101","value":"467916953"},{"use":"usual","system":"urn:oid:1.2.840.114350.1.13.87.3.7.5.737384.102","value":"467916953"},{"use":"usual","system":"urn:oid:1.2.840.114350.1.13.87.3.7.5.737384.103","value":"467916953"},{"use":"usual","system":"urn:oid:1.2.840.114350.1.13.87.3.7.5.737384.104","value":"467916953"},{"use":"usual","system":"urn:oid:1.3.6.1.4.1.22812.19.44324.0","value":"8467916953"},{"use":"usual","system":"urn:oid:1.2.840.114350.1.13.87.3.7.5.737384.110","value":"467916953"},{"use":"usual","system":"urn:oid:2.16.840.1.113883.3.2023.1.2.4.2.0","value":"467916953"},{"extension":[{"url":"http://hl7.org/fhir/StructureDefinition/rendered-value","valueString":"xxx-xx-1116"}],"use":"usual","system":"urn:oid:2.16.840.1.113883.4.1"}],"active":true,"name":[{"use":"usual","text":"Mark AllenTestSix","family":["AllenTestSix"],"given":["Mark"]}],"telecom":[{"system":"phone","value":"215-555-1234","use":"mobile"},{"system":"email","value":"maa+6@xmog.com.com"}],"gender":"male","birthDate":"1982-01-23","deceasedBoolean":false},"search":{"model":"match"}},{"resource":{"resourceType":"OperationOutcome","id":"532794675","issue":[{"severity":"information","code":"informational","details":{"coding":[{"system":"urn:oid:1.2.840.114350.1.13.87.3.7.2.657369","code":"4100","display":"The resource request contained an invalid parameter."}],"text":"The resource request contained an invalid parameter."},"location":["http.birthdate"]}]},"search":{"model":"outcome"}}]}
	//{
	//  "resourceType": "Bundle",
	//  "type": "searchset",
	//  "total": 1.0,
	//  "link": [
	//    {
	//      "relation": "self",
	//      "url": "https://ssproxy.pennhealth.com/PRD-FHIR/api/FHIR/DSTU2/Patient?given\u003dMark\u0026birthdate\u003d1%2F23%2F1982\u0026telecom\u003d215-555-1234\u0026family\u003dAllenTestSix"
	//    }
	//  ],
	//  "entry": [
	//    {
	//      "link": [
	//        {
	//          "relation": "self",
	//          "url": "https://ssproxy.pennhealth.com/PRD-FHIR/api/FHIR/DSTU2/Patient/TF4PQvzW4UH9eCOGCRKohgwrX.9evt9h0Rzvp6Ys-OGkB"
	//        }
	//      ],
	//      "fullUrl": "https://ssproxy.pennhealth.com/PRD-FHIR/api/FHIR/DSTU2/Patient/TF4PQvzW4UH9eCOGCRKohgwrX.9evt9h0Rzvp6Ys-OGkB",
	//      "resource": {
	//        "resourceType": "Patient",
	//        "id": "TF4PQvzW4UH9eCOGCRKohgwrX.9evt9h0Rzvp6Ys-OGkB",
	//        "extension": [
	//          {
	//            "url": "http://hl7.org/fhir/StructureDefinition/us-core-race",
	//            "valueCodeableConcept": {
	//              "coding": [
	//                {
	//                  "system": "urn:oid:2.16.840.1.113883.5.104",
	//                  "code": "UNK",
	//                  "display": "Unknown"
	//                }
	//              ],
	//              "text": "Unknown"
	//            }
	//          },
	//          {
	//            "url": "http://hl7.org/fhir/StructureDefinition/us-core-ethnicity",
	//            "valueCodeableConcept": {
	//              "coding": [
	//                {
	//                  "system": "urn:oid:2.16.840.1.113883.5.50",
	//                  "code": "UNK",
	//                  "display": "Unknown"
	//                }
	//              ],
	//              "text": "Unknown"
	//            }
	//          },
	//          {
	//            "url": "http://hl7.org/fhir/StructureDefinition/us-core-birth-sex",
	//            "valueCodeableConcept": {
	//              "coding": [
	//                {
	//                  "system": "http://hl7.org/fhir/v3/AdministrativeGender",
	//                  "code": "M",
	//                  "display": "Male"
	//                }
	//              ],
	//              "text": "Male"
	//            }
	//          }
	//        ],
	//        "identifier": [
	//          {
	//            "use": "usual",
	//            "system": "urn:oid:1.2.840.114350.1.13.87.3.7.5.737384.0",
	//            "value": "18557741"
	//          },
	//          {
	//            "use": "usual",
	//            "system": "urn:oid:1.2.840.114350.1.13.87.3.7.5.737384.100",
	//            "value": "467916953"
	//          },
	//          {
	//            "use": "usual",
	//            "system": "urn:oid:1.2.840.114350.1.13.87.3.7.5.737384.101",
	//            "value": "467916953"
	//          },
	//          {
	//            "use": "usual",
	//            "system": "urn:oid:1.2.840.114350.1.13.87.3.7.5.737384.102",
	//            "value": "467916953"
	//          },
	//          {
	//            "use": "usual",
	//            "system": "urn:oid:1.2.840.114350.1.13.87.3.7.5.737384.103",
	//            "value": "467916953"
	//          },
	//          {
	//            "use": "usual",
	//            "system": "urn:oid:1.2.840.114350.1.13.87.3.7.5.737384.104",
	//            "value": "467916953"
	//          },
	//          {
	//            "use": "usual",
	//            "system": "urn:oid:1.3.6.1.4.1.22812.19.44324.0",
	//            "value": "8467916953"
	//          },
	//          {
	//            "use": "usual",
	//            "system": "urn:oid:1.2.840.114350.1.13.87.3.7.5.737384.110",
	//            "value": "467916953"
	//          },
	//          {
	//            "use": "usual",
	//            "system": "urn:oid:2.16.840.1.113883.3.2023.1.2.4.2.0",
	//            "value": "467916953"
	//          },
	//          {
	//            "extension": [
	//              {
	//                "url": "http://hl7.org/fhir/StructureDefinition/rendered-value",
	//                "valueString": "xxx-xx-1116"
	//              }
	//            ],
	//            "use": "usual",
	//            "system": "urn:oid:2.16.840.1.113883.4.1"
	//          }
	//        ],
	//        "active": true,
	//        "name": [
	//          {
	//            "use": "usual",
	//            "text": "Mark AllenTestSix",
	//            "family": [
	//              "AllenTestSix"
	//            ],
	//            "given": [
	//              "Mark"
	//            ]
	//          }
	//        ],
	//        "telecom": [
	//          {
	//            "system": "phone",
	//            "value": "215-555-1234",
	//            "use": "mobile"
	//          },
	//          {
	//            "system": "email",
	//            "value": "maa+6@xmog.com.com"
	//          }
	//        ],
	//        "gender": "male",
	//        "birthDate": "1982-01-23",
	//        "deceasedBoolean": false
	//      },
	//      "search": {
	//        "model": "match"
	//      }
	//    },
	//    {
	//      "resource": {
	//        "resourceType": "OperationOutcome",
	//        "id": "532794675",
	//        "issue": [
	//          {
	//            "severity": "information",
	//            "code": "informational",
	//            "details": {
	//              "coding": [
	//                {
	//                  "system": "urn:oid:1.2.840.114350.1.13.87.3.7.2.657369",
	//                  "code": "4100",
	//                  "display": "The resource request contained an invalid parameter."
	//                }
	//              ],
	//              "text": "The resource request contained an invalid parameter."
	//            },
	//            "location": [
	//              "http.birthdate"
	//            ]
	//          }
	//        ]
	//      },
	//      "search": {
	//        "model": "outcome"
	//      }
	//    }
	//  ]
	// }

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
