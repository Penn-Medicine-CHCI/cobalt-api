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
import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.integration.enterprise.EnterprisePluginProvider;
import com.cobaltplatform.api.integration.microsoft.MicrosoftAuthenticator;
import com.cobaltplatform.api.integration.microsoft.request.AuthenticationRedirectRequest;
import com.cobaltplatform.api.model.api.response.AccountSourceApiResponse;
import com.cobaltplatform.api.model.api.response.AccountSourceApiResponse.AccountSourceApiResponseFactory;
import com.cobaltplatform.api.model.api.response.InstitutionApiResponse.InstitutionApiResponseFactory;
import com.cobaltplatform.api.model.api.response.InstitutionBlurbApiResponse;
import com.cobaltplatform.api.model.api.response.InstitutionBlurbApiResponse.InstitutionBlurbApiResponseFactory;
import com.cobaltplatform.api.model.api.response.InstitutionLocationApiResponse;
import com.cobaltplatform.api.model.api.response.InstitutionLocationApiResponse.InstitutionLocationApiResponseFactory;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.AccountSource.AccountSourceId;
import com.cobaltplatform.api.model.db.Institution;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.InstitutionBlurb;
import com.cobaltplatform.api.model.db.InstitutionBlurbType.InstitutionBlurbTypeId;
import com.cobaltplatform.api.model.db.InstitutionTeamMember;
import com.cobaltplatform.api.model.db.UserExperienceType.UserExperienceTypeId;
import com.cobaltplatform.api.service.InstitutionService;
import com.cobaltplatform.api.service.MyChartService;
import com.lokalized.Strings;
import com.soklet.web.annotation.GET;
import com.soklet.web.annotation.PathParameter;
import com.soklet.web.annotation.QueryParameter;
import com.soklet.web.annotation.Resource;
import com.soklet.web.exception.AuthorizationException;
import com.soklet.web.response.ApiResponse;
import com.soklet.web.response.RedirectResponse;
import com.soklet.web.response.RedirectResponse.Type;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify LLC.
 */
@Resource
@Singleton
@ThreadSafe
public class InstitutionResource {
	@Nonnull
	private final InstitutionApiResponseFactory institutionApiResponseFactory;
	@Nonnull
	private final AccountSourceApiResponseFactory accountSourceApiResponseFactory;
	@Nonnull
	private final InstitutionBlurbApiResponseFactory institutionBlurbApiResponseFactory;
	@Nonnull
	private final EnterprisePluginProvider enterprisePluginProvider;
	@Nonnull
	private final InstitutionService institutionService;
	@Nonnull
	private final MyChartService myChartService;
	@Nonnull
	private final Provider<CurrentContext> currentContextProvider;
	@Nonnull
	private final Configuration configuration;
	@Nonnull
	private final Strings strings;
	@Nonnull
	private final InstitutionLocationApiResponseFactory institutionLocationApiResponseFactory;

	@Inject
	public InstitutionResource(@Nonnull InstitutionApiResponseFactory institutionApiResponseFactory,
														 @Nonnull AccountSourceApiResponseFactory accountSourceApiResponseFactory,
														 @Nonnull InstitutionBlurbApiResponseFactory institutionBlurbApiResponseFactory,
														 @Nonnull EnterprisePluginProvider enterprisePluginProvider,
														 @Nonnull InstitutionService institutionService,
														 @Nonnull MyChartService myChartService,
														 @Nonnull Provider<CurrentContext> currentContextProvider,
														 @Nonnull Configuration configuration,
														 @Nonnull Strings strings,
														 @Nonnull InstitutionLocationApiResponseFactory institutionLocationApiResponseFactory) {
		requireNonNull(institutionApiResponseFactory);
		requireNonNull(accountSourceApiResponseFactory);
		requireNonNull(institutionBlurbApiResponseFactory);
		requireNonNull(enterprisePluginProvider);
		requireNonNull(institutionService);
		requireNonNull(myChartService);
		requireNonNull(currentContextProvider);
		requireNonNull(configuration);
		requireNonNull(strings);
		requireNonNull(institutionLocationApiResponseFactory);

		this.institutionApiResponseFactory = institutionApiResponseFactory;
		this.accountSourceApiResponseFactory = accountSourceApiResponseFactory;
		this.institutionBlurbApiResponseFactory = institutionBlurbApiResponseFactory;
		this.enterprisePluginProvider = enterprisePluginProvider;
		this.institutionService = institutionService;
		this.myChartService = myChartService;
		this.currentContextProvider = currentContextProvider;
		this.configuration = configuration;
		this.strings = strings;
		this.institutionLocationApiResponseFactory = institutionLocationApiResponseFactory;
	}

	@GET("/institution/account-sources")
	public ApiResponse getAccountSources(@Nonnull @QueryParameter Optional<AccountSourceId> accountSourceId) {
		requireNonNull(accountSourceId);

		AccountSourceId requestAccountSourceId = accountSourceId.orElse(null);
		Institution institution = getInstitutionService().findInstitutionById(getCurrentContext().getInstitutionId()).get();
		List<AccountSourceApiResponse> accountSources = availableAccountSourcesForInstitutionId(institution.getInstitutionId(), requestAccountSourceId, getCurrentContext().getUserExperienceTypeId().orElse(null));

		return new ApiResponse(new HashMap<String, Object>() {{
			put("accountSources", accountSources);
		}});
	}

	@GET("/institution")
	public ApiResponse getInstitution(@Nonnull @QueryParameter Optional<AccountSourceId> accountSourceId) {
		requireNonNull(accountSourceId);

		AccountSourceId requestAccountSourceId = accountSourceId.orElse(null);
		Institution institution = getInstitutionService().findInstitutionById(getCurrentContext().getInstitutionId()).get();
		List<AccountSourceApiResponse> accountSources = availableAccountSourcesForInstitutionId(institution.getInstitutionId(), requestAccountSourceId, getCurrentContext().getUserExperienceTypeId().orElse(null));

		return new ApiResponse(new HashMap<String, Object>() {{
			put("institution", getInstitutionApiResponseFactory().create(institution, getCurrentContext()));
			put("accountSources", accountSources);
		}});
	}

	@Nonnull
	protected List<AccountSourceApiResponse> availableAccountSourcesForInstitutionId(@Nullable InstitutionId institutionId,
																																									 @Nullable AccountSourceId filterOnAccountSourceId,
																																									 @Nullable UserExperienceTypeId userExperienceTypeId) {
		if (institutionId == null)
			return List.of();

		List<AccountSourceApiResponse> accountSources = getInstitutionService().findAccountSourcesByInstitutionId(institutionId).stream()
				.filter(accountSource -> {
					if (userExperienceTypeId != null && filterOnAccountSourceId != null) {
						return (accountSource.getRequiresUserExperienceTypeId() == null || Objects.equals(accountSource.getRequiresUserExperienceTypeId(), userExperienceTypeId))
								&& Objects.equals(accountSource.getAccountSourceId(), filterOnAccountSourceId);
					} else if (userExperienceTypeId != null) {
						return accountSource.getRequiresUserExperienceTypeId() == null || Objects.equals(accountSource.getRequiresUserExperienceTypeId(), userExperienceTypeId);
					} else if (filterOnAccountSourceId != null) {
						return Objects.equals(accountSource.getAccountSourceId(), filterOnAccountSourceId);
					}

					return true;
				})
				.map(accountSource -> getAccountSourceApiResponseFactory().create(accountSource, getConfiguration().getEnvironment()))
				.collect(Collectors.toList());

		return accountSources;
	}

	@GET("/institution-blurbs")
	public ApiResponse getInstitutionBlurbs() {
		Institution institution = getInstitutionService().findInstitutionById(getCurrentContext().getInstitutionId()).get();
		Account account = getCurrentContext().getAccount().orElse(null);

		// If you're signed in, can't view other institution data
		if (account != null && account.getInstitutionId() != institution.getInstitutionId())
			throw new AuthorizationException();

		List<InstitutionBlurb> institutionBlurbs = getInstitutionService().findInstitutionBlurbsByInstitutionId(institution.getInstitutionId());
		Map<UUID, List<InstitutionTeamMember>> institutionTeamMembersByInstitutionBlurbId = getInstitutionService().findInstitutionTeamMembersByInstitutionBlurbIdForInstitutionId(institution.getInstitutionId());

		Map<InstitutionBlurbTypeId, InstitutionBlurbApiResponse> institutionBlurbsByInstitutionBlurbTypeId = new HashMap<>();
		for (InstitutionBlurb institutionBlurb : institutionBlurbs) {
			// If you're not signed in, can't see anything other than INTRO blurb (the only publicly-visible one)
			if (account == null && institutionBlurb.getInstitutionBlurbTypeId() != InstitutionBlurbTypeId.INTRO)
				continue;

			List<InstitutionTeamMember> institutionTeamMembers = institutionTeamMembersByInstitutionBlurbId.get(institutionBlurb.getInstitutionBlurbId());

			if (institutionTeamMembers == null)
				institutionTeamMembers = List.of();

			institutionBlurbsByInstitutionBlurbTypeId.put(institutionBlurb.getInstitutionBlurbTypeId(), getInstitutionBlurbApiResponseFactory().create(institutionBlurb, institutionTeamMembers));
		}

		return new ApiResponse(new HashMap<String, Object>() {{
			put("institutionBlurbsByInstitutionBlurbTypeId", institutionBlurbsByInstitutionBlurbTypeId);
		}});
	}

	@GET("/institutions/{institutionId}/mychart-authentication-url")
	public Object myChartAuthenticationUrl(@Nonnull @PathParameter InstitutionId institutionId,
																				 @Nonnull @QueryParameter Optional<Boolean> redirectImmediately) {
		requireNonNull(institutionId);
		requireNonNull(redirectImmediately);

		String authenticationUrl = getMyChartService().generateAuthenticationUrlForInstitutionId(institutionId);

		if (redirectImmediately.isPresent() && redirectImmediately.get())
			return new RedirectResponse(authenticationUrl, Type.TEMPORARY);

		return new ApiResponse(new HashMap<String, Object>() {{
			put("authenticationUrl", authenticationUrl);
		}});
	}

	@GET("/institutions/{institutionId}/microsoft-authentication-url")
	public Object microsoftAuthenticationUrl(@Nonnull @PathParameter InstitutionId institutionId,
																					 @Nonnull @QueryParameter Optional<Boolean> redirectImmediately) {
		requireNonNull(institutionId);
		requireNonNull(redirectImmediately);

		MicrosoftAuthenticator microsoftAuthenticator = getEnterprisePluginProvider().enterprisePluginForInstitutionId(institutionId).microsoftAuthenticator().get();

		// TODO: data-drive, use real state
		String authenticationUrl = microsoftAuthenticator.generateAuthenticationUrl(new AuthenticationRedirectRequest() {{
			setResponseType("code");
			setRedirectUri(format("%s/microsoft/oauth/callback", getConfiguration().getBaseUrl()));
			setResponseMode("query");
			setScope("profile openid email https://graph.microsoft.com/User.Read https://graph.microsoft.com/Calendars.Read");
			setState("12345");
		}});

		if (redirectImmediately.isPresent() && redirectImmediately.get())
			return new RedirectResponse(authenticationUrl, Type.TEMPORARY);

		return new ApiResponse(new HashMap<String, Object>() {{
			put("authenticationUrl", authenticationUrl);
		}});
	}

	@GET("/institution/locations")
	public ApiResponse geLocations() {
		Institution institution = getInstitutionService().findInstitutionById(getCurrentContext().getInstitutionId()).get();

		List<InstitutionLocationApiResponse> institutionLocations = getInstitutionService().findLocationsByInstitutionId(institution.getInstitutionId()).stream()
				.map(institutionLocation -> getInstitutionLocationApiResponseFactory().create(institutionLocation))
				.collect(Collectors.toList());

		return new ApiResponse(new HashMap<String, Object>() {{
			put("locations", institutionLocations);
		}});
	}

	@Nonnull
	protected InstitutionApiResponseFactory getInstitutionApiResponseFactory() {
		return this.institutionApiResponseFactory;
	}

	@Nonnull
	protected InstitutionBlurbApiResponseFactory getInstitutionBlurbApiResponseFactory() {
		return this.institutionBlurbApiResponseFactory;
	}

	@Nonnull
	protected InstitutionService getInstitutionService() {
		return this.institutionService;
	}

	@Nonnull
	protected MyChartService getMyChartService() {
		return this.myChartService;
	}

	@Nonnull
	protected CurrentContext getCurrentContext() {
		return this.currentContextProvider.get();
	}

	@Nonnull
	protected AccountSourceApiResponseFactory getAccountSourceApiResponseFactory() {
		return this.accountSourceApiResponseFactory;
	}

	@Nonnull
	protected InstitutionLocationApiResponseFactory getInstitutionLocationApiResponseFactory() {
		return this.institutionLocationApiResponseFactory;
	}

	@Nonnull
	protected EnterprisePluginProvider getEnterprisePluginProvider() {
		return this.enterprisePluginProvider;
	}

	@Nonnull
	protected Configuration getConfiguration() {
		return this.configuration;
	}

	@Nonnull
	protected Strings getStrings() {
		return this.strings;
	}


}
