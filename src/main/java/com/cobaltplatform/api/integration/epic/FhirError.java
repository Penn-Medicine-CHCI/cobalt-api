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

package com.cobaltplatform.api.integration.epic;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
public enum FhirError {
	// Invalid parameter such as a nonexistent patient ID: AllergyIntolerance?patient=foo
	INVALID_PARAMETER_4100(4100, FhirErrorSeverity.FATAL, "The resource request contained an invalid parameter"),

	// A request for data that was otherwise valid but no information was documented or found (i.e. a patient with no pertinent implanted devices, or a demographic search where no patients met the search criteria).
	NO_RESULTS_4101(4101, FhirErrorSeverity.WARNING, "Resource request returns no results"),

	// Invalid Resource ID: AllergyIntolerance/foo
	INVALID_ID_4102(4102, FhirErrorSeverity.FATAL, "The read resource request contained an invalid ID"),

	// A read request for a resource that was deleted in the system
	DELETED_4103(4103, FhirErrorSeverity.FATAL, "The resource requested has been deleted"),

	// Failed to determine a SNOMED code for the smoking status.
	REQUIRED_ELEMENT_NOT_AVAILABLE_4104(4104, FhirErrorSeverity.FATAL, "A required FHIR element was not available to send in the response"),

	// Requesting a Patient which has been merged - in this event, in addition to the error response, we will respond with an HTTP Redirect status. To browsers and many HTTP clients, the redirect will be transparent.
	HAS_BEEN_MERGED_4107(4107, FhirErrorSeverity.FATAL, "The read resource request has been merged"),

	// An invalid search request such as : AllergyIntolerance?
	NO_PARAMETERS_PROVIDED_4110(4110, FhirErrorSeverity.FATAL, "No parameters are provided in the search request"),

	// A request missing a required parameter (such as the patient): Condition?category=diagnosis
	REQUIRED_SEARCH_PARAMETER_MISSING_4111(4111, FhirErrorSeverity.FATAL, "Required search parameter missing from request"),

	// A search containing multiple different patient ID: AllergyIntolerance?patient=[ID 1]&patient=[ID 2]
	INVALID_PARAMETER_COMBINATION_4112(4112, FhirErrorSeverity.FATAL, "The resource request contained an invalid combination of parameters"),

	// Making a request for previously accessed paginated search results after the search has expired.
	SESSION_ID_EXPIRED_4113(4113, FhirErrorSeverity.FATAL, "Session ID for cached search results has expired"),

	// An invalid parameter required for searching: Condition?Patient=[ID]&category=foo
	INVALID_PARAMETER_REQUIRED_4115(4115, FhirErrorSeverity.FATAL, "Required search parameter has an invalid value"),

	// This error only applies to DSTU2 version paging functionality.	Called in paging context if the user has gone to the next page of the same session but changed the original query:
	// AllergyIntolerance?patient=[ID 1] <-- original query
	// AllergyIntolerance?patient=[ID 1]&page=2&session=123 <-- valid
	// AllergyIntolerance?patient=[ID 2]&page=2&session=123 <-- error is logged.
	INVALID_QUERY_FOR_SESSION_4116(4116, FhirErrorSeverity.FATAL, "Invalid query for session"),

	// Request for an Immunization resource without a documented CVX code
	NO_CVX_CODE_4117(4117, FhirErrorSeverity.WARNING, "No CVX code for Immunization resource"),

	// Request data that the authenticated user is not allowed access to view (i.e. a patient asking for data about a stranger's allergies).
	UNAUTHORIZED_4118(4118, FhirErrorSeverity.FATAL, "User not authorized for request"),

	// Request data while authenticated as an authorized patient or patient proxy. Inidicates that data available to the patient may not be the complete medical record within the system.
	ADDITIONAL_DATA_MAY_BE_PRESENT_4119(4119, FhirErrorSeverity.WARNING, "Additional data may be present for patient"),

	// A request with dateWritten on a resource for which the parameter is ignored.
	// A request with parameter zzzzzz on any resource.
	UNKNOWN_QUERY_PARAMETER_4122(4122, FhirErrorSeverity.WARNING, "An unknown query parameter was supplied by the client"),

	// This error is logged whenever Patient.Search exceeds 100 results.
	ADDITIONAL_DATA_MAY_EXIST_4127(4127, FhirErrorSeverity.FATAL, "Additional data may exist"),

	// The patient has a sensitive psychiatric visit and you are not part of the psychiatric or care team. This is returned when you would attempt Encounter.Read on this encounter.
	// Note: these checks are configured by each health system and may vary based on their security policies.
	BREAK_THE_GLASS_REQUIRED_4130(4130, FhirErrorSeverity.FATAL, "Break-the-Glass security does not authorize you to access the selected patient/resource"),

	// You must break the glass to access patient data.	The patient is a hospital employee so extra protection is in place such that only those who have provided a reason to access the chart can access all data. This is returned when you attempt a Patient.Read on that patient.
	// Note: these checks are configured by each health system and may vary based on their security policies.
	BREAK_THE_GLASS_REQUIRED_4131(4131, FhirErrorSeverity.FATAL, "The patient you are attempting to access is restricted. You must break the glass to access patient data."),

	// The patient is a hospital employee so extra protection is in place such that only those who have provided a reason to access the chart can access all data.
	// Note: these checks are configured by each health system and may vary based on their security policies.
	BREAK_THE_GLASS_REQUIRED_4134(4134, FhirErrorSeverity.INFORMATION, "The patient you are attempting to access is restricted. You must Break-the-Glass to view patient data."),

	// Maximum document queries has been reached for the day.
	TRY_AGAIN_TOMORROW_4135(4135, FhirErrorSeverity.FATAL, "Please try again tomorrow"),

	// When calling the FamilyMemberHistory API, a relationship type is not mapped.
	INCOMING_TABLE_MAPPING_ERROR_40000(40000, FhirErrorSeverity.FATAL, "Incoming table mapping error"),

	// /api/FHIR/MedicationStatement?patient=el5.bHVk3kkdaA-5ezLMCHQ3&stat=1 where "stat" is an unknown parameter.
	CONTENT_INVALID_59100(59100, FhirErrorSeverity.INFORMATION, "Content invalid against the specification or a profile."),

	// /api/FHIR/R4/MedicationRequest?patient=eHtj-Tb68iBKA8ZsY8Ioq7w3&category=homemeds where "homemeds" is an unknown category value.
	CONTENT_INVALID_59101(59101, FhirErrorSeverity.WARNING, "Content invalid against the specification or a profile."),

	// /api/FHIR/R4/MedicationRequest?patient=fakepatientid where the patient ID is invalid.
	CONTENT_INVALID_59102(59102, FhirErrorSeverity.FATAL, "Content invalid against the specification or a profile."),

	// Invalid json syntax.
	STRUCTURAL_ISSUE_59105(59105, FhirErrorSeverity.FATAL, "A structural issue in the content"),

	// /api/FHIR/STU3/Observation?patient=eTtVpZg2-.J.yFIP1qJbK4A3 where either the category or code parameter must be specified
	REQUIRED_ELEMENT_MISSING_59108(59108, FhirErrorSeverity.FATAL, "Required {element name} missing"),

	// /api/FHIR/STU3/Encounter?patient=e63wRTbPfr1p8UW81d8Seiw3&class=math where the optional class parameter is not a valid category option
	OPTIONAL_ELEMENT_INVALID_59109(59108, FhirErrorSeverity.INFORMATION, "Optional {element name} invalid: {element value}"),

	// /api/FHIR/R4/Observation?patient=e63wRTbPfr1p8UW81d8Seiw3&category=labresults where the required category parameter is not a valid category option
	REQUIRED_ELEMENT_INVALID_59111(59111, FhirErrorSeverity.FATAL, "Required {element name} invalid: {element value}"),

	// This error is logged when fewer data elements than required are included in the request or when the search exceeds 100 results.
	PROCESSING_ISSUES_59133(59133, FhirErrorSeverity.INFORMATION, "Processing issues."),

	// AllergyIntolerance.Create was used to add an allergy to bees, but an allergy to bees already exists in the patient's chart.
	DUPLICATE_RECORD_59141(59141, FhirErrorSeverity.FATAL, "An attempt was made to create a duplicate record."),

	// /api/FHIR/STU3/Binary/eKqBNFDq6lyQ5SoH5lVu3oNUw1qX1ux2nVAxvjy9qQ9GqOvhmga08hSScb7nvH9ismBvLErBhIFIjKWeEpwa6kiO6w1ZAm5wt.LozW5CSo3s3 when this ID is not found.
	REFERENCE_NOT_FOUND_59144(59144, FhirErrorSeverity.FATAL, "The reference provided was not found."),

	//	A request missing a required parameter (such as the patient): Condition?category=diagnosis
	BUSINESS_RULE_FAILED_59159(59159, FhirErrorSeverity.FATAL, "The content/operation failed to pass a business rule, and so could not proceed."),

	// Failed to file the LDA.
	UNEXPECTED_INTERNAL_ERROR_59177(59177, FhirErrorSeverity.FATAL, "An unexpected internal error has occurred."),

	// When using FHIR Observation.Create in a patient-facing app, the patient does not have any Patient-Entered Flowsheets assigned.
	FAILED_TO_FIND_FLOWSHEETS_59187(59187, FhirErrorSeverity.FATAL, "Failed to find any patient-entered flowsheets."),

	// When using FHIR Observation.Create, no single flowsheet row can be found by the codes provided in the request body. It can also be logged if the flowsheet row found is not mapped to the vital sign LOINC code (8716-3).
	FAILED_TO_FIND_FLOWSHEET_59188(59188, FhirErrorSeverity.FATAL, "Failed to find one vital-signs flowsheet row by given codes."),

	// When using FHIR Observation.Create, the API fails to file any readings to the patient's chart.
	FAILED_TO_FILE_READING_59189(59189, FhirErrorSeverity.FATAL, "Failed to file the reading.");

	@Nonnull
	private static final Map<Integer, FhirError> FHIR_ERRORS_BY_CODE;

	@Nonnull
	private final Integer code;
	@Nonnull
	private final FhirErrorSeverity severity;
	@Nonnull
	private final String description;

	static {
		Map<Integer, FhirError> fhirErrorsByCode = new HashMap<>();

		for (FhirError fhirError : FhirError.values())
			fhirErrorsByCode.put(fhirError.getCode(), fhirError);

		FHIR_ERRORS_BY_CODE = Collections.unmodifiableMap(fhirErrorsByCode);
	}

	private FhirError(@Nonnull Integer code,
										@Nonnull FhirErrorSeverity severity,
										@Nonnull String description) {
		requireNonNull(code);
		requireNonNull(severity);
		requireNonNull(description);

		this.code = code;
		this.severity = severity;
		this.description = description;
	}

	@Nonnull
	public static Optional<FhirError> fromCode(@Nullable Integer code) {
		return Optional.ofNullable(FHIR_ERRORS_BY_CODE.get(code));
	}

	@Nonnull
	public Integer getCode() {
		return this.code;
	}

	@Nonnull
	public FhirErrorSeverity getSeverity() {
		return this.severity;
	}

	@Nonnull
	public String getDescription() {
		return this.description;
	}
}
