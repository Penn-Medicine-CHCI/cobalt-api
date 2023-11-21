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

package com.cobaltplatform.api.integration.mixpanel;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.annotation.concurrent.ThreadSafe;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import static com.soklet.util.LoggingUtils.initializeLogback;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class MixpanelClientTests {
	@BeforeClass
	public static void initialize() {
		initializeLogback(Paths.get("config/local/logback.xml"));
	}

	@Test
	public void testMixpanelClient() throws Exception {
		Long projectId = Long.valueOf(Files.readString(
				Path.of("resources/test/mixpanel-project-id"), StandardCharsets.UTF_8));
		String serviceAccountUsername = Files.readString(
				Path.of("resources/test/mixpanel-service-account-username"), StandardCharsets.UTF_8);
		String serviceAccountSecret = Files.readString(
				Path.of("resources/test/mixpanel-service-account-secret"), StandardCharsets.UTF_8);

		MixpanelClient mixpanelClient = new DefaultMixpanelClient(projectId, serviceAccountUsername, serviceAccountSecret);

		// Pull all events for a single date and discard the "$identify" ones
		List<MixpanelEvent> mixpanelEvents = mixpanelClient.findEventsForDateRange(
						LocalDate.of(2023, 8, 2),
						LocalDate.of(2023, 8, 2)
				).stream()
				.filter(mixpanelEvent -> !mixpanelEvent.getEvent().equals("$identify"))
				.collect(Collectors.toList());

		Assert.assertTrue("Mixpanel events missing", mixpanelEvents.size() > 0);
	}
}

