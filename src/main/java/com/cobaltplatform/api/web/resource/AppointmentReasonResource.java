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

import com.cobaltplatform.api.model.api.response.AppointmentReasonApiResponse;
import com.cobaltplatform.api.model.db.AppointmentReasonType.AppointmentReasonTypeId;
import com.cobaltplatform.api.model.db.Provider;
import com.cobaltplatform.api.model.security.AuthenticationRequired;
import com.cobaltplatform.api.service.AppointmentService;
import com.cobaltplatform.api.service.ProviderService;
import com.soklet.web.annotation.GET;
import com.soklet.web.annotation.QueryParameter;
import com.soklet.web.annotation.Resource;
import com.soklet.web.exception.NotFoundException;
import com.soklet.web.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Resource
@Singleton
@ThreadSafe
public class AppointmentReasonResource {
	@Nonnull
	private final AppointmentService appointmentService;
	@Nonnull
	private final ProviderService providerService;
	@Nonnull
	private final Logger logger;

	@Inject
	public AppointmentReasonResource(@Nonnull AppointmentService appointmentService,
																	 @Nonnull ProviderService providerService) {
		requireNonNull(appointmentService);
		requireNonNull(providerService);

		this.appointmentService = appointmentService;
		this.providerService = providerService;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	@GET("/appointment-reasons")
	@AuthenticationRequired
	public ApiResponse appointmentReasons(@Nonnull @QueryParameter UUID providerId,
																				@Nonnull @QueryParameter AppointmentReasonTypeId appointmentReasonTypeId) {
		requireNonNull(providerId);
		requireNonNull(appointmentReasonTypeId);

		Provider provider = getProviderService().findProviderById(providerId).orElse(null);

		if (provider == null)
			throw new NotFoundException();

		List<AppointmentReasonApiResponse> appointmentReasons = getAppointmentService().findAppointmentReasons(provider.getInstitutionId(), appointmentReasonTypeId).stream()
				.map(appointmentReason -> new AppointmentReasonApiResponse(appointmentReason))
				.collect(Collectors.toList());

		return new ApiResponse(new HashMap<String, Object>() {{
			put("appointmentReasons", appointmentReasons);
		}});
	}

	@Nonnull
	protected AppointmentService getAppointmentService() {
		return appointmentService;
	}

	@Nonnull
	protected ProviderService getProviderService() {
		return providerService;
	}

	@Nonnull
	protected Logger getLogger() {
		return logger;
	}
}