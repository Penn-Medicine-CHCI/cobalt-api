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

import com.cobaltplatform.api.integration.epic.EpicAppointmentBookingErrorResolver.EpicAppointmentBookingErrorDetails;
import com.cobaltplatform.api.integration.epic.EpicAppointmentBookingErrorResolver.EpicAppointmentBookingErrorResolution;
import com.cobaltplatform.api.integration.epic.EpicAppointmentBookingErrorResolver.EpicAppointmentBookingFailureType;
import com.cobaltplatform.api.integration.epic.EpicAppointmentBookingErrorResolver.EpicAppointmentWarningType;
import com.cobaltplatform.api.util.JsonMapper;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Transmogrify, LLC.
 */
public class EpicAppointmentBookingErrorResolverTests {
	@Test
	public void testParseEpicAppointmentWarningTimeslotUnavailable() {
		String epicExceptionMessage = """
				Bad HTTP response 400 for EPIC endpoint POST https://example.org/api/epic/2014/PatientAccess/External/ScheduleAppointmentWithInsurance/Scheduling/Open/ScheduleWithInsurance with query params [none] and request body [none]. Response body was
				{"Message":"An error occurred while executing the command: MAKE-FAIL details: code:APTWARN ERROR Details:25.","ExceptionMessage":"An error occurred while executing the command: MAKE-FAIL details: code:APTWARN ERROR Details:25.","ExceptionType":"System.Web.HttpException","StackTrace":null}
				""".trim();

		EpicAppointmentBookingErrorResolution resolution = new EpicAppointmentBookingErrorResolver(new JsonMapper()).resolve(epicExceptionMessage);
		Optional<EpicAppointmentBookingErrorDetails> errorDetails = resolution.getErrorDetails();

		assertTrue(errorDetails.isPresent());
		assertEquals(Integer.valueOf(400), errorDetails.get().getHttpStatus());
		assertEquals("MAKE-FAIL", errorDetails.get().getCommand());
		assertEquals("APTWARN", errorDetails.get().getErrorCode());
		assertEquals("25", errorDetails.get().getErrorDetailCode());
		assertEquals(1, errorDetails.get().getErrorDetailCodes().size());
		assertEquals("25", errorDetails.get().getErrorDetailCodes().get(0));
		assertTrue(errorDetails.get().isAppointmentWarning());
		assertEquals(EpicAppointmentWarningType.TIMESLOT_UNAVAILABLE, resolution.getWarningType());
		assertEquals(EpicAppointmentBookingFailureType.TIMESLOT_UNAVAILABLE, resolution.getFailureType());
	}

	@Test
	public void testParseEpicAppointmentWarningMultipleDetailCodes() {
		String epicExceptionMessage = """
				Bad HTTP response 400 for EPIC endpoint POST https://example.org/api/epic/2014/PatientAccess/External/ScheduleAppointmentWithInsurance/Scheduling/Open/ScheduleWithInsurance with query params [none] and request body [none]. Response body was
				{"Message":"An error occurred while executing the command: MAKE-FAIL details: code:APTWARN ERROR Details:27, 34.","ExceptionMessage":"An error occurred while executing the command: MAKE-FAIL details: code:APTWARN ERROR Details:27, 34.","ExceptionType":"System.Web.HttpException","StackTrace":null}
				""".trim();

		EpicAppointmentBookingErrorResolution resolution = new EpicAppointmentBookingErrorResolver(new JsonMapper()).resolve(epicExceptionMessage);
		Optional<EpicAppointmentBookingErrorDetails> errorDetails = resolution.getErrorDetails();

		assertTrue(errorDetails.isPresent());
		assertEquals("APTWARN", errorDetails.get().getErrorCode());
		assertEquals("27", errorDetails.get().getErrorDetailCode());
		assertEquals(2, errorDetails.get().getErrorDetailCodes().size());
		assertEquals("27", errorDetails.get().getErrorDetailCodes().get(0));
		assertEquals("34", errorDetails.get().getErrorDetailCodes().get(1));
		assertEquals(EpicAppointmentWarningType.PICK_DIFFERENT_TIME, resolution.getWarningType());
		assertEquals(EpicAppointmentBookingFailureType.PICK_DIFFERENT_TIME, resolution.getFailureType());
	}

	@Test
	public void testParseEpicAppointmentWarningMixedDetailCodePriority() {
		String epicExceptionMessage = """
				Bad HTTP response 400 for EPIC endpoint POST https://example.org/api/epic/2014/PatientAccess/External/ScheduleAppointmentWithInsurance/Scheduling/Open/ScheduleWithInsurance with query params [none] and request body [none]. Response body was
				{"Message":"An error occurred while executing the command: MAKE-FAIL details: code:APTWARN ERROR Details:34, 25.","ExceptionMessage":"An error occurred while executing the command: MAKE-FAIL details: code:APTWARN ERROR Details:34, 25.","ExceptionType":"System.Web.HttpException","StackTrace":null}
				""".trim();

		EpicAppointmentBookingErrorResolution resolution = new EpicAppointmentBookingErrorResolver(new JsonMapper()).resolve(epicExceptionMessage);
		Optional<EpicAppointmentBookingErrorDetails> errorDetails = resolution.getErrorDetails();

		assertTrue(errorDetails.isPresent());
		assertEquals(2, errorDetails.get().getErrorDetailCodes().size());
		assertEquals("34", errorDetails.get().getErrorDetailCodes().get(0));
		assertEquals("25", errorDetails.get().getErrorDetailCodes().get(1));
		assertEquals(EpicAppointmentWarningType.TIMESLOT_UNAVAILABLE, resolution.getWarningType());
		assertEquals(EpicAppointmentBookingFailureType.TIMESLOT_UNAVAILABLE, resolution.getFailureType());
	}

	@Test
	public void testParseEpicMissingDateOfBirthError() {
		String epicExceptionMessage = """
				Bad HTTP response 400 for EPIC endpoint POST https://example.org/api/epic/2012/EMPI/PatientCreate with query params [none] and request body [none]. Response body was
				{"Message":"An error has occurred.","ExceptionMessage":"An error occurred while executing the command: NO-DATE-OF-BIRTH details: Date of birth is required.","ExceptionType":"Epic.ServiceModel.Internal.ServiceCommandException","StackTrace":null}
				""".trim();

		EpicAppointmentBookingErrorResolution resolution = new EpicAppointmentBookingErrorResolver(new JsonMapper()).resolve(epicExceptionMessage);
		Optional<EpicAppointmentBookingErrorDetails> errorDetails = resolution.getErrorDetails();

		assertTrue(errorDetails.isPresent());
		assertEquals("NO-DATE-OF-BIRTH", errorDetails.get().getCommand());
		assertTrue(errorDetails.get().isMissingRequiredPatientData());
		assertEquals(EpicAppointmentBookingFailureType.MISSING_REQUIRED_PATIENT_DATA, resolution.getFailureType());
	}

	@Test
	public void testParseEpicMissingDateOfBirthCommandMappedWithoutRequiredText() {
		String epicExceptionMessage = """
				Bad HTTP response 400 for EPIC endpoint POST https://example.org/api/epic/2012/EMPI/PatientCreate with query params [none] and request body [none]. Response body was
				{"Message":"An error has occurred.","ExceptionMessage":"An error occurred while executing the command: NO-DATE-OF-BIRTH details: Unknown DOB failure.","ExceptionType":"Epic.ServiceModel.Internal.ServiceCommandException","StackTrace":null}
				""".trim();

		EpicAppointmentBookingErrorResolution resolution = new EpicAppointmentBookingErrorResolver(new JsonMapper()).resolve(epicExceptionMessage);
		Optional<EpicAppointmentBookingErrorDetails> errorDetails = resolution.getErrorDetails();

		assertTrue(errorDetails.isPresent());
		assertEquals("NO-DATE-OF-BIRTH", errorDetails.get().getCommand());
		assertEquals(EpicAppointmentBookingFailureType.MISSING_REQUIRED_PATIENT_DATA, resolution.getFailureType());
	}

	@Test
	public void testParseEpicTemporarilyUnavailableError() {
		String epicExceptionMessage = "Unable to call EPIC endpoint POST https://example.org/api/epic/2014/PatientAccess/External/ScheduleAppointmentWithInsurance/Scheduling/Open/ScheduleWithInsurance with query params [none] and request body [none]";

		EpicAppointmentBookingErrorResolution resolution = new EpicAppointmentBookingErrorResolver(new JsonMapper()).resolve(epicExceptionMessage);
		Optional<EpicAppointmentBookingErrorDetails> errorDetails = resolution.getErrorDetails();

		assertTrue(errorDetails.isPresent());
		assertTrue(errorDetails.get().isEpicTemporarilyUnavailable());
		assertEquals(EpicAppointmentBookingFailureType.EPIC_TEMPORARILY_UNAVAILABLE, resolution.getFailureType());
	}
}
