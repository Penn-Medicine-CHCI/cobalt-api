/*
 * Copyright 2021 The University of Pennsylvania and Penn Medicine
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

import com.cobaltplatform.api.IntegrationTestExecutor;
import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.context.CurrentContextExecutor;
import com.cobaltplatform.api.model.api.request.CreateAppointmentRequest;
import com.cobaltplatform.api.model.api.request.CreateAppointmentRequest.BookingExperienceId;
import com.cobaltplatform.api.model.api.request.CreateScreeningAnswersRequest;
import com.cobaltplatform.api.model.api.request.CreateScreeningAnswersRequest.CreateAnswerRequest;
import com.cobaltplatform.api.model.api.request.FindAppointmentBookingRequirementsRequest;
import com.cobaltplatform.api.model.api.response.LocationApiResponse;
import com.cobaltplatform.api.model.api.response.InstitutionApiResponse;
import com.cobaltplatform.api.model.api.response.ProviderApiResponse;
import com.cobaltplatform.api.model.api.response.ProviderListDetailsApiResponse.ProviderAppointmentModalityId;
import com.cobaltplatform.api.model.api.response.ProviderListDetailsApiResponse.ProviderAppointmentSelectionTypeId;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.Feature.FeatureId;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.service.AppointmentBookingRequirements;
import com.cobaltplatform.api.model.service.AppointmentBookingRequirements.AppointmentBookingRequirementsDestinationId;
import com.cobaltplatform.api.model.service.FeatureForInstitution;
import com.cobaltplatform.api.model.service.ScreeningQuestionContext;
import com.cobaltplatform.api.model.service.ScreeningSessionDestination;
import com.cobaltplatform.api.model.service.ScreeningSessionDestination.ScreeningSessionDestinationId;
import com.cobaltplatform.api.model.service.ScreeningSessionDestinationResultId;
import com.cobaltplatform.api.util.JsonMapper;
import com.cobaltplatform.api.util.db.DatabaseProvider;
import com.cobaltplatform.api.web.resource.AccountResource;
import com.cobaltplatform.api.web.resource.ProviderResource;
import com.pyranid.Database;
import com.soklet.web.response.ApiResponse;
import org.junit.Test;

import javax.annotation.concurrent.ThreadSafe;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@ThreadSafe
public class CareNavigatorBookingFixtureTests {
	protected static final UUID CARE_NAVIGATOR_ACCOUNT_ID = UUID.fromString("ca4e0000-0000-4000-8000-000000000001");
	protected static final UUID CARE_NAVIGATOR_PROVIDER_ID = UUID.fromString("ca4e0000-0000-4000-8000-000000000002");
	protected static final UUID CARE_NAVIGATOR_APPOINTMENT_TYPE_ID = UUID.fromString("ca4e0000-0000-4000-8000-000000000003");
	protected static final UUID CARE_NAVIGATOR_SCREENING_FLOW_ID = UUID.fromString("ca4e0000-0000-4000-8000-00000000000b");
	protected static final UUID CARE_NAVIGATOR_PROVIDER_LOCATION_ID = UUID.fromString("ca4e0000-0000-4000-8000-00000000000e");

	@Test
	public void careNavigatorFixturePopulatesHomepageFeatureResponse() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			AccountResource accountResource = app.getInjector().getInstance(AccountResource.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			InstitutionService institutionService = app.getInjector().getInstance(InstitutionService.class);
			CurrentContextExecutor currentContextExecutor = app.getInjector().getInstance(CurrentContextExecutor.class);
			Account account = accountService.findAdminAccountsForInstitution(InstitutionId.COBALT).get(0);
			assertEquals(CARE_NAVIGATOR_PROVIDER_ID,
					institutionService.findCareNavigatorBookingProviderIdForInstitutionId(InstitutionId.COBALT).get());

			currentContextExecutor.execute(new CurrentContext.Builder(account, Locale.US, ZoneId.of("America/New_York")).build(), () -> {
				ApiResponse response = accountResource.account(account.getAccountId(), Optional.empty());
				Map<String, Object> model = (Map<String, Object>) response.model().get();
				InstitutionApiResponse institution = (InstitutionApiResponse) model.get("institution");
				FeatureForInstitution resourceNavigator = institution.getFeatures().stream()
						.filter(feature -> feature.getFeatureId() == FeatureId.RESOURCE_NAVIGATOR)
						.findFirst()
						.get();

				assertEquals(200, response.status());
				assertEquals("Connect with a Care Navigator", resourceNavigator.getName());
				assertEquals(CARE_NAVIGATOR_PROVIDER_ID, resourceNavigator.getProviderId());
				assertEquals(CARE_NAVIGATOR_PROVIDER_ID.toString(),
						new JsonMapper().toMap(resourceNavigator).get("providerId"));
			});
		});
	}

	@Test
	public void careNavigatorFixturePopulatesProviderDetailsResponse() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			ProviderResource providerResource = app.getInjector().getInstance(ProviderResource.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			CurrentContextExecutor currentContextExecutor = app.getInjector().getInstance(CurrentContextExecutor.class);
			Account account = accountService.findAdminAccountsForInstitution(InstitutionId.COBALT).get(0);

			currentContextExecutor.execute(new CurrentContext.Builder(account, Locale.US, ZoneId.of("America/New_York")).build(), () -> {
				ApiResponse response = providerResource.provider("cobalt-care-navigator");
				Map<String, Object> model = (Map<String, Object>) response.model().get();
				ProviderApiResponse provider = (ProviderApiResponse) model.get("provider");

				assertEquals(200, response.status());
				assertEquals(CARE_NAVIGATOR_PROVIDER_ID, provider.getProviderId());
				assertEquals("Cobalt Care Navigator", provider.getName());
				assertEquals("Care Navigator", provider.getTitle());
				assertEquals("Cobalt", provider.getEntity());
				assertEquals("Cobalt Care Navigation", provider.getClinic());
				assertEquals("Care Navigation", provider.getSpecialty());
				assertEquals("Care navigation consultations for provider matching, understanding available care options, and identifying next steps.",
						provider.getDescription());
				assertEquals("https://placehold.co/320x320/png?text=Care+Navigator", provider.getImageUrl());
				assertEquals(Boolean.FALSE, provider.getDefaultImageUrl());
				assertEquals("https://fixtures.cobalt.care/providers/cobalt-care-navigator/bio", provider.getBioUrl());
				assertEquals("https://fixtures.cobalt.care/providers/cobalt-care-navigator", provider.getWebsiteUrl());
				assertTrue(provider.getBio().contains("find a mental health provider"));
				assertTrue(provider.getDetailsHtml().contains("How a Care Navigator can help"));
				assertEquals(List.of("Provider matching", "Care options", "Mental health navigation"), provider.getTags());
				assertEquals("+12155551014", provider.getPhoneNumber());
				assertEquals("(215) 555-1014", provider.getFormattedPhoneNumber());
				assertEquals("Care Navigator", provider.getSupportRolesDescription());
				assertEquals(List.of("No Fee"), provider.getPaymentFundingDescriptions());
				assertEquals(ProviderAppointmentSelectionTypeId.APPOINTMENT_PREDETERMINED,
						provider.getAppointmentSelectionTypeId());
				assertNotNull(provider.getScreeningRequirement());
				assertEquals(1, provider.getLocations().size());
				LocationApiResponse location = provider.getLocations().get(0);
				assertEquals(CARE_NAVIGATOR_PROVIDER_LOCATION_ID, location.getLocationId());
				assertEquals("Cobalt Virtual Care", location.getName());
				assertEquals("Virtual Care", location.getShortName());
				assertNull(location.getAddress());
			});
		});
	}

	@Test
	public void careNavigatorFixtureCompletesAssessmentAndAllowsNativeBooking() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			AppointmentService appointmentService = app.getInjector().getInstance(AppointmentService.class);
			ScreeningService screeningService = app.getInjector().getInstance(ScreeningService.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			Account account = accountService.findAdminAccountsForInstitution(InstitutionId.COBALT).get(0);

			assertFixtureGraph(database);

			LocalDate bookingDate = nextWeekday(LocalDate.now(ZoneId.of("America/New_York")).plusDays(14));
			LocalTime bookingTime = LocalTime.of(9, 0);
			FindAppointmentBookingRequirementsRequest requirementsRequest = requirementsRequestFor(account, bookingDate,
					bookingTime);

			AppointmentBookingRequirements initialRequirements =
					appointmentService.findAppointmentBookingRequirements(requirementsRequest, account);

			assertEquals(AppointmentBookingRequirementsDestinationId.SCREENING_SESSION,
					initialRequirements.getAppointmentBookingRequirementsDestinationId());
			assertEquals(CARE_NAVIGATOR_SCREENING_FLOW_ID, initialRequirements.getScreeningFlowId());
			assertEquals(Boolean.TRUE, initialRequirements.getScreeningRequired());
			assertEquals(Boolean.FALSE, initialRequirements.getScreeningSatisfied());
			assertNotNull(initialRequirements.getScreeningSession());

			UUID screeningSessionId = initialRequirements.getScreeningSession().getScreeningSessionId();
			ScreeningQuestionContext questionContext = screeningService
					.findNextUnansweredScreeningQuestionContextByScreeningSessionId(screeningSessionId)
					.get();

			assertEquals("What would you like help navigating?", questionContext.getScreeningQuestion().getQuestionText());
			assertEquals(3, questionContext.getScreeningAnswerOptions().size());

			CreateAnswerRequest answer = new CreateAnswerRequest();
			answer.setScreeningAnswerOptionId(questionContext.getScreeningAnswerOptions().get(0).getScreeningAnswerOptionId());
			CreateScreeningAnswersRequest answerRequest = new CreateScreeningAnswersRequest();
			answerRequest.setScreeningQuestionContextId(questionContext.getScreeningQuestionContextId());
			answerRequest.setCreatedByAccountId(account.getAccountId());
			answerRequest.setAnswers(List.of(answer));
			screeningService.createScreeningAnswers(answerRequest);

			assertNull(screeningService.findNextUnansweredScreeningQuestionContextByScreeningSessionId(screeningSessionId)
					.orElse(null));
			ScreeningSessionDestination destination = screeningService
					.determineDestinationForScreeningSessionId(screeningSessionId)
					.get();
			assertEquals(ScreeningSessionDestinationId.APPOINTMENT_BOOKING_CONFIRMATION,
					destination.getScreeningSessionDestinationId());
			assertEquals(ScreeningSessionDestinationResultId.SUCCESS,
					destination.getScreeningSessionDestinationResultId());

			AppointmentBookingRequirements completedRequirements =
					appointmentService.findAppointmentBookingRequirements(requirementsRequest, account);
			assertEquals(AppointmentBookingRequirementsDestinationId.APPOINTMENT_BOOKING,
					completedRequirements.getAppointmentBookingRequirementsDestinationId());
			assertEquals(Boolean.TRUE, completedRequirements.getScreeningSatisfied());

			String bookingEmailAddress = "care-navigator-booking-test@cobaltinnovations.org";
			database.execute("""
					INSERT INTO account_email_verification (
					  account_email_verification_id, account_id, code, email_address, verified
					) VALUES (?, ?, ?, ?, TRUE)
					""", UUID.randomUUID(), account.getAccountId(), "123456", bookingEmailAddress);

			CreateAppointmentRequest appointmentRequest = new CreateAppointmentRequest();
			appointmentRequest.setAccountId(account.getAccountId());
			appointmentRequest.setCreatedByAcountId(account.getAccountId());
			appointmentRequest.setProviderId(CARE_NAVIGATOR_PROVIDER_ID);
			appointmentRequest.setAppointmentTypeId(CARE_NAVIGATOR_APPOINTMENT_TYPE_ID);
			appointmentRequest.setDate(bookingDate);
			appointmentRequest.setTime(bookingTime);
			appointmentRequest.setFirstName("Care");
			appointmentRequest.setLastName("Navigator Booking Test");
			appointmentRequest.setEmailAddress(bookingEmailAddress);
			appointmentRequest.setPhoneNumber("+12155550123");
			appointmentRequest.setBookingExperienceId(BookingExperienceId.V2);
			appointmentRequest.setAppointmentModalityId(ProviderAppointmentModalityId.VIRTUAL);

			UUID appointmentId = appointmentService.createAppointment(appointmentRequest);
			assertNotNull(appointmentId);
			assertTrue(appointmentService.findAppointmentById(appointmentId).isPresent());
		});
	}

	protected void assertFixtureGraph(Database database) {
		assertEquals(CARE_NAVIGATOR_PROVIDER_ID, database.queryForObject("""
				SELECT provider_id
				FROM account
				WHERE account_id=?
				AND email_address='care-navigator@cobaltinnovations.org'
				AND role_id='PROVIDER'
				""", UUID.class, CARE_NAVIGATOR_ACCOUNT_ID).get());
		assertEquals(Long.valueOf(1L), database.queryForObject("""
				SELECT COUNT(*)
				FROM account_capability
				WHERE account_id=?
				AND account_capability_type_id='NAVIGATOR'
				""", Long.class, CARE_NAVIGATOR_ACCOUNT_ID).get());
		assertEquals(Long.valueOf(1L), database.queryForObject("""
				SELECT COUNT(*)
				FROM provider_support_role
				WHERE provider_id=?
				AND support_role_id='CARE_NAVIGATOR'
				""", Long.class, CARE_NAVIGATOR_PROVIDER_ID).get());
		assertEquals(CARE_NAVIGATOR_SCREENING_FLOW_ID, database.queryForObject("""
				SELECT screening_flow_id
				FROM appointment_type
				WHERE appointment_type_id=?
				AND name='Care Navigation Consultation'
				AND duration_in_minutes=30
				""", UUID.class, CARE_NAVIGATOR_APPOINTMENT_TYPE_ID).get());
		assertEquals(Long.valueOf(1L), database.queryForObject("""
				SELECT COUNT(*)
				FROM logical_availability la
				JOIN logical_availability_appointment_type laat
				  ON laat.logical_availability_id=la.logical_availability_id
				WHERE la.provider_id=?
				AND laat.appointment_type_id=?
				AND la.recur_monday=TRUE
				AND la.recur_tuesday=TRUE
				AND la.recur_wednesday=TRUE
				AND la.recur_thursday=TRUE
				AND la.recur_friday=TRUE
				""", Long.class, CARE_NAVIGATOR_PROVIDER_ID, CARE_NAVIGATOR_APPOINTMENT_TYPE_ID).get());
	}

	protected FindAppointmentBookingRequirementsRequest requirementsRequestFor(Account account,
																					 LocalDate bookingDate,
																					 LocalTime bookingTime) {
		FindAppointmentBookingRequirementsRequest request = new FindAppointmentBookingRequirementsRequest();
		request.setAccountId(account.getAccountId());
		request.setProviderId(CARE_NAVIGATOR_PROVIDER_ID);
		request.setAppointmentTypeId(CARE_NAVIGATOR_APPOINTMENT_TYPE_ID);
		request.setAppointmentSelectionTypeId(ProviderAppointmentSelectionTypeId.APPOINTMENT_PREDETERMINED);
		request.setAppointmentModalityId(ProviderAppointmentModalityId.VIRTUAL);
		request.setDate(bookingDate);
		request.setTime(bookingTime);
		return request;
	}

	protected LocalDate nextWeekday(LocalDate date) {
		while (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY)
			date = date.plusDays(1);

		return date;
	}
}
