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

import com.cobaltplatform.api.Configuration;
import com.cobaltplatform.api.model.api.response.AccountSourceApiResponse;
import com.cobaltplatform.api.model.api.response.AccountSourceApiResponse.AccountSourceApiResponseFactory;
import com.cobaltplatform.api.model.api.response.InstitutionApiResponse.InstitutionApiResponseFactory;
import com.cobaltplatform.api.model.db.AccountSource;
import com.cobaltplatform.api.model.db.Institution;
import com.cobaltplatform.api.service.InstitutionService;
import com.cobaltplatform.api.util.ValidationUtility;
import com.cobaltplatform.api.web.request.RequestBodyParser;
import com.soklet.web.annotation.GET;
import com.soklet.web.annotation.QueryParameter;
import com.soklet.web.annotation.Resource;
import com.soklet.web.response.ApiResponse;
import org.checkerframework.checker.nullness.qual.NonNull;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;


/**
 * @author Transmogrify LLC.
 */

@Resource
@Singleton
@ThreadSafe
public class InstitutionResource {

	@Nonnull
	private final RequestBodyParser requestBodyParser;
	@NonNull
	private final InstitutionApiResponseFactory institutionApiResponseFactory;
	@NonNull
	private final InstitutionService institutionService;
	@NonNull
	private final AccountSourceApiResponseFactory accountSourceApiResponseFactory;
	@NonNull
	private final Configuration configuration;

	@Inject
	public InstitutionResource(@Nonnull RequestBodyParser requestBodyParser,
														 @NonNull InstitutionApiResponseFactory institutionApiResponseFactory,
														 @NonNull InstitutionService institutionService,
														 @NonNull AccountSourceApiResponseFactory accountSourceApiResponseFactory,
														 @NonNull Configuration configuration) {
		this.requestBodyParser = requestBodyParser;
		this.institutionApiResponseFactory = institutionApiResponseFactory;
		this.institutionService = institutionService;
		this.accountSourceApiResponseFactory = accountSourceApiResponseFactory;
		this.configuration = configuration;
	}

	@GET("/institution/account-sources")
	public ApiResponse getAccountSources(@QueryParameter Optional<String> subdomain,
																			 @QueryParameter Optional<String> accountSourceId) {
		String requestSubdomain;
		AccountSource.AccountSourceId requestAccountSourceId = null;

		if (subdomain.isPresent())
			requestSubdomain = subdomain.get();
		else
			requestSubdomain = getConfiguration().getDefaultSubdomain();

		if (accountSourceId.isPresent() && ValidationUtility.isValidEnum(accountSourceId.get(),
				AccountSource.AccountSourceId.class))
			requestAccountSourceId = AccountSource.AccountSourceId.valueOf(accountSourceId.get());

		Institution institution = getInstitutionService().findInstitutionBySubdomain(requestSubdomain);

		List<AccountSourceApiResponse> accountSources = requestAccountSourceId == null ? getInstitutionService().findAccountSourcesByInstitutionId(
						institution.getInstitutionId()).stream().map((accountSource) ->
						getAccountSourceApiResponseFactory().create(accountSource, getConfiguration().getEnvironment()))
				.collect(Collectors.toList()) : getInstitutionService().findAccountSourcesByInstitutionIdAndAccountSourceId(
						institution.getInstitutionId(), requestAccountSourceId).stream().map((accountSource) ->
						getAccountSourceApiResponseFactory().create(accountSource, getConfiguration().getEnvironment()))
				.collect(Collectors.toList());

		return new ApiResponse(new HashMap<String, Object>() {{
			put("accountSources", accountSources);
		}});
	}

	@GET("/institution")
	public ApiResponse getInstitution(@QueryParameter Optional<String> subdomain,
																		@QueryParameter Optional<String> accountSourceId) {
		String requestSubdomain;
		AccountSource.AccountSourceId requestAccountSourceId = null;

		if (subdomain.isPresent())
			requestSubdomain = subdomain.get();
		else
			requestSubdomain = getConfiguration().getDefaultSubdomain();

		if (accountSourceId.isPresent() && ValidationUtility.isValidEnum(accountSourceId.get(),
				AccountSource.AccountSourceId.class))
			requestAccountSourceId = AccountSource.AccountSourceId.valueOf(accountSourceId.get());

		// TODO: we should revisit this when we roll out other institutions
		boolean isWww = subdomain.isPresent() && subdomain.get().trim().toLowerCase(Locale.US).equals("www");
		Institution institution = getInstitutionService().findInstitutionBySubdomain(requestSubdomain);

		if (requestAccountSourceId != null) {
			if (requestAccountSourceId.equals(AccountSource.AccountSourceId.ANONYMOUS)) {
				institution.setEmailEnabled(false);
				institution.setSsoEnabled(false);
			} else if (requestAccountSourceId.equals(AccountSource.AccountSourceId.EMAIL_PASSWORD)) {
				institution.setSsoEnabled(false);
				institution.setAnonymousEnabled(false);
			} else {
				institution.setAnonymousEnabled(false);
				institution.setEmailEnabled(false);
			}
		}

		List<AccountSourceApiResponse> accountSources = requestAccountSourceId == null ? getInstitutionService().findAccountSourcesByInstitutionId(
						institution.getInstitutionId()).stream().map((accountSource) ->
						getAccountSourceApiResponseFactory().create(accountSource, getConfiguration().getEnvironment()))
				.collect(Collectors.toList()) : getInstitutionService().findAccountSourcesByInstitutionIdAndAccountSourceId(
						institution.getInstitutionId(), requestAccountSourceId).stream().map((accountSource) ->
						getAccountSourceApiResponseFactory().create(accountSource, getConfiguration().getEnvironment()))
				.collect(Collectors.toList());

		return new ApiResponse(new HashMap<String, Object>() {{
			put("institution", getInstitutionApiResponseFactory().create(institution));
			put("accountSources", accountSources);
		}});
	}

	@Nonnull
	public RequestBodyParser getRequestBodyParser() {
		return requestBodyParser;
	}

	@NonNull
	public InstitutionApiResponseFactory getInstitutionApiResponseFactory() {
		return institutionApiResponseFactory;
	}

	@NonNull
	public InstitutionService getInstitutionService() {
		return institutionService;
	}

	@NonNull
	public AccountSourceApiResponseFactory getAccountSourceApiResponseFactory() {
		return accountSourceApiResponseFactory;
	}

	@NonNull
	public Configuration getConfiguration() {
		return configuration;
	}
}
