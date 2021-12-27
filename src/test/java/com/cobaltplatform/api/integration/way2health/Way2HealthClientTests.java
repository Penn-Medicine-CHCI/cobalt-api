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

package com.cobaltplatform.api.integration.way2health;

import com.cobaltplatform.api.integration.way2health.model.entity.Incident;
import com.cobaltplatform.api.integration.way2health.model.request.FindIncidentsRequest;
import com.cobaltplatform.api.integration.way2health.model.response.PagedResponse;
import org.junit.Test;
import org.testng.Assert;

import javax.annotation.concurrent.ThreadSafe;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class Way2HealthClientTests {
	@Test
	public void testJsonParsing() throws Way2HealthException {
		Way2HealthClient way2HealthClient = new MockWay2HealthClient();

		PagedResponse<Incident> incidentsResponse = way2HealthClient.findIncidents(new FindIncidentsRequest() {{
			setStudyId(123L);
		}});

		Assert.assertEquals(incidentsResponse.getData().size(), 1, "Incident count mismatch in mock data");
	}

	@Test
	public void testRealClient() throws Way2HealthException {
		String environmentVariableName = "COBALT_API_WAY2HEALTH_ACCESS_TOKEN";
		String accessToken = trimToNull(System.getenv(environmentVariableName));

		if (accessToken == null)
			throw new IllegalStateException(format("You must specify a value for environment variable '%s'", environmentVariableName));

		Way2HealthClient way2HealthClient = new DefaultWay2HealthClient(Way2HealthEnvironment.PRODUCTION, accessToken);

		PagedResponse<Incident> incidentsResponse = way2HealthClient.findIncidents(new FindIncidentsRequest() {{
			setStudyId(715L);
			setOrderBy("desc(created_at)");
			setPerPage(1);
		}});

		Assert.assertTrue(incidentsResponse.getData().size() > 0, "No incidents were found");

		// Try pagination
		incidentsResponse = way2HealthClient.findIncidents(incidentsResponse.getMeta().getPagination().getLinks().getNext());

		Assert.assertTrue(incidentsResponse.getData().size() > 0, "Unable to iterate via pagination");
	}
}
