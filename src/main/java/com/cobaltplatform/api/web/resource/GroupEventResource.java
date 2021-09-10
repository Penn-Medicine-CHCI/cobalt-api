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

import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.model.api.response.GroupEventApiResponse;
import com.cobaltplatform.api.model.api.response.GroupEventApiResponse.GroupEventApiResponseFactory;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.Appointment;
import com.cobaltplatform.api.model.db.GroupEventType;
import com.cobaltplatform.api.model.security.AuthenticationRequired;
import com.cobaltplatform.api.model.service.GroupEvent;
import com.cobaltplatform.api.service.AppointmentService;
import com.cobaltplatform.api.service.GroupEventService;
import com.soklet.web.annotation.GET;
import com.soklet.web.annotation.PathParameter;
import com.soklet.web.annotation.QueryParameter;
import com.soklet.web.annotation.Resource;
import com.soklet.web.exception.NotFoundException;
import com.soklet.web.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;

/**
 * @author Transmogrify, LLC.
 */
@Resource
@Singleton
@ThreadSafe
public class GroupEventResource {
	@Nonnull
	private final GroupEventService groupEventService;
	@Nonnull
	private final AppointmentService appointmentService;
	@Nonnull
	private final GroupEventApiResponseFactory groupEventApiResponseFactory;
	@Nonnull
	private final Provider<CurrentContext> currentContextProvider;
	@Nonnull
	private final Logger logger;

	@Inject
	public GroupEventResource(@Nonnull GroupEventService groupEventService,
														@Nonnull AppointmentService appointmentService,
														@Nonnull GroupEventApiResponseFactory groupEventApiResponseFactory,
														@Nonnull Provider<CurrentContext> currentContextProvider) {
		requireNonNull(groupEventService);
		requireNonNull(appointmentService);
		requireNonNull(groupEventApiResponseFactory);
		requireNonNull(currentContextProvider);

		this.groupEventService = groupEventService;
		this.appointmentService = appointmentService;
		this.groupEventApiResponseFactory = groupEventApiResponseFactory;
		this.currentContextProvider = currentContextProvider;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	@GET("/group-events")
	@AuthenticationRequired
	public ApiResponse groupEvents(@Nonnull @QueryParameter("class") Optional<String> groupEventTypeUrlName) {
		requireNonNull(groupEventTypeUrlName);

		Account account = getCurrentContext().getAccount().get();

		List<GroupEvent> groupEvents = getGroupEventService().findGroupEventsByInstitutionId(account.getInstitutionId(), getCurrentContext().getTimeZone());

		if (groupEventTypeUrlName.isPresent()) {
			String urlName = trimToEmpty(groupEventTypeUrlName.get());
			GroupEventType groupEventType = getGroupEventService().findGroupEventTypeByUrlName(urlName, account.getInstitutionId()).orElse(null);

			if (groupEventType != null)
				groupEvents = groupEvents.stream()
						.filter((groupEvent -> groupEvent.getGroupEventTypeId().equals(groupEventType.getGroupEventTypeId())))
						.collect(Collectors.toList());
			else
				groupEvents = Collections.emptyList();
		}

		List<String> groupEventIds = groupEvents.stream().map((groupEvent) -> groupEvent.getGroupEventId()).collect(Collectors.toList());
		Map<String, Appointment> appointmentsByGroupEventId = getAppointmentService().findAppointmentsForGroupEvents(account.getAccountId(), groupEventIds);

		List<GroupEventApiResponse> finalGroupEvents = groupEvents.stream()
				.map((groupEvent) -> getGroupEventApiResponseFactory().create(groupEvent, appointmentsByGroupEventId.get(groupEvent.getGroupEventId())))
				.collect(Collectors.toList());

		return new ApiResponse(new HashMap<String, Object>() {{
			put("groupEvents", finalGroupEvents);
		}});
	}

	@Nonnull
	@GET("/group-events/{groupEventId}")
	@AuthenticationRequired
	public ApiResponse groupEvent(@Nonnull @PathParameter String groupEventId) {
		requireNonNull(groupEventId);

		Account account = getCurrentContext().getAccount().get();
		GroupEvent groupEvent = getGroupEventService().findGroupEventById(groupEventId, account.getInstitutionId(),
				getCurrentContext().getTimeZone()).orElse(null);
		Map<String, Appointment> appointmentsByGroupEventId = getAppointmentService().findAppointmentsForGroupEvents(account.getAccountId(), List.of(groupEventId));

		if (groupEvent == null)
			throw new NotFoundException();

		return new ApiResponse(new HashMap<String, Object>() {{
			put("groupEvent", getGroupEventApiResponseFactory().create(groupEvent, appointmentsByGroupEventId.get(groupEventId)));
		}});
	}

	@Nonnull
	protected GroupEventService getGroupEventService() {
		return groupEventService;
	}

	@Nonnull
	protected AppointmentService getAppointmentService() {
		return appointmentService;
	}

	@Nonnull
	protected GroupEventApiResponseFactory getGroupEventApiResponseFactory() {
		return groupEventApiResponseFactory;
	}

	@Nonnull
	protected CurrentContext getCurrentContext() {
		return currentContextProvider.get();
	}

	@Nonnull
	protected Logger getLogger() {
		return logger;
	}
}