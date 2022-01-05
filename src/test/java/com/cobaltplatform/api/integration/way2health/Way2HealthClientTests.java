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
import com.cobaltplatform.api.integration.way2health.model.request.GetIncidentRequest;
import com.cobaltplatform.api.integration.way2health.model.request.GetIncidentsRequest;
import com.cobaltplatform.api.integration.way2health.model.request.UpdateIncidentsRequest;
import com.cobaltplatform.api.integration.way2health.model.response.ObjectResponse;
import com.cobaltplatform.api.integration.way2health.model.response.PagedResponse;
import org.junit.Test;
import org.testng.Assert;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.util.List;

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

		PagedResponse<Incident> incidentsResponse = way2HealthClient.getIncidents(new GetIncidentsRequest() {{
			setStudyId(123L);
		}});

		Assert.assertEquals(incidentsResponse.getData().size(), 1, "Incident count mismatch in mock data");
	}

	@Test
	public void testRealClientPagination() throws Way2HealthException {
		Way2HealthClient way2HealthClient = createRealClient();

		PagedResponse<Incident> incidentsResponse = way2HealthClient.getIncidents(new GetIncidentsRequest() {{
			setStudyId(715L);
			// setType("Medical Emergency: Suicide Ideation");
			setOrderBy("desc(created_at)");
			setPerPage(1);
		}});

		Assert.assertTrue(incidentsResponse.getData().size() > 0, "No incidents were found");

		// Try pagination
		incidentsResponse = way2HealthClient.getIncidents(incidentsResponse.getMeta().getPagination().getLinks().getNext());

		Assert.assertTrue(incidentsResponse.getData().size() > 0, "Unable to iterate via pagination");
	}

	@Test
	public void testRealClientPatientData() throws Way2HealthException {
		Way2HealthClient way2HealthClient = createRealClient();

		PagedResponse<Incident> incidentsResponse = way2HealthClient.getIncidents(new GetIncidentsRequest() {{
			setStatus("New");
			setStudyId(715L);
			//setType("Medical Emergency: Suicide Ideation");
			setOrderBy("desc(created_at)");
			setInclude(List.of("comments", "participant", "reporter", "tags", "attachments"));
			setPerPage(1);
		}});

		Assert.assertTrue(incidentsResponse.getData().size() > 0, "No incidents were found");

		Incident firstIncident = incidentsResponse.getData().get(0);

		ObjectResponse<Incident> incidentResponse = way2HealthClient.getIncident(new GetIncidentRequest() {{
			setIncidentId(firstIncident.getId());
			setInclude(List.of("comments", "participant", "reporter", "tags", "attachments"));
		}});

		Assert.assertTrue(incidentResponse.getData() != null, "No incident was found for ID " + firstIncident.getId());
	}

	@Test
	public void testRealUpdateIncidents() throws Way2HealthException {
		Way2HealthClient way2HealthClient = createRealClient();

		way2HealthClient.updateIncidents(new UpdateIncidentsRequest() {{
			setId("in(4297203)");
			setPatchOperations(List.of(
					new PatchOperation() {{
						setOp("add");
						setPath("/comments");
						setValue("Imported to Cobalt");
					}},
					new PatchOperation() {{
						setOp("replace");
						setPath("/status");
						setValue("Resolved");
					}}
			));
		}});
	}

	@Nonnull
	protected Way2HealthClient createRealClient() {
		String environmentVariableName = "COBALT_API_WAY2HEALTH_ACCESS_TOKEN";
		String accessToken = trimToNull(System.getenv(environmentVariableName));

		if (accessToken == null)
			throw new IllegalStateException(format("You must specify a value for environment variable '%s'", environmentVariableName));

		return new DefaultWay2HealthClient(Way2HealthEnvironment.PRODUCTION, accessToken);
	}
}
