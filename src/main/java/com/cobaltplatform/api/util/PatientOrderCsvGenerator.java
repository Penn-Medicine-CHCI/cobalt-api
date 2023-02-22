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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

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

		public FakeDepartment(@Nonnull String id, @Nonnull String name) {
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
			List<String> names = Files.readAllLines(Paths.get("resources/mock/fake-names"), StandardCharsets.UTF_8);
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
				new FakeDepartment("9999901", "Internal Medicine Dept"),
				new FakeDepartment("9999902", "Pediatrics Dept"),
				new FakeDepartment("9999903", "Family Medicine Dept"),
				new FakeDepartment("9999904", "General Internal Medicine Dept"),
				new FakeDepartment("9999905", "Geriatrics (Gerontology) Dept"),
				new FakeDepartment("9999906", "General Obstetrics Dept"),
				new FakeDepartment("9999907", "General Pediatrics Dept"),
				new FakeDepartment("9999908", "Lifestyle Medicine Dept")
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
			List<String> fakeInsuranceLines = Files.readAllLines(Paths.get("resources/mock/fake-insurances"), StandardCharsets.UTF_8);
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
			List<String> fakePayorLines = Files.readAllLines(Paths.get("resources/mock/fake-payors"), StandardCharsets.UTF_8);
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
			for (int i = 0; i < numberOfRows; ++i) {
				FakeDepartment encounterFakeDepartment = pickRandomElement(getFakeDepartments());
				FakeDepartment referringPractice = pickRandomElement(getFakeDepartments());
				FakeProvider orderingProvider = pickRandomElement(getFakeProviders());
				FakeProvider billingProvider = pickRandomElement(getFakeProviders());
				FakePayor primaryPayor = pickRandomElement(getFakePayors());
				FakeInsurance primaryPlan = pickRandomElement(getFakeInsurances());

				csvPrinter.printRecord(
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
						pickRandomElement(getFirstNames()),
						pickRandomElement(getLastNames()),
						randomMrn(),
						randomUid(),
						randomSex(),
						randomDob(),
						format("%s-%s", primaryPayor.getId(), primaryPayor.getName()), // e.g. 128000-IBC
						format("%s-%s", primaryPlan.getId(), primaryPlan.getName()), // e.g. 128002-KEYSTONE HEALTH PLAN EAST
						randomOrderDate(),
						randomOrderId(),
						randomAgeOfOrder(),
						randomCcbhOrderRouting(),
						randomReasonsForReferral(),
						randomDx(),
						randomOrderAssociatedDiagnosisIcd10(),
						randomCallBackNumber(),
						randomPreferredContactHours(),
						randomOrderComments(),
						randomImgCcRecipients(),
						randomPatientAddressLine1(),
						randomPatientAddressLine2(),
						randomCity(),
						randomPatientState(),
						randomZipCode(),
						randomCcbhLastActiveMedOrderSummary(),
						randomCcbhMedicationsList(),
						randomPsychotherapeuticMedLst2Weeks()
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
	protected String randomPrimaryPayor() {
		return pickRandomElement(List.of("TBD"));
	}

	@Nonnull
	protected String randomPrimaryPlan() {
		return pickRandomElement(List.of("TBD"));
	}

	@Nonnull
	protected String randomOrderDate() {
		return pickRandomElement(List.of("TBD"));
	}

	@Nonnull
	protected String randomOrderId() {
		return pickRandomElement(List.of("TBD"));
	}

	@Nonnull
	protected String randomAgeOfOrder() {
		return pickRandomElement(List.of("TBD"));
	}

	@Nonnull
	protected String randomCcbhOrderRouting() {
		return pickRandomElement(List.of("TBD"));
	}

	@Nonnull
	protected String randomReasonsForReferral() {
		return pickRandomElement(List.of("TBD"));
	}

	@Nonnull
	protected String randomDx() {
		return pickRandomElement(List.of("TBD"));
	}

	@Nonnull
	protected String randomOrderAssociatedDiagnosisIcd10() {
		return pickRandomElement(List.of("TBD"));
	}

	@Nonnull
	protected String randomCallBackNumber() {
		return pickRandomElement(List.of("TBD"));
	}

	@Nonnull
	protected String randomPreferredContactHours() {
		return pickRandomElement(List.of("TBD"));
	}

	@Nonnull
	protected String randomOrderComments() {
		return pickRandomElement(List.of("TBD"));
	}

	@Nonnull
	protected String randomImgCcRecipients() {
		return pickRandomElement(List.of("TBD"));
	}

	@Nonnull
	protected String randomPatientAddressLine1() {
		return pickRandomElement(List.of("TBD"));
	}

	@Nonnull
	protected String randomPatientAddressLine2() {
		return pickRandomElement(List.of("TBD"));
	}

	@Nonnull
	protected String randomCity() {
		return pickRandomElement(List.of("TBD"));
	}

	@Nonnull
	protected String randomPatientState() {
		return pickRandomElement(List.of("TBD"));
	}

	@Nonnull
	protected String randomZipCode() {
		return pickRandomElement(List.of("TBD"));
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
}
