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
import com.cobaltplatform.api.model.db.CareEncounterCancellationReason;
import org.junit.Test;

import java.util.List;

import static com.cobaltplatform.api.model.db.CareEncounterCancellationReason.CareEncounterCancellationReasonId.CARE_DELIVERED_DURING_CALL;
import static com.cobaltplatform.api.model.db.CareEncounterCancellationReason.CareEncounterCancellationReasonId.DUPLICATE_BOOKING;
import static com.cobaltplatform.api.model.db.CareEncounterCancellationReason.CareEncounterCancellationReasonId.FOLLOW_UP_COMPLETED;
import static com.cobaltplatform.api.model.db.CareEncounterCancellationReason.CareEncounterCancellationReasonId.OTHER;
import static com.cobaltplatform.api.model.db.CareEncounterCancellationReason.CareEncounterCancellationReasonId.PATIENT_REQUESTED;
import static com.cobaltplatform.api.model.db.CareEncounterCancellationReason.CareEncounterCancellationReasonId.SCHEDULING_CONFLICT;
import static com.cobaltplatform.api.model.db.CareEncounterCancellationReason.CareEncounterCancellationReasonId.UNABLE_TO_REACH_PATIENT;
import static org.junit.Assert.assertEquals;

public class CareEncounterCancellationReasonTests {
	@Test
	public void cancellationReasonsAreAvailableInDisplayOrder() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			CareEncounterService careEncounterService = app.getInjector().getInstance(CareEncounterService.class);

			List<CareEncounterCancellationReason> cancellationReasons =
					careEncounterService.findCareEncounterCancellationReasons();

			assertEquals(7, cancellationReasons.size());
			assertReason(cancellationReasons.get(0), PATIENT_REQUESTED, "Patient requested cancellation", 1, false);
			assertReason(cancellationReasons.get(1), CARE_DELIVERED_DURING_CALL, "Care delivered during call", 2, false);
			assertReason(cancellationReasons.get(2), UNABLE_TO_REACH_PATIENT, "Unable to reach patient", 3, false);
			assertReason(cancellationReasons.get(3), SCHEDULING_CONFLICT, "Scheduling conflict", 4, false);
			assertReason(cancellationReasons.get(4), DUPLICATE_BOOKING, "Duplicate booking", 5, false);
			assertReason(cancellationReasons.get(5), FOLLOW_UP_COMPLETED, "Follow-up completed", 6, false);
			assertReason(cancellationReasons.get(6), OTHER, "Other", 7, true);
		});
	}

	protected void assertReason(CareEncounterCancellationReason cancellationReason,
									CareEncounterCancellationReason.CareEncounterCancellationReasonId expectedId,
									String expectedDescription,
									int expectedDisplayOrder,
									boolean expectedFreeformTextRequired) {
		assertEquals(expectedId, cancellationReason.getCareEncounterCancellationReasonId());
		assertEquals(expectedDescription, cancellationReason.getDescription());
		assertEquals(Integer.valueOf(expectedDisplayOrder), cancellationReason.getDisplayOrder());
		assertEquals(Boolean.valueOf(expectedFreeformTextRequired), cancellationReason.getFreeformTextRequired());
	}
}
