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
import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.List;
import java.util.Random;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
public class PatientOrderCsvGenerator {
	@Nonnull
	private final Random random;

	public PatientOrderCsvGenerator() {
		this.random = new Random();
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
				csvPrinter.printRecord(

						randomEncounterDepartmentName(),
						randomEncounterDepartmentId(),
						randomReferringPractice(),
						null, // There are 2 referring practice columns for some reason, only 1 should be populated
						randomOrderingProvider(),
						randomBillingProvider(),
						randomLastName(),
						randomFirstName(),
						randomMrn(),
						randomUid(),
						randomSex(),
						randomDob(),
						randomPrimaryPayor(),
						randomPrimaryPlan(),
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
	protected String randomEncounterDepartmentName() {
		return pickRandomElement(List.of("TBD"));
	}

	@Nonnull
	protected String randomEncounterDepartmentId() {
		return pickRandomElement(List.of("TBD"));
	}

	@Nonnull
	protected String randomReferringPractice() {
		return pickRandomElement(List.of("TBD"));
	}

	@Nonnull
	protected String randomOrderingProvider() {
		return pickRandomElement(List.of("TBD"));
	}

	@Nonnull
	protected String randomBillingProvider() {
		return pickRandomElement(List.of("TBD"));
	}

	@Nonnull
	protected String randomFirstName() {
		return pickRandomElement(List.of("TBD"));
	}

	@Nonnull
	protected String randomLastName() {
		return pickRandomElement(List.of("TBD"));
	}

	@Nonnull
	protected String randomMrn() {
		return pickRandomElement(List.of("TBD"));
	}

	@Nonnull
	protected String randomUid() {
		return pickRandomElement(List.of("TBD"));
	}

	@Nonnull
	protected String randomSex() {
		return pickRandomElement(List.of("TBD"));
	}

	@Nonnull
	protected String randomDob() {
		return pickRandomElement(List.of("TBD"));
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
	protected String pickRandomElement(@Nonnull List<String> values) {
		requireNonNull(values);
		return values.get(getRandom().nextInt(values.size()));
	}

	@Nonnull
	protected Random getRandom() {
		return this.random;
	}
}
