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
package com.cobaltplatform.api.util;

import com.cobaltplatform.api.model.service.IcTestPatientEmailAddress;
import com.google.common.base.Strings;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
public class PatientOrderCsvGenerator {
	@Nonnull
	private final Random random;
	@Nonnull
	private final List<FakeDepartment> fakeDepartments;
	@Nonnull
	private final List<FakeProvider> fakeProviders;
	@Nonnull
	private final List<FakeInsurance> fakeInsurances;
	@Nonnull
	private final List<FakePayor> fakePayors;
	@Nonnull
	private final List<String> firstNames;
	@Nonnull
	private final List<String> lastNames;
	@Nonnull
	private final List<Icd10Code> icd10Codes;
	@Nonnull
	private final List<FakeUsAddress> fakeUsAddresses;
	@Nonnull
	private final List<FakeMedication> fakeMedications;

	@ThreadSafe
	protected static class FakeMedication {
		@Nonnull
		private final String id;
		@Nonnull
		private final String name;

		public FakeMedication(@Nonnull String id,
													@Nonnull String name) {
			requireNonNull(id);
			requireNonNull(name);

			this.id = id;
			this.name = name;
		}

		@Nonnull
		public String getId() {
			return this.id;
		}

		@Nonnull
		public String getName() {
			return this.name;
		}
	}

	@ThreadSafe
	protected static class FakeUsAddress {
		@Nonnull
		private final String street;
		@Nonnull
		private final String city;
		@Nonnull
		private final String stateAbbreviation;
		@Nonnull
		private final String zipCode;

		@Nonnull
		public FakeUsAddress(@Nonnull String street,
												 @Nonnull String city,
												 @Nonnull String stateAbbreviation,
												 @Nonnull String zipCode) {
			requireNonNull(street);
			requireNonNull(city);
			requireNonNull(stateAbbreviation);
			requireNonNull(zipCode);

			this.street = street;
			this.city = city;
			this.stateAbbreviation = stateAbbreviation;
			this.zipCode = zipCode;
		}

		@Nonnull
		public String getStreet() {
			return this.street;
		}

		@Nonnull
		public String getCity() {
			return this.city;
		}

		@Nonnull
		public String getStateAbbreviation() {
			return this.stateAbbreviation;
		}

		@Nonnull
		public String getZipCode() {
			return this.zipCode;
		}
	}

	@ThreadSafe
	protected static class Icd10Code {
		@Nonnull
		private final String code;
		@Nonnull
		private final String name;

		@Nonnull
		public Icd10Code(@Nonnull String code,
										 @Nonnull String name) {
			requireNonNull(code);
			requireNonNull(name);

			this.code = code;
			this.name = name;
		}

		@Nonnull
		public String getCode() {
			return this.code;
		}

		@Nonnull
		public String getName() {
			return this.name;
		}
	}

	@ThreadSafe
	protected static class FakeInsurance {
		@Nonnull
		private final String id;
		@Nonnull
		private final String name;

		@Nonnull
		public FakeInsurance(@Nonnull String id,
												 @Nonnull String name) {
			requireNonNull(id);
			requireNonNull(name);

			this.id = id;
			this.name = name;
		}

		@Nonnull
		public String getId() {
			return this.id;
		}

		@Nonnull
		public String getName() {
			return this.name;
		}
	}

	@ThreadSafe
	protected static class FakePayor {
		@Nonnull
		private final String id;
		@Nonnull
		private final String name;

		@Nonnull
		public FakePayor(@Nonnull String id,
										 @Nonnull String name) {
			requireNonNull(id);
			requireNonNull(name);

			this.id = id;
			this.name = name;
		}

		@Nonnull
		public String getId() {
			return this.id;
		}

		@Nonnull
		public String getName() {
			return this.name;
		}
	}

	@ThreadSafe
	protected static class FakeProvider {
		@Nonnull
		private final String id;
		@Nonnull
		private final String firstName;
		@Nonnull
		private final String lastName;
		@Nullable
		private final String middleInitial;

		@Nonnull
		public FakeProvider(@Nonnull String id,
												@Nonnull String firstName,
												@Nonnull String lastName,
												@Nullable String middleInitial) {
			requireNonNull(id);
			requireNonNull(firstName);
			requireNonNull(lastName);

			this.id = id;
			this.firstName = firstName;
			this.lastName = lastName;
			this.middleInitial = middleInitial;
		}

		@Nonnull
		public String getId() {
			return this.id;
		}

		@Nonnull
		public String getFirstName() {
			return this.firstName;
		}

		@Nonnull
		public String getLastName() {
			return this.lastName;
		}

		@Nonnull
		public Optional<String> getMiddleInitial() {
			return Optional.ofNullable(this.middleInitial);
		}
	}

	@ThreadSafe
	protected static class FakeDepartment {
		@Nonnull
		private final String id;
		@Nonnull
		private final String name;

		public FakeDepartment(@Nonnull String id,
													@Nonnull String name) {
			requireNonNull(id);
			requireNonNull(name);

			this.id = id;
			this.name = name;
		}

		@Nonnull
		public String getId() {
			return this.id;
		}

		@Nonnull
		public String getName() {
			return this.name;
		}
	}

	public PatientOrderCsvGenerator() {
		this.random = new Random();

		try {
			List<String> names = Files.readAllLines(Paths.get("resources/mock/ic/fake-names"), StandardCharsets.UTF_8);
			List<String> firstNames = new ArrayList<>();
			List<String> lastNames = new ArrayList<>();

			for (String name : names) {
				name = name.trim();

				if (name.length() == 0)
					continue;

				String[] nameComponents = name.trim().split(" ");

				if (nameComponents.length != 2)
					continue;

				firstNames.add(nameComponents[0]);
				lastNames.add(nameComponents[1]);
			}

			this.firstNames = Collections.unmodifiableList(firstNames);
			this.lastNames = Collections.unmodifiableList(lastNames);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		this.fakeDepartments = List.of(
				new FakeDepartment("9999901", "Internal Medicine"),
				new FakeDepartment("9999902", "Pediatrics"),
				new FakeDepartment("9999903", "Family Medicine"),
				new FakeDepartment("9999904", "General Internal Medicine"),
				new FakeDepartment("9999905", "Geriatrics (Gerontology)"),
				new FakeDepartment("9999906", "General Obstetrics"),
				new FakeDepartment("9999907", "General Pediatrics"),
				new FakeDepartment("9999908", "Lifestyle Medicine")
		);

		List<FakeProvider> fakeProviders = new ArrayList<>(getFirstNames().size());

		int providerId = 100000;

		for (int i = 0; i < getFirstNames().size(); ++i) {
			String firstName = getFirstNames().get(i);
			String lastName = getLastNames().get(i);
			String middleInitial = null;

			if (getRandom().nextBoolean())
				middleInitial = pickRandomElement(getFirstNames()).substring(0, 1);

			FakeProvider fakeProvider = new FakeProvider(String.valueOf(providerId), firstName, lastName, middleInitial);
			fakeProviders.add(fakeProvider);

			++providerId;
		}

		this.fakeProviders = Collections.unmodifiableList(fakeProviders);

		try {
			List<String> fakeInsuranceLines = Files.readAllLines(Paths.get("resources/mock/ic/fake-insurances"), StandardCharsets.UTF_8);
			List<FakeInsurance> fakeInsurances = new ArrayList<>(fakeInsuranceLines.size());
			Set<String> insuranceIds = new HashSet<>();

			for (String fakeInsuranceLine : fakeInsuranceLines) {
				fakeInsuranceLine = fakeInsuranceLine.trim();

				if (fakeInsuranceLine.length() == 0)
					continue;

				fakeInsuranceLine = fakeInsuranceLine.trim().replaceAll("\\s+", " ");

				// e.g. 10064IN003	10064	Medical Expense Insurance Medicare Supplemental Insurance
				String insuranceId = fakeInsuranceLine.substring(0, fakeInsuranceLine.indexOf(" "));
				String remainder = fakeInsuranceLine.substring(insuranceId.length()).trim();
				String insuranceName = remainder.substring(remainder.indexOf(" ")).trim();

				if (insuranceIds.contains(insuranceId))
					continue;

				insuranceIds.add(insuranceId);

				FakeInsurance fakeInsurance = new FakeInsurance(insuranceId, insuranceName);
				fakeInsurances.add(fakeInsurance);
			}

			this.fakeInsurances = Collections.unmodifiableList(fakeInsurances);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		try {
			List<String> fakePayorLines = Files.readAllLines(Paths.get("resources/mock/ic/fake-payors"), StandardCharsets.UTF_8);
			List<FakePayor> fakePayors = new ArrayList<>(fakePayorLines.size());
			Set<String> payorIds = new HashSet<>();

			for (String fakePayorLine : fakePayorLines) {
				fakePayorLine = fakePayorLine.trim();

				if (fakePayorLine.length() == 0)
					continue;

				fakePayorLine = fakePayorLine.trim().replaceAll("\\s+", " ");

				// e.g. 16043	Renaissance Life & Health Insurance Company of America
				String payorId = fakePayorLine.substring(0, 5);
				String payorName = fakePayorLine.substring(6).trim();

				if (payorIds.contains(payorId))
					continue;

				payorIds.add(payorId);

				FakePayor fakePayor = new FakePayor(payorId, payorName);
				fakePayors.add(fakePayor);
			}

			this.fakePayors = Collections.unmodifiableList(fakePayors);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		try {
			List<String> icd10Lines = Files.readAllLines(Paths.get("resources/mock/ic/icd10-codes"), StandardCharsets.UTF_8);
			List<Icd10Code> icd10Codes = new ArrayList<>(icd10Lines.size());

			for (String icd10Line : icd10Lines) {
				icd10Line = icd10Line.trim();

				if (icd10Line.length() == 0)
					continue;

				icd10Line = icd10Line.trim().replaceAll("\\s+", " ");

				// e.g. F18920  Inhalant use, unspecified with intoxication, uncomplicated
				String code = icd10Line.substring(0, icd10Line.indexOf(" "));
				String name = icd10Line.substring(code.length()).trim();

				Icd10Code icd10Code = new Icd10Code(code, name);
				icd10Codes.add(icd10Code);
			}

			this.icd10Codes = Collections.unmodifiableList(icd10Codes);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		try {
			List<String> addressLines = Files.readAllLines(Paths.get("resources/mock/ic/fake-addresses-us"), StandardCharsets.UTF_8);
			List<FakeUsAddress> fakeUsAddresses = new ArrayList<>(addressLines.size());

			for (String addressLine : addressLines) {
				addressLine = addressLine.trim();

				if (addressLine.length() == 0)
					continue;

				addressLine = addressLine.trim().replaceAll("\\s+", " ");

				// e.g. 3365 South Cato Springs Road, Fayetteville AR 72701
				String street = addressLine.substring(0, addressLine.indexOf(","));
				String remainder = addressLine.substring(addressLine.indexOf(",") + 1).trim();
				String zipCode = remainder.substring(remainder.length() - 5);
				remainder = remainder.substring(0, remainder.length() - 5).trim();
				String state = remainder.substring(remainder.length() - 2);
				String city = remainder.substring(0, remainder.length() - 2).trim();

				FakeUsAddress fakeUsAddress = new FakeUsAddress(street, city, state, zipCode);
				fakeUsAddresses.add(fakeUsAddress);
			}

			this.fakeUsAddresses = Collections.unmodifiableList(fakeUsAddresses);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		try {
			List<String> medicationLines = Files.readAllLines(Paths.get("resources/mock/ic/fake-medications"), StandardCharsets.UTF_8);
			List<FakeMedication> fakeMedications = new ArrayList<>(medicationLines.size());

			for (String medicationLine : medicationLines) {
				medicationLine = medicationLine.trim();

				if (medicationLine.length() == 0)
					continue;

				medicationLine = medicationLine.trim().replaceAll("\\s+", " ");

				// e.g. 853790683 Paxil (paroxetine)
				String id = medicationLine.substring(0, medicationLine.indexOf(" "));
				String name = medicationLine.substring(id.length() + 1).trim();

				FakeMedication fakeMedication = new FakeMedication(id, name);
				fakeMedications.add(fakeMedication);
			}

			this.fakeMedications = Collections.unmodifiableList(fakeMedications);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Convenience method for small files - returns a string.
	 */
	@Nonnull
	public String generateCsv(@Nonnull Integer numberOfRows) {
		requireNonNull(numberOfRows);

		try (StringWriter stringWriter = new StringWriter()) {
			generateCsv(numberOfRows, stringWriter);
			return stringWriter.toString();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Suitable for larger CSV files.  It's caller's responsibility to close/clean up the writer.
	 */
	public void generateCsv(@Nonnull Integer numberOfRows,
													@Nonnull Writer writer) {
		requireNonNull(numberOfRows);
		requireNonNull(writer);

		CSVFormat csvFormat = CSVFormat.DEFAULT.withHeader(
				"Test Patient Email Address",
				"Test Patient Password",
				"Encounter Dept Name",
				"Encounter Dept ID",
				"Referring Practice",
				"Referring Practice",
				"Ordering Provider",
				"Billing Provider",
				"Last Name",
				"First Name",
				"MRN",
				"UID",
				"Sex",
				"DOB",
				"Primary Payor",
				"Primary Plan",
				"Order Date",
				"Order ID",
				"Age of Order",
				"CCBH Order Routing",
				"Reasons for Referral",
				"DX",
				"Order Associated Diagnosis (ICD-10)",
				"Call Back Number",
				"Preferred Contact Hours",
				"Order Comments",
				"IMG CC Recipients",
				"Patient Address (Line 1)",
				"Patient Address (Line 2)",
				"City",
				"Patient State",
				"ZIP Code",
				"CCBH Last Active Med Order Summary",
				"CCBH Medications List",
				"Psychotherapeutic Med Lst 2 Weeks"
		);

		try (CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat)) {
			long orderIdBase = Instant.now().toEpochMilli() / 1_000;

			for (int i = 0; i < numberOfRows; ++i) {
				FakeDepartment encounterFakeDepartment = pickRandomElement(getFakeDepartments());
				FakeDepartment referringPractice = pickRandomElement(getFakeDepartments());
				FakeProvider orderingProvider = pickRandomElement(getFakeProviders());
				FakeProvider billingProvider = pickRandomElement(getFakeProviders());
				FakePayor primaryPayor = pickRandomElement(getFakePayors());
				FakeInsurance primaryPlan = pickRandomElement(getFakeInsurances());

				List<Icd10Code> diagnoses = new ArrayList<>();
				Set<String> icd10Codes = new HashSet<>(diagnoses.size());
				int diagnosisCount = getRandom().nextInt(1, 5);

				for (int j = 0; j < diagnosisCount; ++j) {
					Icd10Code icd10Code = getIcd10Codes().get(getRandom().nextInt(getIcd10Codes().size()));

					if (icd10Codes.contains(icd10Code.getCode()))
						continue;

					diagnoses.add(icd10Code);
					icd10Codes.add(icd10Code.getCode());
				}

				FakeUsAddress fakeUsAddress = pickRandomElement(getFakeUsAddresses());

				FakeMedication fakeMedication = getRandom().nextInt(100) < 30 ? pickRandomElement(getFakeMedications()) : null;
				Integer tabletSize = fakeMedication == null ? null : pickRandomElement(List.of(10, 20, 30, 40, 50));
				Integer tabletAmount = fakeMedication == null ? null : pickRandomElement(List.of(10, 20, 30, 40, 50));

				String firstName = pickRandomElement(getFirstNames());
				String lastName = pickRandomElement(getLastNames());
				String uid = randomUid();
				String mrn = randomMrn();
				String testPatientEmail = new IcTestPatientEmailAddress(firstName, lastName, uid, mrn).getEmailAddress();

				// Hat tip to engilyin, see https://stackoverflow.com/a/53349505
				String testPatientPassword = new Random().ints(10, 97, 122)
						.collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
						.toString();

				csvPrinter.printRecord(
						testPatientEmail,
						testPatientPassword,
						encounterFakeDepartment.getName(),
						encounterFakeDepartment.getId(),
						format("%s [%s]", referringPractice.getName(), referringPractice.getId()),
						null, // There are 2 referring practice columns for some reason, only 1 should be populated
						orderingProvider.getMiddleInitial().isPresent()
								? format("%s, %s %s", orderingProvider.getLastName(), orderingProvider.getFirstName(), orderingProvider.getMiddleInitial().get()) // Robinson, Laura E
								: format("%s, %s", orderingProvider.getLastName(), orderingProvider.getFirstName()), // Robinson, Laura
						billingProvider.getMiddleInitial().isPresent()
								? format("%s, %s %s [%s]", billingProvider.getLastName(), billingProvider.getFirstName(), billingProvider.getMiddleInitial().get(), billingProvider.getId()) // Robinson, Laura E [R12345]
								: format("%s, %s [%s]", billingProvider.getLastName(), billingProvider.getFirstName(), billingProvider.getId()), // Robinson, Laura [R12345]
						lastName,
						firstName,
						mrn,
						uid,
						randomSex(),
						randomDob(),
						format("%s-%s", primaryPayor.getId(), primaryPayor.getName()), // e.g. 128000-IBC
						format("%s-%s", primaryPlan.getId(), primaryPlan.getName()), // e.g. 128002-KEYSTONE HEALTH PLAN EAST
						randomOrderDate(),
						String.valueOf(orderIdBase + i),
						randomAgeOfOrder(),
						randomCcbhOrderRouting(),
						randomReasonsForReferral(),
						// Multiple lines separated like this:
						// GAD (generalized anxiety disorder) [213881]
						//
						// Smoker [283397]
						//
						// Alcohol abuse [155739]
						diagnoses.stream()
								.map(icd10Code -> format("%s [%s]", icd10Code.getName(), icd10Code.getCode()))
								.collect(Collectors.joining("\n\n")),
						diagnoses.get(0).getCode(),
						randomCallBackNumber(),
						randomPreferredContactHours(),
						randomOrderComments(),
						randomImgCcRecipients(),
						fakeUsAddress.getStreet(),
						randomPatientAddressLine2(),
						fakeUsAddress.getCity(),
						fakeUsAddress.getStateAbbreviation(),
						fakeUsAddress.getZipCode(),
						fakeMedication == null ? "" : format("Take 1 tablet by mouth daily.<br>E-Prescribe, Disp-%d tablet, R-1", tabletAmount),
						// Escitalopram 10 mg tablet [517587114]
						fakeMedication == null ? "" : format("%s %dmg tablet [%s]", fakeMedication.getName(), tabletSize, fakeMedication.getId()),
						fakeMedication == null ? "" : "Yes"
				);
			}

			csvPrinter.flush();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Nonnull
	protected String randomMrn() {
		return String.valueOf(randomNumberInRange(10_000_000, 99_999_999));
	}

	@Nonnull
	protected String randomUid() {
		return String.valueOf(randomNumberInRange(10_000_000, 99_999_999));
	}

	@Nonnull
	protected String randomSex() {
		return pickRandomElement(List.of("Male", "Female"));
	}

	@Nonnull
	protected String randomDob() {
		return format("%d/%d/%d", randomNumberInRange(1, 12), randomNumberInRange(1, 28), randomNumberInRange(1910, 2010));
	}

	@Nonnull
	protected String randomOrderDate() {
		LocalDate orderDate = LocalDate.now(ZoneId.of("UTC")).minusDays(10);
		orderDate = orderDate.minusDays(randomNumberInRange(1, 60));
		return format("%d/%d/%d", orderDate.getMonthValue(), orderDate.getDayOfMonth(), orderDate.getYear() - 2000);
	}

	@Nonnull
	protected String randomAgeOfOrder() {
		// 5d 05h 43m
		int day = randomNumberInRange(0, 5);
		int hour = randomNumberInRange(0, 23);
		int minute = randomNumberInRange(0, 59);

		return format("%dd %sh %sm",
				day,
				Strings.padStart(String.valueOf(hour), 2, '0'),
				Strings.padStart(String.valueOf(minute), 2, '0')
		);
	}

	@Nonnull
	protected String randomCcbhOrderRouting() {
		return pickRandomElement(List.of("Phone by Resource Center"));
	}

	@Nonnull
	protected String randomReasonsForReferral() {
		return pickRandomElement(List.of(
				"Alcohol or drug use or abuse",
				"Anxiety symptoms",
				"Alcohol or drug use or abuse, Anxiety symptoms",
				"Mood or depression symptoms",
				"Alcohol or drug use or abuse, Mood or depression symptoms"
		));
	}

	@Nonnull
	protected String randomDx() {
		return pickRandomElement(List.of("TBD"));
	}

	@Nonnull
	protected String randomCallBackNumber() {
		return format("215-555-1212");
	}

	@Nonnull
	protected String randomPreferredContactHours() {
		return pickRandomElement(List.of(""));
	}

	@Nonnull
	protected String randomOrderComments() {
		return pickRandomElement(List.of("Your provider's care team is available to help you with your mental health concerns. These may include thoughts, feelings, or behaviors that you would like to change, such as sadness, worry, stress, difficulty sleeping, or substance use.  Your provider recommended that you call the Cobalt Integrated Care Resource Center for a brief assessment and to be connected to care that is right for you. Please call 215-555-1212 today or at your earliest convenience. The Resource Center is available Monday through Friday between the hours of 8:30 AM and 5:00 PM, or you may leave a message after hours. The Resource Center will also be reaching out to you to ensure you get connected to care."));
	}

	@Nonnull
	protected String randomImgCcRecipients() {
		return pickRandomElement(List.of("TBD"));
	}

	@Nonnull
	protected String randomPatientAddressLine2() {
		int randomValue = getRandom().nextInt(10);

		if (randomValue > 8)
			return format("PO Box %s", randomNumberInRange(100, 5_000));

		if (randomValue > 5)
			return "2nd Fl";

		return "";
	}

	@Nonnull
	protected String randomCcbhLastActiveMedOrderSummary() {
		return pickRandomElement(List.of("TBD"));
	}

	@Nonnull
	protected String randomCcbhMedicationsList() {
		return pickRandomElement(List.of("TBD"));
	}

	@Nonnull
	protected String randomPsychotherapeuticMedLst2Weeks() {
		return pickRandomElement(List.of("TBD"));
	}

	@Nonnull
	protected <T> T pickRandomElement(@Nonnull List<T> values) {
		requireNonNull(values);
		return values.get(getRandom().nextInt(values.size()));
	}

	@Nonnull
	protected Integer randomNumberInRange(@Nonnull Integer min,
																				@Nonnull Integer max) {
		requireNonNull(min);
		requireNonNull(max);

		if (max <= min)
			throw new IllegalArgumentException(format("Illegal min/max values %d/%d", min, max));

		return getRandom().nextInt(max - min) + min;
	}

	@Nonnull
	protected Random getRandom() {
		return this.random;
	}

	@Nonnull
	protected List<FakeDepartment> getFakeDepartments() {
		return this.fakeDepartments;
	}

	@Nonnull
	protected List<String> getFirstNames() {
		return this.firstNames;
	}

	@Nonnull
	protected List<String> getLastNames() {
		return this.lastNames;
	}

	@Nonnull
	protected List<FakeProvider> getFakeProviders() {
		return this.fakeProviders;
	}

	@Nonnull
	protected List<FakeInsurance> getFakeInsurances() {
		return this.fakeInsurances;
	}

	@Nonnull
	protected List<FakePayor> getFakePayors() {
		return this.fakePayors;
	}

	@Nonnull
	protected List<Icd10Code> getIcd10Codes() {
		return this.icd10Codes;
	}

	@Nonnull
	protected List<FakeUsAddress> getFakeUsAddresses() {
		return this.fakeUsAddresses;
	}

	@Nonnull
	protected List<FakeMedication> getFakeMedications() {
		return this.fakeMedications;
	}
}
