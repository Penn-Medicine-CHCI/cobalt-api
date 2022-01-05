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
import com.cobaltplatform.api.integration.way2health.model.request.UpdateIncidentsRequest;
import com.cobaltplatform.api.integration.way2health.model.response.ListResponse;
import com.cobaltplatform.api.integration.way2health.model.response.ObjectResponse;
import com.cobaltplatform.api.integration.way2health.model.response.PagedResponse;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.util.ArrayList;
import java.util.List;

/**
 * A <a href="https://www.waytohealth.org/">Way2Health</a> REST API client implementation.
 *
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public interface Way2HealthClient {
	/**
	 * Fetch a single incident from Way2Health.
	 * <p>
	 * See https://app.waytohealth.org/api/v2#operation/getIncident
	 *
	 * @param request (nonnull) parameters which dictate the incident to fetch
	 * @return (nonnull) an incident response
	 * @throws Way2HealthException if an error occurred while fetching
	 */
	@Nonnull
	ObjectResponse<Incident> getIncident(@Nonnull GetIncidentRequest request) throws Way2HealthException;

	/**
	 * Fetch a set of incidents from Way2Health.
	 * <p>
	 * See https://app.waytohealth.org/api/v2#operation/getIncidents
	 *
	 * @param request (nonnull) parameters which dictate what incidents to fetch
	 * @return (nonnull) a paged response over the incidents
	 * @throws Way2HealthException if an error occurred while fetching
	 */
	@Nonnull
	PagedResponse<Incident> getIncidents(@Nonnull GetIncidentsRequest request) throws Way2HealthException;

	/**
	 * Fetch a set of incidents from Way2Health.
	 * <p>
	 * See https://app.waytohealth.org/api/v2#operation/getIncidents
	 *
	 * @param pageLink (nonnull) the URL to use to fetch incidents (e.g. from a {@code next} link in {@link PagedResponse})
	 * @return (nonnull) a paged response over the incidents
	 * @throws Way2HealthException if an error occurred while fetching
	 */
	@Nonnull
	PagedResponse<Incident> getIncidents(@Nonnull String pageLink) throws Way2HealthException;

	/**
	 * Fetch all incidents from Way2Health (walk all pages) that conform to the request.
	 * <p>
	 * See https://app.waytohealth.org/api/v2#operation/getIncidents
	 *
	 * @param request (nonnull) parameters which dictate what incidents to fetch
	 * @return (nonnull) a list of incidents
	 * @throws Way2HealthException if an error occurred while fetching
	 */
	@Nonnull
	default List<Incident> getAllIncidents(@Nonnull GetIncidentsRequest request) throws Way2HealthException {
		List<Incident> incidents = new ArrayList<>();
		String nextLink;

		// Make initial call using provided request configuration
		PagedResponse<Incident> incidentsResponse = getIncidents(request);

		incidents.addAll(incidentsResponse.getData());
		nextLink = incidentsResponse.getMeta().getPagination().getLinks().getNext();

		// Walk remaining pages
		while (nextLink != null) {
			incidentsResponse = getIncidents(nextLink);
			incidents.addAll(incidentsResponse.getData());
			nextLink = incidentsResponse.getMeta().getPagination().getLinks().getNext();
		}

		return incidents;
	}

	/**
	 * Update a single incident in Way2Health.
	 * <p>
	 * Example request body:
	 * <pre>
	 * [
	 *   {"op":"add","path":"/comments","value":"Imported to Cobalt”},
	 *   {"op":"replace","path":"/status","value":"Resolved"}
	 * ]
	 * </pre>
	 * <p>
	 * See https://app.waytohealth.org/api/v2#operation/updateIncident
	 *
	 * @param request (nonnull) parameters which dictate how to update an incident
	 * @throws Way2HealthException if an error occurred while updating
	 */
	@Nonnull
	ObjectResponse<Incident> updateIncident(@Nonnull UpdateIncidentRequest request) throws Way2HealthException;

	/**
	 * Batch-update incidents in Way2Health.
	 * <p>
	 * Example request body:
	 * <pre>
	 * [
	 *   {"op":"add","path":"/comments","value":"Imported to Cobalt”},
	 *   {"op":"replace","path":"/status","value":"Resolved"}
	 * ]
	 * </pre>
	 * <p>
	 * See https://app.waytohealth.org/api/v2#operation/updateIncidents
	 *
	 * @param request (nonnull) parameters which dictate how to update a set of incidents
	 * @throws Way2HealthException if an error occurred while updating
	 */
	@Nonnull
	ListResponse<Incident> updateIncidents(@Nonnull UpdateIncidentsRequest request) throws Way2HealthException;
}
