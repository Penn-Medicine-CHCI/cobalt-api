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

package com.cobaltplatform.api.service;

import com.cobaltplatform.api.model.api.request.CreateAddressRequest;
import com.cobaltplatform.api.model.api.request.CreatePatientOrderImportRequest;
import com.cobaltplatform.api.model.api.request.CreatePatientOrderRequest;
import com.cobaltplatform.api.model.db.BirthSex.BirthSexId;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.PatientOrder;
import com.cobaltplatform.api.model.db.PatientOrderImport;
import com.cobaltplatform.api.model.db.PatientOrderImportType.PatientOrderImportTypeId;
import com.cobaltplatform.api.model.db.PatientOrderStatus.PatientOrderStatusId;
import com.cobaltplatform.api.util.Normalizer;
import com.cobaltplatform.api.util.ValidationException;
import com.cobaltplatform.api.util.ValidationException.FieldError;
import com.lokalized.Strings;
import com.pyranid.Database;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@Singleton
@ThreadSafe
public class PatientOrderService {
	@Nonnull
	private final Provider<AddressService> addressServiceProvider;
	@Nonnull
	private final Database database;
	@Nonnull
	private final Normalizer normalizer;
	@Nonnull
	private final Strings strings;
	@Nonnull
	private final Logger logger;

	@Inject
	public PatientOrderService(@Nonnull Provider<AddressService> addressServiceProvider,
														 @Nonnull Database database,
														 @Nonnull Normalizer normalizer,
														 @Nonnull Strings strings) {
		requireNonNull(addressServiceProvider);
		requireNonNull(database);
		requireNonNull(normalizer);
		requireNonNull(strings);

		this.addressServiceProvider = addressServiceProvider;
		this.database = database;
		this.normalizer = normalizer;
		this.strings = strings;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	public Optional<PatientOrder> findPatientOrderById(@Nullable UUID patientOrderId) {
		if (patientOrderId == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				SELECT * 
				FROM patient_order 
				WHERE patient_order_id=?
				""", PatientOrder.class, patientOrderId);
	}

	@Nonnull
	public Optional<PatientOrderImport> findPatientOrderImportById(@Nullable UUID patientOrderImportId) {
		if (patientOrderImportId == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				SELECT * 
				FROM patient_order_import
				WHERE patient_order_import_id=?
				""", PatientOrderImport.class, patientOrderImportId);
	}

	@Nonnull
	public List<PatientOrder> findPatientOrdersByAccountId(@Nullable UUID accountId) {
		if (accountId == null)
			return List.of();

		return getDatabase().queryForList("""
				SELECT * 
				FROM patient_order_import
				WHERE patient_account_id=?
				ORDER BY order_date DESC, order_age_in_minutes DESC
				""", PatientOrder.class, accountId);
	}

	@Nonnull
	public List<PatientOrder> findPatientOrdersByPatientOrderImportId(@Nullable UUID patientOrderImportId) {
		if (patientOrderImportId == null)
			return List.of();

		return getDatabase().queryForList("""
				SELECT * 
				FROM patient_order
				WHERE patient_order_import_id=?
				ORDER BY order_date DESC, order_age_in_minutes DESC
				""", PatientOrder.class, patientOrderImportId);
	}

	@Nonnull
	public UUID createPatientOrderImport(@Nonnull CreatePatientOrderImportRequest request) {
		requireNonNull(request);

		InstitutionId institutionId = request.getInstitutionId();
		PatientOrderImportTypeId patientOrderImportTypeId = request.getPatientOrderImportTypeId();
		UUID accountId = request.getAccountId();
		String csvContent = trimToNull(request.getCsvContent());
		UUID patientOrderImportId = UUID.randomUUID();
		ValidationException validationException = new ValidationException();

		if (institutionId == null)
			validationException.add(new FieldError("institutionId", getStrings().get("Institution ID is required.")));

		if (patientOrderImportTypeId == null)
			validationException.add(new FieldError("patientOrderImportTypeId", getStrings().get("Patient Order Import Type ID is required.")));

		if (patientOrderImportTypeId == PatientOrderImportTypeId.CSV && csvContent == null)
			validationException.add(new FieldError("csvContent", getStrings().get("CSV file is required.")));

		if (patientOrderImportTypeId == PatientOrderImportTypeId.CSV && accountId == null)
			validationException.add(new FieldError("accountId", getStrings().get("Account ID is required.")));

		// TODO: revisit when we support EPIC imports directly
		if (patientOrderImportTypeId != PatientOrderImportTypeId.CSV)
			throw new IllegalArgumentException(format("We do not yet support %s.%s", PatientOrderImportTypeId.class.getSimpleName(),
					patientOrderImportTypeId.name()));

		if (validationException.hasErrors())
			throw validationException;

		getDatabase().execute("""
				INSERT INTO patient_order_import (
				patient_order_import_id,
				patient_order_import_type_id,
				institution_id,
				account_id,
				raw_order
				) VALUES (?,?,?,?,?)
				""", patientOrderImportId, patientOrderImportTypeId, institutionId, accountId, csvContent);

		if (patientOrderImportTypeId == PatientOrderImportTypeId.CSV) {
			Map<Integer, ValidationException> validationExceptionsByRowNumber = new HashMap<>();
			int rowNumber = 0;

			// Pull data from the CSV
			try (Reader reader = new StringReader(csvContent)) {
				for (CSVRecord record : CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader)) {
					CreatePatientOrderRequest patientOrderRequest = new CreatePatientOrderRequest();
					patientOrderRequest.setPatientOrderImportId(patientOrderImportId);
					patientOrderRequest.setInstitutionId(institutionId);
					patientOrderRequest.setEncounterDepartmentName(trimToNull(record.get("Encounter Dept Name")));
					patientOrderRequest.setEncounterDepartmentId(trimToNull(record.get("Encounter Dept ID")));

					// Referring Practice has 2 fields with the same name (currently...)
					// So we try the first one, and if it's null, we try the second
					patientOrderRequest.setReferringPracticeName(trimToNull(record.get(2)));

					if (patientOrderRequest.getReferringPracticeName() == null)
						patientOrderRequest.setReferringPracticeName(trimToNull(record.get(3)));

					patientOrderRequest.setOrderingProviderName(trimToNull(record.get("Ordering Provider")));
					patientOrderRequest.setBillingProviderName(trimToNull(record.get("Billing Provider")));
					patientOrderRequest.setPatientLastName(trimToNull(record.get("Last Name")));
					patientOrderRequest.setPatientFirstName(trimToNull(record.get("First Name")));
					patientOrderRequest.setPatientMrn(trimToNull(record.get("MRN")));
					patientOrderRequest.setPatientId(trimToNull(record.get("UID")));
					patientOrderRequest.setPatientIdType("UID");
					patientOrderRequest.setPatientBirthSexId(trimToNull(record.get("Sex")));
					patientOrderRequest.setPatientBirthdate(trimToNull(record.get("DOB")));
					patientOrderRequest.setPrimaryPayor(trimToNull(record.get("Primary Payor")));
					patientOrderRequest.setPrimaryPlan(trimToNull(record.get("Primary Plan")));
					patientOrderRequest.setOrderDate(trimToNull(record.get("Order Date")));
					patientOrderRequest.setOrderId(trimToNull(record.get("Order ID")));
					patientOrderRequest.setOrderAge(trimToNull(record.get("Age of Order")));
					patientOrderRequest.setRouting(trimToNull(record.get("CCBH Order Routing")));
					patientOrderRequest.setReasonForReferral(trimToNull(record.get("Reasons for Referral")));
					patientOrderRequest.setDiagnosis(trimToNull(record.get("DX")));
					patientOrderRequest.setAssociatedDiagnosis(trimToNull(record.get("Order Associated Diagnosis (ICD-10)")));
					patientOrderRequest.setCallbackPhoneNumber(trimToNull(record.get("Call Back Number")));
					patientOrderRequest.setPreferredContactHours(trimToNull(record.get("Preferred Contact Hours")));
					patientOrderRequest.setComments(trimToNull(record.get("Order Comments")));
					patientOrderRequest.setCcRecipients(trimToNull(record.get("IMG CC Recipients")));
					patientOrderRequest.setPatientAddressLine1(trimToNull(record.get("Patient Address (Line 1)")));
					patientOrderRequest.setPatientAddressLine2(trimToNull(record.get("Patient Address (Line 2)")));
					patientOrderRequest.setPatientLocality(trimToNull(record.get("City")));
					patientOrderRequest.setPatientRegion(trimToNull(record.get("Patient State")));
					patientOrderRequest.setPatientPostalCode(trimToNull(record.get("ZIP Code")));
					patientOrderRequest.setLastActiveMedicationOrderSummary(trimToNull(record.get("CCBH Last Active Med Order Summary")));
					patientOrderRequest.setMedications(trimToNull(record.get("CCBH Medications List")));
					patientOrderRequest.setRecentPsychotherapeuticMedications(trimToNull(record.get("Psychotherapeutic Med Lst 2 Weeks")));

					try {
						createPatientOrder(patientOrderRequest);
					} catch (ValidationException e) {
						validationExceptionsByRowNumber.put(rowNumber, e);
					}

					++rowNumber;
				}
			} catch (IOException e) {
				// In practice, we should never hit IOException because the Reader is operating over an in-memory String
				throw new UncheckedIOException("Unable to read CSV string", e);
			}

			// If any row-level validation exceptions, group all the errors per row into a single line.
			// Then throw back the list of lines to the client.
			// example: "Row 1: Patient ID is required. Callback Phone Number is invalid.", "Row 3: Patient Last Name is required."
			if (validationExceptionsByRowNumber.size() > 0) {
				List<String> globalErrors = new ArrayList<>();
				List<Integer> rowNumbers = validationExceptionsByRowNumber.keySet().stream().sorted().toList();

				for (Integer currentRowNumber : rowNumbers) {
					ValidationException currentValidationException = validationExceptionsByRowNumber.get(currentRowNumber);
					List<String> rowErrors = new ArrayList<>(currentValidationException.getGlobalErrors());
					rowErrors.addAll(currentValidationException.getFieldErrors().stream()
							.map(fieldError -> fieldError.getError())
							.collect(Collectors.toSet()));

					String rowErrorsAsString = rowErrors.stream().collect(Collectors.joining(" "));

					globalErrors.add(getStrings().get("Row {{rowNumber}}: {{rowErrors}}", new HashMap<>() {{
						put("rowNumber", currentRowNumber + 1);
						put("rowErrors", rowErrorsAsString);
					}}));
				}

				throw new ValidationException(globalErrors, List.of());
			}
		}

		return patientOrderImportId;
	}

	@Nonnull
	public UUID createPatientOrder(@Nonnull CreatePatientOrderRequest request) {
		requireNonNull(request);

		PatientOrderStatusId patientOrderStatusId = PatientOrderStatusId.NEW;
		UUID patientOrderImportId = request.getPatientOrderImportId();
		InstitutionId institutionId = request.getInstitutionId();
		String encounterDepartmentId = trimToNull(request.getEncounterDepartmentId());
		String encounterDepartmentIdType = trimToNull(request.getEncounterDepartmentIdType());
		String encounterDepartmentName = trimToNull(request.getEncounterDepartmentName());
		String referringPracticeId = trimToNull(request.getReferringPracticeId());
		String referringPracticeIdType = trimToNull(request.getReferringPracticeIdType());
		String referringPracticeName = trimToNull(request.getReferringPracticeName());
		String orderingProviderId = trimToNull(request.getOrderingProviderId());
		String orderingProviderIdType = trimToNull(request.getOrderingProviderIdType());
		String orderingProviderName = trimToNull(request.getOrderingProviderName());
		String billingProviderId = trimToNull(request.getBillingProviderId());
		String billingProviderIdType = trimToNull(request.getBillingProviderIdType());
		String billingProviderName = trimToNull(request.getBillingProviderName());
		String patientLastName = trimToNull(request.getPatientLastName());
		String patientFirstName = trimToNull(request.getPatientFirstName());
		String patientMrn = trimToNull(request.getPatientMrn());
		String patientId = trimToNull(request.getPatientId());
		String patientIdType = trimToNull(request.getPatientIdType());
		String patientBirthSexIdAsString = trimToNull(request.getPatientBirthSexId());
		BirthSexId patientBirthSexId = null;
		String patientBirthdateAsString = trimToNull(request.getPatientBirthdate());
		LocalDate patientBirthdate = null;
		String patientAddressLine1 = trimToNull(request.getPatientAddressLine1());
		String patientAddressLine2 = trimToNull(request.getPatientAddressLine2());
		String patientLocality = trimToNull(request.getPatientLocality());
		String patientRegion = trimToNull(request.getPatientRegion());
		String patientPostalCode = trimToNull(request.getPatientPostalCode());
		String patientCountryCode = trimToNull(request.getPatientCountryCode());
		UUID patientAddressId = null;
		String primaryPayor = trimToNull(request.getPrimaryPayor());
		String primaryPlan = trimToNull(request.getPrimaryPlan());
		String orderDateAsString = trimToNull(request.getOrderDate());
		LocalDate orderDate = null;
		String orderAge = trimToNull(request.getOrderAge());
		Long orderAgeInMinutes = null;
		String orderId = trimToNull(request.getOrderId());
		String routing = trimToNull(request.getRouting());
		String reasonForReferral = trimToNull(request.getReasonForReferral());
		String diagnosis = trimToNull(request.getDiagnosis());
		String associatedDiagnosis = trimToNull(request.getAssociatedDiagnosis());
		String callbackPhoneNumber = trimToNull(request.getCallbackPhoneNumber());
		String preferredContactHours = trimToNull(request.getPreferredContactHours());
		String comments = trimToNull(request.getComments());
		String ccRecipients = trimToNull(request.getCcRecipients());
		String lastActiveMedicationOrderSummary = trimToNull(request.getLastActiveMedicationOrderSummary());
		String medications = trimToNull(request.getMedications());
		String recentPsychotherapeuticMedications = trimToNull(request.getRecentPsychotherapeuticMedications());
		UUID patientOrderId = UUID.randomUUID();
		ValidationException validationException = new ValidationException();

		// TODO: revisit when we support non-US institutions
		// Example: "2/25/21"
		DateTimeFormatter twoDigitYearDateFormatter = DateTimeFormatter.ofPattern("M/d/yy", Locale.US);
		// Example: "2/25/2021"
		DateTimeFormatter fourDigitYearDateFormatter = DateTimeFormatter.ofPattern("M/d/yyyy", Locale.US);

		if (patientOrderImportId == null)
			validationException.add(new FieldError("patientOrderImportId", getStrings().get("Patient Order Import ID is required.")));

		if (institutionId == null)
			validationException.add(new FieldError("institutionId", getStrings().get("Institution ID is required.")));

		if (patientMrn == null)
			validationException.add(new FieldError("patientMrn", getStrings().get("Patient MRN is required.")));

		if (patientId == null)
			validationException.add(new FieldError("patientId", getStrings().get("Patient ID is required.")));

		if (patientIdType == null)
			validationException.add(new FieldError("patientIdType", getStrings().get("Patient ID Type is required.")));

		if (orderId == null)
			validationException.add(new FieldError("orderId", getStrings().get("Order ID is required.")));

		if (orderDateAsString == null) {
			validationException.add(new FieldError("orderDate", getStrings().get("Order date is required.")));
		} else {
			try {
				orderDate = LocalDate.parse(orderDateAsString, twoDigitYearDateFormatter);
			} catch (Exception e) {
				validationException.add(new FieldError("orderDate", getStrings().get("Unrecognized order date format: {{orderDate}}",
						Map.of("orderDate", orderDateAsString))));
			}
		}

		if (orderAge == null) {
			validationException.add(new FieldError("orderAge", getStrings().get("Order age is required.")));
		} else {
			// Order Age example: "5d 05h 43m"
			int days = 0;
			int hours = 0;
			int minutes = 0;

			try {
				for (String orderAgeComponent : orderAge.split(" ")) {
					if (orderAgeComponent.endsWith("d")) {
						days = Integer.parseInt(orderAgeComponent.replace("d", ""), 10);
					} else if (orderAgeComponent.endsWith("h")) {
						hours = Integer.parseInt(orderAgeComponent.replace("h", ""), 10);
					} else if (orderAgeComponent.endsWith("m")) {
						minutes = Integer.parseInt(orderAgeComponent.replace("m", ""), 10);
					} else if (orderAgeComponent.length() > 0) {
						throw new IllegalArgumentException(format("Unexpected format for order age component '%s'", orderAgeComponent));
					}
				}

				orderAgeInMinutes = Duration.ofDays(days).plus(Duration.ofHours(hours)).plus(Duration.ofMinutes(minutes)).toMinutes();
			} catch (Exception e) {
				getLogger().warn(format("Unable to process order age string %s", orderAge), e);
			}
		}

		if (patientBirthdateAsString != null) {
			try {
				patientBirthdate = LocalDate.parse(patientBirthdateAsString, fourDigitYearDateFormatter);
			} catch (Exception e) {
				validationException.add(new FieldError("patientBirthdate", getStrings().get("Unrecognized patient birthdate format: {{patientBirthdate}}",
						Map.of("patientBirthdate", patientBirthdateAsString))));
			}
		}

		if (patientBirthSexIdAsString != null) {
			String normalizedPatientBirthSexIdAsString = patientBirthSexIdAsString.toUpperCase(Locale.US);

			if ("MALE".equals(normalizedPatientBirthSexIdAsString))
				patientBirthSexId = BirthSexId.MALE;
			else if ("FEMALE".equals(normalizedPatientBirthSexIdAsString))
				patientBirthSexId = BirthSexId.FEMALE;
		}

		// Fall back to UNKNOWN if we're not sure
		if (patientBirthSexId == null)
			patientBirthSexId = BirthSexId.UNKNOWN;

		try {
			String postalName = Normalizer.normalizeName(patientFirstName, patientLastName).get();
			
			patientAddressId = getAddressService().createAddress(new CreateAddressRequest() {{
				setPostalName(postalName);
				setStreetAddress1(patientAddressLine1);
				setStreetAddress2(patientAddressLine2);
				setLocality(patientLocality);
				setRegion(patientRegion);
				setPostalCode(patientPostalCode);
				// TODO: revisit when we support non-US institutions
				setCountryCode(patientCountryCode == null ? "US" : patientCountryCode);
			}});
		} catch (ValidationException addressValidationException) {
			validationException.add(addressValidationException);
		}

		if (validationException.hasErrors())
			throw validationException;

		getDatabase().execute("""
						  INSERT INTO patient_order (
						  patient_order_id,
						  patient_order_status_id,
						  patient_order_import_id,
						  institution_id,
						  encounter_department_id,
						  encounter_department_id_type,
						  encounter_department_name,
						  referring_practice_id,
						  referring_practice_id_type,
						  referring_practice_name,
						  ordering_provider_id,
						  ordering_provider_id_type,
						  ordering_provider_name,
						  billing_provider_id,
						  billing_provider_id_type,
						  billing_provider_name,
						  patient_last_name,
						  patient_first_name,
						  patient_mrn,
						  patient_id,
						  patient_id_type,
						  patient_birth_sex_id,
						  patient_birthdate,
						  patient_address_id,
						  primary_payor,
						  primary_plan,
						  order_date,
						  order_age_in_minutes,
						  order_id,
						  routing,
						  reason_for_referral,
						  diagnosis,
						  associated_diagnosis,
						  callback_phone_number,
						  preferred_contact_hours,
						  comments,
						  cc_recipients,
						  last_active_medication_order_summary,
						  medications,
						  recent_psychotherapeutic_medications
						) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
						""",
				patientOrderId, patientOrderStatusId, patientOrderImportId, institutionId, encounterDepartmentId,
				encounterDepartmentIdType, encounterDepartmentName, referringPracticeId, referringPracticeIdType,
				referringPracticeName, orderingProviderId, orderingProviderIdType, orderingProviderName, billingProviderId,
				billingProviderIdType, billingProviderName, patientLastName, patientFirstName, patientMrn, patientId,
				patientIdType, patientBirthSexId, patientBirthdate, patientAddressId, primaryPayor, primaryPlan,
				orderDate, orderAgeInMinutes, orderId, routing, reasonForReferral, diagnosis, associatedDiagnosis,
				callbackPhoneNumber, preferredContactHours, comments, ccRecipients, lastActiveMedicationOrderSummary,
				medications, recentPsychotherapeuticMedications);

		return patientOrderId;
	}

	@Nonnull
	protected AddressService getAddressService() {
		return this.addressServiceProvider.get();
	}

	@Nonnull
	protected Database getDatabase() {
		return this.database;
	}

	@Nonnull
	protected Normalizer getNormalizer() {
		return this.normalizer;
	}

	@Nonnull
	protected Strings getStrings() {
		return this.strings;
	}

	@Nonnull
	protected Logger getLogger() {
		return this.logger;
	}
}
