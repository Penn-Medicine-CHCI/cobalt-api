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

import com.cobaltplatform.api.integration.epic.EpicClient;
import com.cobaltplatform.api.integration.epic.request.GetPatientDemographicsRequest;
import com.cobaltplatform.api.integration.epic.request.PatientSearchRequest;
import com.cobaltplatform.api.integration.epic.response.GetPatientDemographicsResponse;
import com.cobaltplatform.api.integration.epic.response.PatientSearchResponse;
import com.cobaltplatform.api.model.qualifier.AuditLogged;
import info.debatty.java.stringsimilarity.Levenshtein;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class EpicPatientMatcher {
	@Nonnull
	private static final Integer LOW_THRESHOLD;
	@Nonnull
	private static final Integer HIGH_THRESHOLD;

	@Nonnull
	private final EpicClient epicClient;
	@Nonnull
	private final Levenshtein levenshtein;

	static {
		LOW_THRESHOLD = 22;
		HIGH_THRESHOLD = 40;
	}

	@Inject
	public EpicPatientMatcher(@AuditLogged @Nonnull EpicClient epicClient) {
		requireNonNull(epicClient);
		this.epicClient = epicClient;
		this.levenshtein = new Levenshtein();
	}

	@Nonnull
	public List<EpicPatientMatch> match(@Nonnull EpicPatientMatchRequest request) {
		requireNonNull(request);

		List<PatientSearchResponse.Entry> patientEntries = new ArrayList<>(200); // 200 = 2 pages of results
		Set<String> patientUids = new HashSet<>(200);

		// Weird flow: first search by telephone only, if available.
		// This is to sidestep the case where we search for everything and our desired result is row > 100,
		// which is next page of results, which we have no way to fetch
		if (request.getPhoneNumber() != null && request.getPhoneNumber().trim().length() > 0) {
			PatientSearchRequest searchRequest = new PatientSearchRequest();
			searchRequest.setTelecom(request.getPhoneNumber());

			PatientSearchResponse searchResponse = getEpicClient().performPatientSearch(searchRequest);

			for (PatientSearchResponse.Entry entry : searchResponse.getEntry()) {
				String patientUid = getEpicClient().extractUIDFromPatientEntry(entry).orElse(null);

				if (patientUid != null) {
					patientUids.add(patientUid);
					patientEntries.add(entry);
				}
			}
		}

		PatientSearchRequest searchRequest = new PatientSearchRequest();
		searchRequest.setBirthdate(request.getDateOfBirth());
		searchRequest.setFamily(request.getLastName());
		searchRequest.setTelecom(request.getPhoneNumber());
		searchRequest.setGiven(request.getFirstName());

		if (request.getGender() != null)
			searchRequest.setGender(PatientSearchRequest.Gender.valueOf(request.getGender().name()));

		PatientSearchResponse searchResponse = getEpicClient().performPatientSearch(searchRequest);

		// If we found more distinct records, add them to the set to match on
		for (PatientSearchResponse.Entry entry : searchResponse.getEntry()) {
			String patientUid = getEpicClient().extractUIDFromPatientEntry(entry).orElse(null);

			if (patientUid != null && !patientUids.contains(patientUid)) {
				patientUids.add(patientUid);
				patientEntries.add(entry);
			}
		}

		List<EpicPatientMatch> matches = new ArrayList<>(patientEntries.size());

		for (PatientSearchResponse.Entry entry : patientEntries) {
			Set<EpicPatientMatchRule> matchRules = new HashSet<>();

			String patientId = epicClient.extractUIDFromPatientEntry(entry).orElse(null);

			// Ignore records with no UIDs
			if (patientId == null)
				continue;

			applyNameMatchRules(request, entry, matchRules);
			applyGenderMatchRules(request, entry, matchRules);
			applyDateOfBirthMatchRules(request, entry, matchRules);
			applyNationalIdentifierMatchRules(request, entry, matchRules);
			applyAddressMatchRules(request, entry, matchRules);
			applyPhoneNumberMatchRules(request, entry, matchRules);
			applyEmailAddressMatchRules(request, entry, matchRules);

			if (matchRules.size() > 0) {
				int score = matchRules.stream().mapToInt(matchRule -> matchRule.getWeight()).sum();

				if (score > 0) {
					boolean match = score >= getLowThreshold();
					matches.add(new EpicPatientMatch(entry, matchRules, score, match));
				}
			}
		}

		// Sort highest scores first
		Collections.sort(matches, (match1, match2) -> match2.getScore().compareTo(match1.getScore()));

		return matches;
	}

	protected void applyNameMatchRules(@Nonnull EpicPatientMatchRequest request,
																		 @Nonnull PatientSearchResponse.Entry entry,
																		 @Nonnull Set<EpicPatientMatchRule> matchRules) {
		requireNonNull(request);
		requireNonNull(entry);
		requireNonNull(matchRules);

		String firstName = normalizeName(request.getFirstName());
		String lastName = normalizeName(request.getLastName());
		String middleInitial = normalizeName(request.getMiddleInitial());

		boolean matchedFirst = false;
		boolean matchedLast = false;
		boolean matchedMiddleInitial = false;

		// [{"use":"usual","text":"Bob W Smith","family":["Smith"],"given":["Bob","W"]}]
		for (PatientSearchResponse.Entry.Resource.Name name : entry.getResource().getName()) {
			List<String> potentialFirstNames = name.getGiven().stream().map(potentialFirstName -> normalizeName(potentialFirstName)).collect(Collectors.toList());
			List<String> potentialLastNames = name.getFamily().stream().map(potentialLastName -> normalizeName(potentialLastName)).collect(Collectors.toList());

			// Solve case like first name is "Xiao-Zhong" but is represented as ["XIAO", "ZHONG"] and we should exact-match it
			List<String> longFirstNames = new ArrayList<>(potentialFirstNames.size());

			for (String potentialFirstName : potentialFirstNames)
				if (potentialFirstName.length() > 1)
					longFirstNames.add(potentialFirstName);

			if (longFirstNames.size() > 1)
				potentialFirstNames.add(longFirstNames.stream().collect(Collectors.joining("")));

			// Same for last names
			List<String> longLastNames = new ArrayList<>(potentialFirstNames.size());

			for (String potentialLastName : potentialLastNames)
				if (potentialLastName.length() > 1)
					longLastNames.add(potentialLastName);

			if (longLastNames.size() > 1)
				potentialLastNames.add(longLastNames.stream().collect(Collectors.joining("")));

			if (potentialFirstNames.contains(firstName))
				matchedFirst = true;

			if (potentialFirstNames.contains(middleInitial))
				matchedMiddleInitial = true;

			if (potentialLastNames.contains(lastName))
				matchedLast = true;
		}

		if (matchedFirst && matchedLast && (matchedMiddleInitial || middleInitial.isBlank()))
			matchRules.add(EpicPatientMatchRule.EXACT_NAME);
		else if (matchedFirst && matchedLast)
			matchRules.add(EpicPatientMatchRule.EXACT_NAME_WITHOUT_MIDDLE_INITIAL);
	}

	protected void applyGenderMatchRules(@Nonnull EpicPatientMatchRequest request,
																			 @Nonnull PatientSearchResponse.Entry entry,
																			 @Nonnull Set<EpicPatientMatchRule> matchRules) {
		requireNonNull(request);
		requireNonNull(entry);
		requireNonNull(matchRules);

		if (request.getGender() == null)
			return;

		String gender = normalize(request.getGender().name());
		String possibleGender = normalize(entry.getResource().getGender());

		if (gender.equals(possibleGender))
			matchRules.add(EpicPatientMatchRule.EXACT_SEX);
	}

	protected void applyDateOfBirthMatchRules(@Nonnull EpicPatientMatchRequest request,
																						@Nonnull PatientSearchResponse.Entry entry,
																						@Nonnull Set<EpicPatientMatchRule> matchRules) {
		requireNonNull(request);
		requireNonNull(entry);
		requireNonNull(matchRules);

		if (request.getDateOfBirth() == null)
			return;

		String dateOfBirth = getEpicClient().formatDateWithHyphens(request.getDateOfBirth());
		String possibleDateOfBirth = normalize(entry.getResource().getBirthDate());

		LocalDate localDateOfBirth = getEpicClient().parseDateWithHyphens(dateOfBirth);
		LocalDate possibleLocalDateOfBirth = getEpicClient().parseDateWithHyphens(possibleDateOfBirth);

		if (dateOfBirth.equals(possibleDateOfBirth))
			matchRules.add(EpicPatientMatchRule.EXACT_DOB);
		else if (getLevenshtein().distance(dateOfBirth, possibleDateOfBirth) == 1)
			matchRules.add(EpicPatientMatchRule.DOB_ONE_DIGIT_DIFFERENCE);
			// Special support - if month and year match, this is "one digit difference"
		else if (localDateOfBirth.getMonth().equals(possibleLocalDateOfBirth.getMonth()) && localDateOfBirth.getYear() == possibleLocalDateOfBirth.getYear())
			matchRules.add(EpicPatientMatchRule.DOB_ONE_DIGIT_DIFFERENCE);
	}

	protected void applyNationalIdentifierMatchRules(@Nonnull EpicPatientMatchRequest request,
																									 @Nonnull PatientSearchResponse.Entry entry,
																									 @Nonnull Set<EpicPatientMatchRule> matchRules) {
		requireNonNull(request);
		requireNonNull(entry);
		requireNonNull(matchRules);

		if (request.getNationalIdentifier() == null)
			return;

		String ssn = normalize(request.getNationalIdentifier());

		if (ssn.length() != 11 && ssn.length() != 4)
			return;

		String ssnLastFour = ssn.length() == 4 ? ssn : ssn.substring(7);
		String possibleSsnLastFour = normalize(getEpicClient().extractSsnLastFourFromPatientEntry(entry).orElse(null));

		if (possibleSsnLastFour.length() < 4)
			return;

		if (ssn.length() == 4) {
			if (ssnLastFour.equals(possibleSsnLastFour))
				matchRules.add(EpicPatientMatchRule.SSN_LAST_4);

			return;
		}

		// Last 4 of SSN is match (or one-off), so let's pull the entire SSN and compare...
		if (ssnLastFour.equals(possibleSsnLastFour) || getLevenshtein().distance(ssnLastFour, possibleSsnLastFour) == 1) {
			String possibleUid = getEpicClient().extractUIDFromPatientEntry(entry).orElse(null);

			if (possibleUid != null) {
				GetPatientDemographicsRequest demographicsRequest = new GetPatientDemographicsRequest();
				demographicsRequest.setPatientID(possibleUid);
				demographicsRequest.setPatientIDType("UID");
				demographicsRequest.setUserID(getEpicClient().getEpicUserId());
				demographicsRequest.setUserIDType("EXTERNAL");

				GetPatientDemographicsResponse demographicsResponse = getEpicClient().performGetPatientDemographics(demographicsRequest);

				String possibleSsn = demographicsResponse.getNationalIdentifier();

				if (ssn.equals(possibleSsn))
					matchRules.add(EpicPatientMatchRule.EXACT_SSN);
				else if (ssnLastFour.equals(possibleSsnLastFour))
					matchRules.add(EpicPatientMatchRule.SSN_LAST_4);
				else if (getLevenshtein().distance(ssn, possibleSsn) == 1)
					matchRules.add(EpicPatientMatchRule.SSN_ONE_DIGIT_DIFFERENCE);
			}
		}
	}

	protected void applyAddressMatchRules(@Nonnull EpicPatientMatchRequest request,
																				@Nonnull PatientSearchResponse.Entry entry,
																				@Nonnull Set<EpicPatientMatchRule> matchRules) {
		requireNonNull(request);
		requireNonNull(entry);
		requireNonNull(matchRules);

		if (request.getAddress() == null || entry.getResource().getAddress() == null)
			return;

		String line1 = normalizeAddressLine(request.getAddress().getLine1());
		String city = normalizeCity(request.getAddress().getCity());
		String state = normalizeState(request.getAddress().getState());
		String postalCode = normalize(request.getAddress().getPostalCode());

		boolean matchedLine1 = false;
		boolean matchedCity = false;
		boolean matchedState = false;
		boolean matchedPostalCode = false;

		boolean similarLine1 = false;
		boolean similarCity = false;
		boolean similarState = false;
		boolean similarPostalCode = false;

		for (PatientSearchResponse.Entry.Resource.Address possibleAddress : entry.getResource().getAddress()) {
			List<String> possibleLines = possibleAddress.getLine() == null ? Collections.emptyList() : possibleAddress.getLine().stream().map(line -> normalizeAddressLine(line)).collect(Collectors.toList());

			if (possibleLines.contains(line1)) {
				matchedLine1 = true;
			} else {
				for (String possibleLine : possibleLines) {
					if (getLevenshtein().distance(possibleLine, line1) < 6) {
						similarLine1 = true;
						break;
					}
				}
			}

			String possibleCity = normalizeCity(possibleAddress.getCity());
			String possibleState = normalizeState(possibleAddress.getState());
			String possiblePostalCode = normalize(possibleAddress.getPostalCode());

			if (possibleCity.equals(city))
				matchedCity = true;
			else if (getLevenshtein().distance(possibleCity, city) == 1)
				similarCity = true;

			if (possibleState.equals(state))
				matchedState = true;
			else if (getLevenshtein().distance(possibleState, state) == 1)
				similarState = true;

			if (possiblePostalCode.equals(postalCode))
				matchedPostalCode = true;
			else if (getLevenshtein().distance(possiblePostalCode, postalCode) == 1)
				similarPostalCode = true;
		}

		if (matchedLine1 && matchedCity && matchedState && matchedPostalCode)
			matchRules.add(EpicPatientMatchRule.EXACT_ADDRESS);
		else if ((matchedLine1 || similarLine1) && (matchedCity || similarCity) && (matchedState || similarState) && (matchedPostalCode || similarPostalCode))
			matchRules.add(EpicPatientMatchRule.SIMILAR_ADDRESS);
	}

	@Nonnull
	protected String normalizeAddressLine(@Nullable String line) {
		// Rip out punctuation
		line = normalize(line).replaceAll("\\p{Punct}", "");

		String[] lineComponents = line.split(" ");
		List<String> normalizedLineComponents = new ArrayList<>(lineComponents.length);

		for (String lineComponent : lineComponents) {
			String normalizedLineComponent = lineComponent;

			if ("NORTH".equals(lineComponent))
				normalizedLineComponent = "N";
			else if ("SOUTH".equals(lineComponent))
				normalizedLineComponent = "S";
			else if ("EAST".equals(lineComponent))
				normalizedLineComponent = "E";
			else if ("WEST".equals(lineComponent))
				normalizedLineComponent = "W";
			else if ("ROAD".equals(lineComponent))
				normalizedLineComponent = "RD";
			else if ("STREET".equals(lineComponent))
				normalizedLineComponent = "ST";
			else if ("AVENUE".equals(lineComponent))
				normalizedLineComponent = "AVE";
			else if ("BOULEVARD".equals(lineComponent))
				normalizedLineComponent = "BLVD";
			else if ("LANE".equals(lineComponent))
				normalizedLineComponent = "LN";

			normalizedLineComponents.add(normalizedLineComponent);
		}

		return normalizedLineComponents.stream().collect(Collectors.joining(" "));
	}

	@Nonnull
	protected String normalizeCity(@Nullable String city) {
		// Rip out punctuation
		city = normalize(city).replaceAll("\\p{Punct}", "");

		if ("PHILA".equals(city))
			city = "PHILADELPHIA";

		return city;
	}

	@Nonnull
	protected String normalizeState(@Nullable String state) {
		// Rip out punctuation
		state = normalize(state).replaceAll("\\p{Punct}", "");

		// Turn "DE" into "Delaware"
		try {
			if (state.length() == 2)
				state = normalize(AddressState.valueOfAbbreviation(state).name);
		} catch (Exception ignored) {
			// Don't care that this failed
		}

		return state;
	}

	protected void applyPhoneNumberMatchRules(@Nonnull EpicPatientMatchRequest request,
																						@Nonnull PatientSearchResponse.Entry entry,
																						@Nonnull Set<EpicPatientMatchRule> matchRules) {
		requireNonNull(request);
		requireNonNull(entry);
		requireNonNull(matchRules);

		if (request.getPhoneNumber() == null || entry.getResource().getTelecom() == null)
			return;

		String phoneNumber = getEpicClient().formatPhoneNumber(normalize(request.getPhoneNumber()));

		for (PatientSearchResponse.Entry.Resource.Telecom telecom : entry.getResource().getTelecom()) {
			if (!"PHONE".equals(normalize(telecom.getSystem())))
				continue;

			String possiblePhoneNumber = getEpicClient().formatPhoneNumber(normalize(telecom.getValue()));

			if (possiblePhoneNumber.equals(phoneNumber))
				matchRules.add(EpicPatientMatchRule.EXACT_PHONE_NUMBER);
			else if (getLevenshtein().distance(possiblePhoneNumber, phoneNumber) == 1)
				matchRules.add(EpicPatientMatchRule.SIMILAR_PHONE_NUMBER);
		}
	}

	protected void applyEmailAddressMatchRules(@Nonnull EpicPatientMatchRequest request,
																						 @Nonnull PatientSearchResponse.Entry entry,
																						 @Nonnull Set<EpicPatientMatchRule> matchRules) {
		requireNonNull(request);
		requireNonNull(entry);
		requireNonNull(matchRules);

		if (request.getEmailAddress() == null || entry.getResource().getTelecom() == null)
			return;

		String emailAddress = normalize(request.getEmailAddress());

		for (PatientSearchResponse.Entry.Resource.Telecom telecom : entry.getResource().getTelecom()) {
			if (!"EMAIL".equals(normalize(telecom.getSystem())))
				continue;

			String possibleEmailAddress = normalize(telecom.getValue());

			if (possibleEmailAddress.equals(emailAddress))
				matchRules.add(EpicPatientMatchRule.EXACT_EMAIL);
		}
	}

	@Nonnull
	protected String normalize(@Nullable String string) {
		return StringUtils.normalizeSpace(trimToEmpty(string)).toUpperCase(Locale.US);
	}

	@Nonnull
	protected String normalizeName(@Nullable String string) {
		// Rip out punctuation
		string = normalize(string).replaceAll("\\p{Punct}", "");

		if (string.endsWith(" SR"))
			string = string.substring(0, string.length() - 3);

		if (string.endsWith(" JR"))
			string = string.substring(0, string.length() - 3);

		if (string.endsWith(" III"))
			string = string.substring(0, string.length() - 4);

		return string.trim();
	}

	// Borrowed from https://gist.github.com/webdevwilson/5271984
	protected enum AddressState {
		ALABAMA("Alabama", "AL"), ALASKA("Alaska", "AK"), AMERICAN_SAMOA("American Samoa", "AS"), ARIZONA("Arizona", "AZ"), ARKANSAS(
				"Arkansas", "AR"), CALIFORNIA("California", "CA"), COLORADO("Colorado", "CO"), CONNECTICUT("Connecticut", "CT"), DELAWARE(
				"Delaware", "DE"), DISTRICT_OF_COLUMBIA("District of Columbia", "DC"), FEDERATED_STATES_OF_MICRONESIA(
				"Federated States of Micronesia", "FM"), FLORIDA("Florida", "FL"), GEORGIA("Georgia", "GA"), GUAM("Guam", "GU"), HAWAII(
				"Hawaii", "HI"), IDAHO("Idaho", "ID"), ILLINOIS("Illinois", "IL"), INDIANA("Indiana", "IN"), IOWA("Iowa", "IA"), KANSAS(
				"Kansas", "KS"), KENTUCKY("Kentucky", "KY"), LOUISIANA("Louisiana", "LA"), MAINE("Maine", "ME"), MARYLAND("Maryland", "MD"), MARSHALL_ISLANDS(
				"Marshall Islands", "MH"), MASSACHUSETTS("Massachusetts", "MA"), MICHIGAN("Michigan", "MI"), MINNESOTA("Minnesota", "MN"), MISSISSIPPI(
				"Mississippi", "MS"), MISSOURI("Missouri", "MO"), MONTANA("Montana", "MT"), NEBRASKA("Nebraska", "NE"), NEVADA("Nevada",
				"NV"), NEW_HAMPSHIRE("New Hampshire", "NH"), NEW_JERSEY("New Jersey", "NJ"), NEW_MEXICO("New Mexico", "NM"), NEW_YORK(
				"New York", "NY"), NORTH_CAROLINA("North Carolina", "NC"), NORTH_DAKOTA("North Dakota", "ND"), NORTHERN_MARIANA_ISLANDS(
				"Northern Mariana Islands", "MP"), OHIO("Ohio", "OH"), OKLAHOMA("Oklahoma", "OK"), OREGON("Oregon", "OR"), PALAU("Palau",
				"PW"), PENNSYLVANIA("Pennsylvania", "PA"), PUERTO_RICO("Puerto Rico", "PR"), RHODE_ISLAND("Rhode Island", "RI"), SOUTH_CAROLINA(
				"South Carolina", "SC"), SOUTH_DAKOTA("South Dakota", "SD"), TENNESSEE("Tennessee", "TN"), TEXAS("Texas", "TX"), UTAH(
				"Utah", "UT"), VERMONT("Vermont", "VT"), VIRGIN_ISLANDS("Virgin Islands", "VI"), VIRGINIA("Virginia", "VA"), WASHINGTON(
				"Washington", "WA"), WEST_VIRGINIA("West Virginia", "WV"), WISCONSIN("Wisconsin", "WI"), WYOMING("Wyoming", "WY"), UNKNOWN(
				"Unknown", "");

		/**
		 * The state's name.
		 */
		private String name;

		/**
		 * The state's abbreviation.
		 */
		private String abbreviation;

		/**
		 * The set of states addressed by abbreviations.
		 */
		private static final Map<String, AddressState> STATES_BY_ABBR = new HashMap<String, AddressState>();

		/* static initializer */
		static {
			for (AddressState state : values()) {
				STATES_BY_ABBR.put(state.getAbbreviation(), state);
			}
		}

		/**
		 * Constructs a new state.
		 *
		 * @param name         the state's name.
		 * @param abbreviation the state's abbreviation.
		 */
		AddressState(String name, String abbreviation) {
			this.name = name;
			this.abbreviation = abbreviation;
		}

		/**
		 * Returns the state's abbreviation.
		 *
		 * @return the state's abbreviation.
		 */
		public String getAbbreviation() {
			return abbreviation;
		}

		/**
		 * Gets the enum constant with the specified abbreviation.
		 *
		 * @param abbr the state's abbreviation.
		 * @return the enum constant with the specified abbreviation.
		 */
		public static AddressState valueOfAbbreviation(final String abbr) {
			final AddressState state = STATES_BY_ABBR.get(abbr);
			if (state != null) {
				return state;
			} else {
				return UNKNOWN;
			}
		}

		public static AddressState valueOfName(final String name) {
			final String enumName = name.toUpperCase().replaceAll(" ", "_");
			try {
				return valueOf(enumName);
			} catch (final IllegalArgumentException e) {
				return AddressState.UNKNOWN;
			}
		}

		@Override
		public String toString() {
			return name;
		}
	}

	@Nonnull
	public Integer getLowThreshold() {
		return LOW_THRESHOLD;
	}

	@Nonnull
	public Integer getHighThreshold() {
		return HIGH_THRESHOLD;
	}

	@Nonnull
	protected EpicClient getEpicClient() {
		return epicClient;
	}

	@Nonnull
	protected Levenshtein getLevenshtein() {
		return levenshtein;
	}
}
