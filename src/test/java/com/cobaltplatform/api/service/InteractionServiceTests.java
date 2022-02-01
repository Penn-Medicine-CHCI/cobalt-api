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
import com.cobaltplatform.api.model.db.InteractionInstance;
import org.junit.Assert;
import org.junit.Test;

import javax.annotation.concurrent.ThreadSafe;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class InteractionServiceTests {
	@Test
	public void formatInteractionMessage() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			final UUID INTERACTION_ID = UUID.fromString("45f4082c-4d16-400e-aecd-38e87726f6d9");
			final ZoneId TIME_ZONE = ZoneId.of("America/New_York");

			InteractionService interactionService = app.getInjector().getInstance(InteractionService.class);

			InteractionInstance interactionInstance = new InteractionInstance();
			interactionInstance.setInteractionId(INTERACTION_ID);
			interactionInstance.setStartDateTime(LocalDateTime.now(TIME_ZONE).minusHours(10L));
			interactionInstance.setTimeZone(TIME_ZONE);
			interactionInstance.setCompletedFlag(true);
			interactionInstance.setCompletedDate(Instant.now().minusMillis(5_000L));

			String message = "maxInteractionCount: [maxInteractionCount]\n" +
					"frequencyHoursAndMinutes: [frequencyHoursAndMinutes]\n" +
					"completionTimeHoursAndMinutes: [completionTimeHoursAndMinutes]";

			String formattedMessage = interactionService.formatInteractionOptionResponseMessage(interactionInstance, message);

			Assert.assertEquals("Format mismatch", "maxInteractionCount: 3\n" +
					"frequencyHoursAndMinutes: 24h\n" +
					"completionTimeHoursAndMinutes: 9h 59m 55s", formattedMessage);
		});
	}
}
