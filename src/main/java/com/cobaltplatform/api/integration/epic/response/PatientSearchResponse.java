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

import com.cobaltplatform.api.integration.epic.code.AdministrativeGenderCode;
import com.cobaltplatform.api.integration.epic.code.BirthSexCode;
import com.cobaltplatform.api.integration.epic.code.ClinicalSexCode;
import com.cobaltplatform.api.integration.epic.code.EthnicityCode;
import com.cobaltplatform.api.integration.epic.code.GenderIdentityCode;
import com.cobaltplatform.api.integration.epic.code.LegalSexCode;
import com.cobaltplatform.api.integration.epic.code.PreferredPronounCode;
import com.cobaltplatform.api.integration.epic.code.RaceCode;
import com.cobaltplatform.api.integration.epic.response.PatientSearchResponse.Entry.Resource.Extension;
import com.cobaltplatform.api.integration.epic.response.PatientSearchResponse.Entry.Resource.Extension.EmbeddedExtension;
import com.cobaltplatform.api.model.db.AdministrativeGender.AdministrativeGenderId;
import com.cobaltplatform.api.model.db.BirthSex.BirthSexId;
import com.cobaltplatform.api.model.db.ClinicalSex.ClinicalSexId;
import com.cobaltplatform.api.model.db.Ethnicity.EthnicityId;
import com.cobaltplatform.api.model.db.GenderIdentity.GenderIdentityId;
import com.cobaltplatform.api.model.db.LegalSex.LegalSexId;
import com.cobaltplatform.api.model.db.PreferredPronoun.PreferredPronounId;
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
	private String rawJson;

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

		// Found DSTU2, let's try to use it.
		if (matchingExtension != null && matchingExtension.getValueCodeableConcept() != null) {
			RaceCode raceCode = RaceCode.fromDstu2Value(matchingExtension.getValueCodeableConcept().getCoding().get(0).getCode()).orElse(null);

			if (raceCode != null) {
				Optional<RaceId> raceId = toRaceId(raceCode);

				if (raceId.isPresent())
					return raceId;
			}
		} else {
			// Didn't find DSTU2, let's try FHIR.
			matchingExtension = entry.getResource().getExtension().stream()
					.filter(extension -> Objects.equals(RaceCode.EXTENSION_URL, extension.getUrl()))
					.findFirst().orElse(null);

			if (matchingExtension != null && matchingExtension.getExtension() != null && matchingExtension.getExtension().size() > 0) {
				EmbeddedExtension matchingEmbeddedExtension = matchingExtension.getExtension().stream()
						.filter(extension -> Objects.equals("ombCategory", extension.getUrl()))
						.findFirst().orElse(null);

				if (matchingEmbeddedExtension != null && matchingEmbeddedExtension.getValueCoding() != null) {
					String code = matchingEmbeddedExtension.getValueCoding().getCode();

					if (code != null) {
						RaceCode raceCode = RaceCode.fromDstu2Value(code).orElse(null);

						if (raceCode != null) {
							Optional<RaceId> raceId = toRaceId(raceCode);

							if (raceId.isPresent())
								return raceId;
						}
					}
				}
			}
		}

		return Optional.empty();
	}

	@Nonnull
	protected Optional<RaceId> toRaceId(@Nullable RaceCode raceCode) {
		if (raceCode == null)
			return Optional.empty();

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
		if (raceCode == RaceCode.OTHER_RACE)
			return Optional.of(RaceId.OTHER);
		if (raceCode == RaceCode.SOME_OTHER_RACE)
			return Optional.of(RaceId.OTHER);
		if (raceCode == RaceCode.UNKNOWN)
			return Optional.of(RaceId.UNKNOWN);

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

		// Found DSTU2, let's try to use it.
		if (matchingExtension != null && matchingExtension.getValueCodeableConcept() != null) {
			EthnicityCode ethnicityCode = EthnicityCode.fromDstu2Value(matchingExtension.getValueCodeableConcept().getCoding().get(0).getCode()).orElse(null);

			if (ethnicityCode != null) {
				Optional<EthnicityId> ethnicityId = toEthnicityId(ethnicityCode);

				if (ethnicityId.isPresent())
					return ethnicityId;
			}
		} else {
			// Didn't find DSTU2, let's try FHIR.
			matchingExtension = entry.getResource().getExtension().stream()
					.filter(extension -> Objects.equals(EthnicityCode.EXTENSION_URL, extension.getUrl()))
					.findFirst().orElse(null);

			if (matchingExtension != null && matchingExtension.getExtension() != null && matchingExtension.getExtension().size() > 0) {
				EmbeddedExtension matchingEmbeddedExtension = matchingExtension.getExtension().stream()
						.filter(extension -> Objects.equals("ombCategory", extension.getUrl()))
						.findFirst().orElse(null);

				if (matchingEmbeddedExtension != null && matchingEmbeddedExtension.getValueCoding() != null) {
					String code = matchingEmbeddedExtension.getValueCoding().getCode();

					if (code != null) {
						EthnicityCode ethnicityCode = EthnicityCode.fromDstu2Value(code).orElse(null);

						if (ethnicityCode != null) {
							Optional<EthnicityId> ethnicityId = toEthnicityId(ethnicityCode);

							if (ethnicityId.isPresent())
								return ethnicityId;
						}
					}
				}
			}
		}

		return Optional.empty();
	}

	@Nonnull
	protected Optional<EthnicityId> toEthnicityId(@Nullable EthnicityCode ethnicityCode) {
		if (ethnicityCode == null)
			return Optional.empty();

		if (ethnicityCode == EthnicityCode.HISPANIC_OR_LATINO)
			return Optional.of(EthnicityId.HISPANIC_OR_LATINO);
		if (ethnicityCode == EthnicityCode.NOT_HISPANIC_OR_LATINO)
			return Optional.of(EthnicityId.NOT_HISPANIC_OR_LATINO);

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

		// Found DSTU2, let's try to use it.
		if (matchingExtension != null) {
			BirthSexCode birthSexCode = null;

			if (matchingExtension.getValueCodeableConcept() != null)
				birthSexCode = BirthSexCode.fromDstu2Value(matchingExtension.getValueCodeableConcept().getCoding().get(0).getCode()).orElse(null);

			if (birthSexCode == null && matchingExtension.getValueCode() != null)
				birthSexCode = BirthSexCode.fromDstu2Value(matchingExtension.getValueCode()).orElse(null);

			if (birthSexCode != null) {
				Optional<BirthSexId> birthSexId = toBirthSexId(birthSexCode);

				if (birthSexId.isPresent())
					return birthSexId;
			}
		} else {
			// Didn't find DSTU2, let's try FHIR.
			matchingExtension = entry.getResource().getExtension().stream()
					.filter(extension -> Objects.equals(BirthSexCode.EXTENSION_URL, extension.getUrl()))
					.findFirst().orElse(null);

			if (matchingExtension != null) {
				if (matchingExtension.getValueCode() != null) {
					BirthSexCode birthSexCode = BirthSexCode.fromDstu2Value(matchingExtension.getValueCode()).orElse(null);

					if (birthSexCode != null) {
						Optional<BirthSexId> birthSexId = toBirthSexId(birthSexCode);

						if (birthSexId.isPresent())
							return birthSexId;
					}
				}

				if (matchingExtension.getExtension() != null && matchingExtension.getExtension().size() > 0) {
					EmbeddedExtension matchingEmbeddedExtension = matchingExtension.getExtension().stream()
							.filter(extension -> Objects.equals("ombCategory", extension.getUrl()))
							.findFirst().orElse(null);

					if (matchingEmbeddedExtension != null && matchingEmbeddedExtension.getValueCoding() != null) {
						String code = matchingEmbeddedExtension.getValueCoding().getCode();

						if (code != null) {
							BirthSexCode birthSexCode = BirthSexCode.fromDstu2Value(code).orElse(null);

							if (birthSexCode != null) {
								Optional<BirthSexId> birthSexId = toBirthSexId(birthSexCode);

								if (birthSexId.isPresent())
									return birthSexId;
							}
						}
					}
				}
			}
		}

		return Optional.empty();
	}

	@Nonnull
	protected Optional<BirthSexId> toBirthSexId(@Nullable BirthSexCode birthSexCode) {
		if (birthSexCode == null)
			return Optional.empty();

		if (birthSexCode == BirthSexCode.FEMALE)
			return Optional.of(BirthSexId.FEMALE);
		if (birthSexCode == BirthSexCode.MALE)
			return Optional.of(BirthSexId.MALE);
		if (birthSexCode == BirthSexCode.OTHER)
			return Optional.of(BirthSexId.OTHER);
		if (birthSexCode == BirthSexCode.UNKNOWN)
			return Optional.of(BirthSexId.UNKNOWN);

		return Optional.empty();
	}

	@Nonnull
	public Optional<GenderIdentityId> extractGenderIdentityId() {
		if (getEntry() == null || getEntry().size() == 0)
			return Optional.empty();

		if (getEntry().size() > 1)
			throw new IllegalStateException("Multiple patient results; not sure which one to extract data from");

		Entry entry = getEntry().get(0);

		Extension matchingExtension = entry.getResource().getExtension().stream()
				.filter(extension -> Objects.equals(GenderIdentityCode.EXTENSION_URL, extension.getUrl()))
				.findFirst().orElse(null);

		// Found FHIR extension, let's try to use it.
		if (matchingExtension != null) {
			GenderIdentityCode genderIdentityCode = null;

			if (matchingExtension.getValueCodeableConcept() != null)
				genderIdentityCode = GenderIdentityCode.fromFhirValue(matchingExtension.getValueCodeableConcept().getCoding().get(0).getCode()).orElse(null);

			if (genderIdentityCode != null) {
				Optional<GenderIdentityId> genderIdentityId = toGenderIdentityId(genderIdentityCode);

				if (genderIdentityId.isPresent())
					return genderIdentityId;
			}
		}

		return Optional.empty();
	}

	@Nonnull
	protected Optional<GenderIdentityId> toGenderIdentityId(@Nullable GenderIdentityCode genderIdentityCode) {
		if (genderIdentityCode == null)
			return Optional.empty();

		if (genderIdentityCode == GenderIdentityCode.TRANSGENDER_FEMALE)
			return Optional.of(GenderIdentityId.TRANSGENDER_MTF);
		if (genderIdentityCode == GenderIdentityCode.TRANSGENDER_MALE)
			return Optional.of(GenderIdentityId.TRANSGENDER_FTM);
		if (genderIdentityCode == GenderIdentityCode.NON_BINARY)
			return Optional.of(GenderIdentityId.NON_BINARY);
		if (genderIdentityCode == GenderIdentityCode.MALE)
			return Optional.of(GenderIdentityId.MALE);
		if (genderIdentityCode == GenderIdentityCode.FEMALE)
			return Optional.of(GenderIdentityId.FEMALE);
		if (genderIdentityCode == GenderIdentityCode.OTHER)
			return Optional.of(GenderIdentityId.OTHER);
		if (genderIdentityCode == GenderIdentityCode.NON_DISCLOSE)
			return Optional.of(GenderIdentityId.NOT_DISCLOSED);

		return Optional.empty();
	}

	@Nonnull
	public Optional<PreferredPronounId> extractPreferredPronounId() {
		if (getEntry() == null || getEntry().size() == 0)
			return Optional.empty();

		if (getEntry().size() > 1)
			throw new IllegalStateException("Multiple patient results; not sure which one to extract data from");

		Entry entry = getEntry().get(0);

		Extension matchingExtension = entry.getResource().getExtension().stream()
				.filter(extension -> Objects.equals(PreferredPronounCode.EXTENSION_URL, extension.getUrl()))
				.findFirst().orElse(null);

		// Found FHIR extension, let's try to use it.
		if (matchingExtension != null) {
			PreferredPronounCode preferredPronounCode = null;

			if (matchingExtension.getValueCodeableConcept() != null)
				preferredPronounCode = PreferredPronounCode.fromFhirValue(matchingExtension.getValueCodeableConcept().getCoding().get(0).getCode()).orElse(null);

			if (preferredPronounCode != null) {
				Optional<PreferredPronounId> preferredPronounId = toPreferredPronounId(preferredPronounCode);

				if (preferredPronounId.isPresent())
					return preferredPronounId;
			}
		}

		return Optional.empty();
	}

	@Nonnull
	protected Optional<PreferredPronounId> toPreferredPronounId(@Nullable PreferredPronounCode preferredPronounCode) {
		if (preferredPronounCode == null)
			return Optional.empty();

		if (preferredPronounCode == PreferredPronounCode.HE_HIM_HIS_HIS_HIMSELF)
			return Optional.of(PreferredPronounId.HE_HIM_HIS_HIS_HIMSELF);
		if (preferredPronounCode == PreferredPronounCode.SHE_HER_HER_HERS_HERSELF)
			return Optional.of(PreferredPronounId.SHE_HER_HER_HERS_HERSELF);
		if (preferredPronounCode == PreferredPronounCode.THEY_THEM_THEIR_THEIRS_THEMSELVES)
			return Optional.of(PreferredPronounId.THEY_THEM_THEIR_THEIRS_THEMSELVES);
		if (preferredPronounCode == PreferredPronounCode.ZE_ZIR_ZIR_ZIRS_ZIRSELF)
			return Optional.of(PreferredPronounId.ZE_ZIR_ZIR_ZIRS_ZIRSELF);
		if (preferredPronounCode == PreferredPronounCode.XIE_HIR_HERE_HIR_HIRS_HIRSELF)
			return Optional.of(PreferredPronounId.XIE_HIR_HERE_HIR_HIRS_HIRSELF);
		if (preferredPronounCode == PreferredPronounCode.CO_CO_COS_COS_COSELF)
			return Optional.of(PreferredPronounId.CO_CO_COS_COS_COSELF);
		if (preferredPronounCode == PreferredPronounCode.EN_EN_ENS_ENS_ENSELF)
			return Optional.of(PreferredPronounId.EN_EN_ENS_ENS_ENSELF);
		if (preferredPronounCode == PreferredPronounCode.EY_EM_EIR_EIRS_EMSELF)
			return Optional.of(PreferredPronounId.EY_EM_EIR_EIRS_EMSELF);
		if (preferredPronounCode == PreferredPronounCode.YO_YO_YOS_YOS_YOSELF)
			return Optional.of(PreferredPronounId.YO_YO_YOS_YOS_YOSELF);
		if (preferredPronounCode == PreferredPronounCode.VE_VIS_VER_VER_VERSELF)
			return Optional.of(PreferredPronounId.VE_VIS_VER_VER_VERSELF);
		if (preferredPronounCode == PreferredPronounCode.DO_NOT_USE_PRONOUNS)
			return Optional.of(PreferredPronounId.DO_NOT_USE_PRONOUNS);

		return Optional.empty();
	}

	@Nonnull
	public Optional<ClinicalSexId> extractClinicalSexId() {
		if (getEntry() == null || getEntry().size() == 0)
			return Optional.empty();

		if (getEntry().size() > 1)
			throw new IllegalStateException("Multiple patient results; not sure which one to extract data from");

		Entry entry = getEntry().get(0);

		Extension matchingExtension = entry.getResource().getExtension().stream()
				.filter(extension -> Objects.equals(ClinicalSexCode.EXTENSION_URL, extension.getUrl()))
				.findFirst().orElse(null);

		// Found FHIR extension, let's try to use it.
		if (matchingExtension != null) {
			ClinicalSexCode clinicalSexCode = null;

			if (matchingExtension.getValueCodeableConcept() != null)
				clinicalSexCode = ClinicalSexCode.fromFhirValue(matchingExtension.getValueCodeableConcept().getCoding().get(0).getCode()).orElse(null);

			if (clinicalSexCode != null) {
				Optional<ClinicalSexId> clinicalSexId = toClinicalSexId(clinicalSexCode);

				if (clinicalSexId.isPresent())
					return clinicalSexId;
			}
		}

		return Optional.empty();
	}

	@Nonnull
	protected Optional<ClinicalSexId> toClinicalSexId(@Nullable ClinicalSexCode clinicalSexCode) {
		if (clinicalSexCode == null)
			return Optional.empty();

		if (clinicalSexCode == ClinicalSexCode.MALE)
			return Optional.of(ClinicalSexId.MALE);
		if (clinicalSexCode == ClinicalSexCode.FEMALE)
			return Optional.of(ClinicalSexId.FEMALE);
		if (clinicalSexCode == ClinicalSexCode.SPECIFIED)
			return Optional.of(ClinicalSexId.SPECIFIED);
		if (clinicalSexCode == ClinicalSexCode.UNKNOWN)
			return Optional.of(ClinicalSexId.UNKNOWN);

		return Optional.empty();
	}

	@Nonnull
	public Optional<LegalSexId> extractLegalSexId() {
		if (getEntry() == null || getEntry().size() == 0)
			return Optional.empty();

		if (getEntry().size() > 1)
			throw new IllegalStateException("Multiple patient results; not sure which one to extract data from");

		Entry entry = getEntry().get(0);

		Extension matchingExtension = entry.getResource().getExtension().stream()
				.filter(extension -> Objects.equals(LegalSexCode.EXTENSION_URL, extension.getUrl()))
				.findFirst().orElse(null);

		// Found FHIR extension, let's try to use it.
		if (matchingExtension != null) {
			LegalSexCode legalSexCode = null;

			if (matchingExtension.getValueCodeableConcept() != null)
				legalSexCode = LegalSexCode.fromFhirValue(matchingExtension.getValueCodeableConcept().getCoding().get(0).getCode()).orElse(null);

			if (legalSexCode != null) {
				Optional<LegalSexId> legalSexId = toLegalSexId(legalSexCode);

				if (legalSexId.isPresent())
					return legalSexId;
			}
		}

		return Optional.empty();
	}

	@Nonnull
	protected Optional<LegalSexId> toLegalSexId(@Nullable LegalSexCode legalSexCode) {
		if (legalSexCode == null)
			return Optional.empty();

		if (legalSexCode == LegalSexCode.MALE)
			return Optional.of(LegalSexId.MALE);
		if (legalSexCode == LegalSexCode.FEMALE)
			return Optional.of(LegalSexId.FEMALE);
		if (legalSexCode == LegalSexCode.UNDIFFERENTIATED)
			return Optional.of(LegalSexId.UNDIFFERENTIATED);

		return Optional.empty();
	}

	@Nonnull
	public Optional<AdministrativeGenderId> extractAdministrativeGenderId() {
		if (getEntry() == null || getEntry().size() == 0)
			return Optional.empty();

		if (getEntry().size() > 1)
			throw new IllegalStateException("Multiple patient results; not sure which one to extract data from");

		Entry entry = getEntry().get(0);
		String gender = entry.getResource().getGender();
		AdministrativeGenderCode administrativeGenderCode = AdministrativeGenderCode.fromFhirValue(gender).orElse(null);

		if (administrativeGenderCode == null)
			return Optional.empty();

		if (administrativeGenderCode == AdministrativeGenderCode.MALE)
			return Optional.of(AdministrativeGenderId.MALE);
		if (administrativeGenderCode == AdministrativeGenderCode.FEMALE)
			return Optional.of(AdministrativeGenderId.FEMALE);
		if (administrativeGenderCode == AdministrativeGenderCode.OTHER)
			return Optional.of(AdministrativeGenderId.OTHER);
		if (administrativeGenderCode == AdministrativeGenderCode.UNKNOWN)
			return Optional.of(AdministrativeGenderId.UNKNOWN);

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
					return this.family;
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
			public static class Extension {
				@Nullable
				private String url;
				@Nullable
				private String valueCode;
				@Nullable
				private String valueString;
				@Nullable
				private ValueCodeableConcept valueCodeableConcept;
				@Nullable
				private List<EmbeddedExtension> extension;

				@NotThreadSafe
				public static class EmbeddedExtension {
					// Some extensions can have an array of "mini extensions" like this:
					// {
					//    "extension": [
					//        {
					//            "valueCoding": {
					//                "system": "urn:oid:2.16.840.1.113883.6.238",
					//                "code": "2186-5",
					//                "display": "Not Hispanic or Latino"
					//            },
					//            "url": "ombCategory"
					//        },
					//        {
					//            "valueString": "Not Hispanic or Latino",
					//            "url": "text"
					//        }
					//    ],
					//    "url": "http://hl7.org/fhir/us/core/StructureDefinition/us-core-ethnicity"
					// }

					@Nullable
					private String valueString;
					@Nullable
					private String url;
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
				}

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
				public String getValueCode() {
					return this.valueCode;
				}

				public void setValueCode(@Nullable String valueCode) {
					this.valueCode = valueCode;
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

				@Nullable
				public List<EmbeddedExtension> getExtension() {
					return this.extension;
				}

				public void setExtension(@Nullable List<EmbeddedExtension> extension) {
					this.extension = extension;
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

	@Nullable
	public String getRawJson() {
		return this.rawJson;
	}

	public void setRawJson(@Nullable String rawJson) {
		this.rawJson = rawJson;
	}
}
