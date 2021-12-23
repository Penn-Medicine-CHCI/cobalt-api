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
import com.cobaltplatform.api.integration.way2health.model.request.PatchIncidentsRequest;
import com.cobaltplatform.api.integration.way2health.model.response.BasicResponse;
import com.cobaltplatform.api.integration.way2health.model.response.PagedResponse;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

/**
 * A <a href="https://www.waytohealth.org/">Way2Health</a> REST API client implementation.
 *
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public interface Way2HealthClient {
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
	PagedResponse<Incident> findIncidents(@Nonnull FindIncidentsRequest request) throws Way2HealthException;

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
	PagedResponse<Incident> findIncidents(@Nonnull String pageLink) throws Way2HealthException;

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
	 * @param request
	 * @throws Way2HealthException if an error occurred while fetching
	 */
	@Nonnull
	BasicResponse<Incident> patchIncidents(@Nonnull PatchIncidentsRequest request) throws Way2HealthException;
}
