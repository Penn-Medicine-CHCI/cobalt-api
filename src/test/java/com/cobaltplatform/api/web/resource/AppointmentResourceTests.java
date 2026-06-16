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

package com.cobaltplatform.api.web.resource;

import com.cobaltplatform.api.IntegrationTestExecutor;
import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.context.CurrentContextExecutor;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.service.AccountService;
import com.cobaltplatform.api.util.ValidationException;
import com.cobaltplatform.api.util.ValidationException.FieldError;
import org.junit.Test;

import java.time.ZoneId;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Transmogrify, LLC.
 */
public class AppointmentResourceTests {
	@Test
	public void createAppointmentRejectsMissingFirstNameAndLastName() {
		assertCreateAppointmentNameValidation("{}", new FieldError("firstName", "First name is required."),
				new FieldError("lastName", "Last name is required."));
	}

	@Test
	public void createAppointmentRejectsBlankFirstNameAndLastName() {
		assertCreateAppointmentNameValidation("""
				{
				  "firstName": "   ",
				  "lastName": "   "
				}
				""", new FieldError("firstName", "First name is required."),
				new FieldError("lastName", "Last name is required."));
	}

	protected void assertCreateAppointmentNameValidation(String requestBody,
																											 FieldError firstNameFieldError,
																											 FieldError lastNameFieldError) {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			AppointmentResource appointmentResource = app.getInjector().getInstance(AppointmentResource.class);
			Account account = app.getInjector().getInstance(AccountService.class)
					.findAdminAccountsForInstitution(InstitutionId.COBALT).get(0);
			CurrentContextExecutor currentContextExecutor = app.getInjector().getInstance(CurrentContextExecutor.class);

			currentContextExecutor.execute(new CurrentContext.Builder(account, Locale.US, ZoneId.of("America/New_York")).build(), () -> {
				try {
					appointmentResource.createAppointment(requestBody);
					fail("Expected appointment creation to fail validation.");
				} catch (ValidationException e) {
					assertEquals(0, e.getGlobalErrors().size());
					assertEquals(2, e.getFieldErrors().size());
					assertTrue(e.getFieldErrors().contains(firstNameFieldError));
					assertTrue(e.getFieldErrors().contains(lastNameFieldError));
				}
			});
		});
	}
}
