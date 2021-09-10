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

import com.lokalized.Strings;
import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.model.api.request.CreateFollowupRequest;
import com.cobaltplatform.api.model.api.request.FindFollowupsRequest;
import com.cobaltplatform.api.model.api.response.AppointmentApiResponse.AppointmentApiResponseFactory;
import com.cobaltplatform.api.model.api.response.AvailabilityTimeApiResponse.AvailabilityTimeApiResponseFactory;
import com.cobaltplatform.api.model.api.response.ClinicApiResponse.ClinicApiResponseFactory;
import com.cobaltplatform.api.model.api.response.FollowupApiResponse.FollowupApiResponseFactory;
import com.cobaltplatform.api.model.api.response.FollowupApiResponse.FollowupApiResponseSupplement;
import com.cobaltplatform.api.model.api.response.ProviderApiResponse.ProviderApiResponseFactory;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.Followup;
import com.cobaltplatform.api.model.db.Role;
import com.cobaltplatform.api.model.security.AuthenticationRequired;
import com.cobaltplatform.api.service.AccountService;
import com.cobaltplatform.api.service.AppointmentService;
import com.cobaltplatform.api.service.AssessmentScoringService;
import com.cobaltplatform.api.service.AssessmentService;
import com.cobaltplatform.api.service.ClinicService;
import com.cobaltplatform.api.service.FollowupService;
import com.cobaltplatform.api.service.ProviderService;
import com.cobaltplatform.api.util.Formatter;
import com.cobaltplatform.api.web.request.RequestBodyParser;
import com.soklet.web.annotation.GET;
import com.soklet.web.annotation.POST;
import com.soklet.web.annotation.PathParameter;
import com.soklet.web.annotation.QueryParameter;
import com.soklet.web.annotation.RequestBody;
import com.soklet.web.annotation.Resource;
import com.soklet.web.exception.AuthorizationException;
import com.soklet.web.exception.NotFoundException;
import com.soklet.web.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Resource
@Singleton
@ThreadSafe
public class FollowupResource {
	@Nonnull
	private final AssessmentService assessmentService;
	@Nonnull
	private final AssessmentScoringService assessmentScoringService;
	@Nonnull
	private final ProviderService providerService;
	@Nonnull
	private final AppointmentService appointmentService;
	@Nonnull
	private final ClinicService clinicService;
	@Nonnull
	private final FollowupService followupService;
	@Nonnull
	private final AccountService accountService;
	@Nonnull
	private final ProviderApiResponseFactory providerApiResponseFactory;
	@Nonnull
	private final ClinicApiResponseFactory clinicApiResponseFactory;
	@Nonnull
	private final AppointmentApiResponseFactory appointmentApiResponseFactory;
	@Nonnull
	private final AvailabilityTimeApiResponseFactory availabilityTimeApiResponseFactory;
	@Nonnull
	private final FollowupApiResponseFactory followupApiResponseFactory;
	@Nonnull
	private final javax.inject.Provider<CurrentContext> currentContextProvider;
	@Nonnull
	private final RequestBodyParser requestBodyParser;
	@Nonnull
	private final Formatter formatter;
	@Nonnull
	private final Strings strings;
	@Nonnull
	private final Logger logger;

	@Inject
	public FollowupResource(@Nonnull AssessmentService assessmentService,
													@Nonnull AssessmentScoringService assessmentScoringService,
													@Nonnull ProviderService providerService,
													@Nonnull AppointmentService appointmentService,
													@Nonnull ClinicService clinicService,
													@Nonnull FollowupService followupService,
													@Nonnull AccountService accountService,
													@Nonnull ProviderApiResponseFactory providerApiResponseFactory,
													@Nonnull ClinicApiResponseFactory clinicApiResponseFactory,
													@Nonnull AppointmentApiResponseFactory appointmentApiResponseFactory,
													@Nonnull AvailabilityTimeApiResponseFactory availabilityTimeApiResponseFactory,
													@Nonnull FollowupApiResponseFactory followupApiResponseFactory,
													@Nonnull javax.inject.Provider<CurrentContext> currentContextProvider,
													@Nonnull RequestBodyParser requestBodyParser,
													@Nonnull Formatter formatter,
													@Nonnull Strings strings) {
		requireNonNull(assessmentService);
		requireNonNull(assessmentScoringService);
		requireNonNull(providerService);
		requireNonNull(appointmentService);
		requireNonNull(clinicService);
		requireNonNull(followupService);
		requireNonNull(accountService);
		requireNonNull(providerApiResponseFactory);
		requireNonNull(clinicApiResponseFactory);
		requireNonNull(appointmentApiResponseFactory);
		requireNonNull(availabilityTimeApiResponseFactory);
		requireNonNull(followupApiResponseFactory);
		requireNonNull(currentContextProvider);
		requireNonNull(requestBodyParser);
		requireNonNull(formatter);
		requireNonNull(strings);

		this.assessmentService = assessmentService;
		this.assessmentScoringService = assessmentScoringService;
		this.providerService = providerService;
		this.appointmentService = appointmentService;
		this.clinicService = clinicService;
		this.followupService = followupService;
		this.accountService = accountService;
		this.providerApiResponseFactory = providerApiResponseFactory;
		this.clinicApiResponseFactory = clinicApiResponseFactory;
		this.appointmentApiResponseFactory = appointmentApiResponseFactory;
		this.availabilityTimeApiResponseFactory = availabilityTimeApiResponseFactory;
		this.followupApiResponseFactory = followupApiResponseFactory;
		this.currentContextProvider = currentContextProvider;
		this.requestBodyParser = requestBodyParser;
		this.formatter = formatter;
		this.strings = strings;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	@GET("/followups/{followupId}")
	@AuthenticationRequired
	public ApiResponse followup(@Nonnull @PathParameter UUID followupId) {
		requireNonNull(followupId);

		Account account = getCurrentContext().getAccount().get();
		Followup followup = getFollowupService().findFollowupById(followupId).orElse(null);

		if (followup == null)
			throw new NotFoundException();

		if (!followup.getCreatedByAccountId().equals(account.getAccountId()))
			throw new AuthorizationException();

		return new ApiResponse(new HashMap<String, Object>() {{
			put("followup", getFollowupApiResponseFactory().create(followup, Collections.singleton(FollowupApiResponseSupplement.ALL)));
		}});
	}

	@Nonnull
	@POST("/followups")
	@AuthenticationRequired
	public ApiResponse createFollowup(@Nonnull @RequestBody String requestBody) {
		requireNonNull(requestBody);

		Account account = getCurrentContext().getAccount().get();

		CreateFollowupRequest request = getRequestBodyParser().parse(requestBody, CreateFollowupRequest.class);
		request.setCreatedByAccountId(account.getAccountId());

		UUID followupId = getFollowupService().createFollowup(request);
		Followup followup = getFollowupService().findFollowupById(followupId).get();

		return new ApiResponse(new HashMap<String, Object>() {{
			put("followup", getFollowupApiResponseFactory().create(followup, Collections.singleton(FollowupApiResponseSupplement.ALL)));
		}});
	}

	@Nonnull
	@GET("/followups")
	@AuthenticationRequired
	public ApiResponse followups(@Nonnull @QueryParameter UUID accountId,
															 @Nonnull @QueryParameter Optional<FindFollowupsRequest.FilterBy> filterBy) {
		requireNonNull(accountId);
		requireNonNull(filterBy);

		Account account = getCurrentContext().getAccount().get();
		Account followupAccount = getAccountService().findAccountById(accountId).orElse(null);
		List<Followup> followups = new ArrayList<>();

		if (followupAccount != null) {
			// Some users can find followups on behalf of other patients
			if (account.getRoleId() == Role.RoleId.SUPER_ADMINISTRATOR) {
				// Superadmin can find any followups
			} else if (account.getRoleId() == Role.RoleId.MHIC || account.getRoleId() == Role.RoleId.ADMINISTRATOR) {
				// "Normal" admins can find followups within the same institution
				if (!account.getInstitutionId().equals(followupAccount.getInstitutionId()))
					throw new AuthorizationException();
			} else {
				// If you are not a special role, you can only search for yourself
				if (!followupAccount.getAccountId().equals(account.getAccountId()))
					throw new AuthorizationException();
			}

			FindFollowupsRequest request = new FindFollowupsRequest();
			request.setAccountId(followupAccount.getAccountId());
			request.setFilterBy(filterBy.orElse(null));
			request.setTimeZone(account.getTimeZone());

			followups.addAll(getFollowupService().findFollowups(request));
		}

		return new ApiResponse(new HashMap<String, Object>() {{
			put("followups", followups.stream()
					.map(followup -> getFollowupApiResponseFactory().create(followup, Collections.singleton(FollowupApiResponseSupplement.ALL)))
					.collect(Collectors.toList()));
		}});
	}

	@Nonnull
	@POST("/followups/{followupId}/cancel")
	@AuthenticationRequired
	public void cancelFollowup(@Nonnull @PathParameter UUID followupId) {
		requireNonNull(followupId);

		Account account = getCurrentContext().getAccount().get();
		Followup followup = getFollowupService().findFollowupById(followupId).orElse(null);

		if (followup != null) {
			if (!followup.getCreatedByAccountId().equals(account.getAccountId()))
				throw new AuthorizationException();

			getFollowupService().cancelFollowup(followupId);
		}
	}

	@Nonnull
	protected AssessmentService getAssessmentService() {
		return assessmentService;
	}

	@Nonnull
	protected AssessmentScoringService getAssessmentScoringService() {
		return assessmentScoringService;
	}

	@Nonnull
	protected ProviderService getProviderService() {
		return providerService;
	}

	@Nonnull
	protected ClinicService getClinicService() {
		return clinicService;
	}

	@Nonnull
	protected AppointmentService getAppointmentService() {
		return appointmentService;
	}

	@Nonnull
	protected ProviderApiResponseFactory getProviderApiResponseFactory() {
		return providerApiResponseFactory;
	}

	@Nonnull
	protected ClinicApiResponseFactory getClinicApiResponseFactory() {
		return clinicApiResponseFactory;
	}

	@Nonnull
	protected AppointmentApiResponseFactory getAppointmentApiResponseFactory() {
		return appointmentApiResponseFactory;
	}

	@Nonnull
	protected AvailabilityTimeApiResponseFactory getAvailabilityTimeApiResponseFactory() {
		return availabilityTimeApiResponseFactory;
	}

	@Nonnull
	protected FollowupService getFollowupService() {
		return followupService;
	}

	@Nonnull
	protected AccountService getAccountService() {
		return accountService;
	}

	@Nonnull
	protected FollowupApiResponseFactory getFollowupApiResponseFactory() {
		return followupApiResponseFactory;
	}

	@Nonnull
	protected CurrentContext getCurrentContext() {
		return currentContextProvider.get();
	}

	@Nonnull
	protected RequestBodyParser getRequestBodyParser() {
		return requestBodyParser;
	}

	@Nonnull
	protected Formatter getFormatter() {
		return formatter;
	}

	@Nonnull
	protected Strings getStrings() {
		return strings;
	}

	@Nonnull
	protected Logger getLogger() {
		return logger;
	}
}