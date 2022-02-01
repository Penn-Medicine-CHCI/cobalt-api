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
import com.cobaltplatform.api.integration.way2health.model.request.UpdateIncidentRequest;
import com.cobaltplatform.api.integration.way2health.model.response.ObjectResponse;
import com.cobaltplatform.api.integration.way2health.model.response.PagedResponse;
import org.junit.Assert;
import org.junit.Test;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

		Assert.assertEquals("Incident count mismatch in mock data", 1, incidentsResponse.getData().size());
	}

	@Test
	public void testRealClientPagination() throws Way2HealthException {
		Way2HealthClient way2HealthClient = createRealClient();

		String nextLink;
		List<Incident> incidents = new ArrayList<>();

		PagedResponse<Incident> incidentsResponse = way2HealthClient.getIncidents(new GetIncidentsRequest() {{
			setStudyId(715L);
			setOrderBy("desc(created_at)");
			setPerPage(10);
		}});

		int expectedResults = incidentsResponse.getMeta().getPagination().getTotal();

		incidents.addAll(incidentsResponse.getData());
		nextLink = incidentsResponse.getMeta().getPagination().getLinks().getNext();

		while (nextLink != null) {
			incidentsResponse = way2HealthClient.getIncidents(nextLink);
			incidents.addAll(incidentsResponse.getData());
			nextLink = incidentsResponse.getMeta().getPagination().getLinks().getNext();
		}

		Assert.assertEquals("Mismatch in number of incidents from pagination data", expectedResults, incidents.size());
	}

	@Test
	public void testRealClientPatientData() throws Way2HealthException {
		Way2HealthClient way2HealthClient = createRealClient();

		PagedResponse<Incident> incidentsResponse = way2HealthClient.getIncidents(new GetIncidentsRequest() {{
			setStatus("New");
			setStudyId(715L);
			setType("Medical Emergency: Suicide Ideation");
			setOrderBy("desc(created_at)");
			setInclude(List.of("comments", "participant", "reporter", "tags", "attachments"));
			setPerPage(10);
		}});

		Assert.assertTrue("No incidents were found", incidentsResponse.getData().size() > 0);

		Incident firstIncident = incidentsResponse.getData().get(0);

		ObjectResponse<Incident> incidentResponse = way2HealthClient.getIncident(new GetIncidentRequest() {{
			setIncidentId(firstIncident.getId());
			setInclude(List.of("comments", "participant", "reporter", "tags", "attachments"));
		}});

		Assert.assertTrue("No incident was found for ID " + firstIncident.getId(), incidentResponse.getData() != null);
	}

	@Test
	public void testRealUpdateIncident() throws Way2HealthException {
		Way2HealthClient way2HealthClient = createRealClient();

		String commentValue = "Cobalt Test";
		String statusValue = "Resolved";

		ObjectResponse<Incident> incident = way2HealthClient.updateIncident(new UpdateIncidentRequest() {{
			setIncidentId(4297203L);
			setPatchOperations(List.of(
					new PatchOperation() {{
						setOp("add");
						setPath("/comments");
						setValue(commentValue);
					}},
					new PatchOperation() {{
						setOp("replace");
						setPath("/status");
						setValue(statusValue);
					}}
			));
		}});

		List<String> commentValues = incident.getData().getComments().stream()
				.map(comment -> comment.getComment())
				.collect(Collectors.toList());

		Assert.assertTrue("No comment was found with the name we specified", commentValues.contains(commentValue));
		Assert.assertEquals("Status value doesn't match the name we specified", statusValue, incident.getData().getStatus());
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
