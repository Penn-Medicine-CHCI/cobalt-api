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

import com.cobaltplatform.api.IntegrationTestExecutor;
import com.cobaltplatform.api.model.api.request.CreateAccountRequest;
import com.cobaltplatform.api.model.api.request.CreateAppointmentTypeRequest;
import com.cobaltplatform.api.model.api.request.CreateLogicalAvailabilityRequest;
import com.cobaltplatform.api.model.db.AccountSource.AccountSourceId;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.LogicalAvailabilityType.LogicalAvailabilityTypeId;
import com.cobaltplatform.api.model.db.ProviderAvailability;
import com.cobaltplatform.api.model.db.RecurrenceType.RecurrenceTypeId;
import com.cobaltplatform.api.model.db.Role.RoleId;
import com.cobaltplatform.api.model.db.SchedulingSystem.SchedulingSystemId;
import com.cobaltplatform.api.model.db.VisitType.VisitTypeId;
import com.pyranid.Database;
import org.junit.Test;

import javax.annotation.concurrent.ThreadSafe;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class AvailabilityServiceTests {
	@Test
	public void testLogicalAvailabilitySlots() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			final UUID PROVIDER_ID = UUID.randomUUID();
			final String PROVIDER_NAME = "Test Provider";
			final String PROVIDER_EMAIL_ADDRESS = "test@cobaltinnovations.org";
			final InstitutionId PROVIDER_INSTITUTION_ID = InstitutionId.COBALT;

			Database database = app.getInjector().getInstance(Database.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			AvailabilityService availabilityService = app.getInjector().getInstance(AvailabilityService.class);
			AppointmentService appointmentService = app.getInjector().getInstance(AppointmentService.class);

			// 1. Make a provider
			database.execute("INSERT INTO provider (provider_id, institution_id, name, email_address) VALUES (?,?,?,?)",
					PROVIDER_ID, PROVIDER_INSTITUTION_ID, PROVIDER_NAME, PROVIDER_EMAIL_ADDRESS);

			// 2. Make an account for the provider
			UUID accountId = accountService.createAccount(new CreateAccountRequest() {{
				setAccountSourceId(AccountSourceId.ANONYMOUS);
				setInstitutionId(InstitutionId.COBALT);
				setRoleId(RoleId.PROVIDER);
				setEmailAddress("test@cobaltinnovations.org");
			}});

			database.execute("UPDATE account SET provider_id=? WHERE account_id=?", PROVIDER_ID, accountId);

			// 3. Create appointment types
			UUID npvAppointmentTypeId = appointmentService.createAppointmentType(new CreateAppointmentTypeRequest() {{
				setProviderId(PROVIDER_ID);
				setName("Test NPV");
				setDescription("Test NPV Description");
				setVisitTypeId(VisitTypeId.INITIAL);
				setDurationInMinutes(60L);
				setHexColor("#FFFFFF");
				setSchedulingSystemId(SchedulingSystemId.COBALT);
				setPatientIntakeQuestions(Collections.emptyList());
				setScreeningQuestions(Collections.emptyList());
			}});

			// 4. Create logical availability
			availabilityService.createLogicalAvailability(new CreateLogicalAvailabilityRequest() {{
				setProviderId(PROVIDER_ID);
				setAccountId(accountId);
				setLogicalAvailabilityTypeId(LogicalAvailabilityTypeId.OPEN);
				setRecurrenceTypeId(RecurrenceTypeId.NONE);
				setAppointmentTypeIds(List.of(npvAppointmentTypeId));
				setStartDateTime(LocalDateTime.of(
						LocalDate.of(2022, 2, 15),
						LocalTime.of(10, 30)));
				setEndDateTime(LocalDateTime.of(
						LocalDate.of(2022, 2, 15),
						LocalTime.of(14, 30)));
			}});

			// 5. Ask for availability slots
			LocalDateTime startDateTime = LocalDateTime.of(
					LocalDate.of(2022, 2, 15),
					LocalTime.of(9, 00));

			LocalDateTime endDateTime = LocalDateTime.of(
					LocalDate.of(2022, 2, 15),
					LocalTime.of(18, 00));

			// 6. Verify slots
			List<ProviderAvailability> providerAvailabilities = availabilityService.findProviderAvailabilities(PROVIDER_ID, startDateTime, endDateTime);

			// TODO: verify slots...
		});
	}
}
