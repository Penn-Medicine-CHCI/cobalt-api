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

package com.cobaltplatform.api.util;

import com.cobaltplatform.api.Configuration;
import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.model.db.ClientDeviceType.ClientDeviceTypeId;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.InteractionInstance;
import com.cobaltplatform.api.model.db.InteractionOption;
import com.cobaltplatform.api.model.db.LoginDestination.LoginDestinationId;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
@Singleton
public class LinkGenerator {
	@Nonnull
	private final Configuration configuration;
	@Nonnull
	private final Provider<CurrentContext> currentContextProvider;

	@Inject
	public LinkGenerator(@Nonnull Configuration configuration,
											 @Nonnull Provider<CurrentContext> currentContextProvider) {
		requireNonNull(configuration);
		requireNonNull(configuration);

		this.configuration = configuration;
		this.currentContextProvider = currentContextProvider;
	}

	@Nonnull
	public String generateAuthenticationLink(@Nonnull InstitutionId institutionId,
																					 @Nonnull String accessToken) {
		return generateAuthenticationLink(institutionId, determineCurrentClientDeviceTypeId(), accessToken);
	}

	@Nonnull
	public String generateAuthenticationLink(@Nonnull String baseUrl,
																					 @Nonnull String accessToken) {
		requireNonNull(baseUrl);
		requireNonNull(accessToken);

		return constructUrl(baseUrl, "auth", new HashMap<String, Object>() {{
			put("accessToken", accessToken);
		}});
	}

	@Nonnull
	public String generateAuthenticationLink(@Nonnull InstitutionId institutionId,
																					 @Nonnull ClientDeviceTypeId clientDeviceTypeId,
																					 @Nonnull String accessToken) {
		requireNonNull(institutionId);
		requireNonNull(clientDeviceTypeId);
		requireNonNull(accessToken);

		return generateAuthenticationLink(institutionId, LoginDestinationId.COBALT_PATIENT, clientDeviceTypeId, accessToken);
	}

	@Nonnull
	public String generateAuthenticationLink(@Nonnull InstitutionId institutionId,
																					 @Nonnull LoginDestinationId loginDestinationId,
																					 @Nonnull ClientDeviceTypeId clientDeviceTypeId,
																					 @Nonnull String accessToken) {
		requireNonNull(institutionId);
		requireNonNull(loginDestinationId);
		requireNonNull(clientDeviceTypeId);
		requireNonNull(accessToken);

		String baseUrl = determineBaseUrl(institutionId, clientDeviceTypeId);
		String urlPath = "auth";

		if (loginDestinationId == LoginDestinationId.IC_PANEL)
			baseUrl = getConfiguration().getIcWebappBaseUrl();

		return constructUrl(baseUrl, urlPath, new HashMap<String, Object>() {{
			put("accessToken", accessToken);
		}});
	}

	@Nonnull
	public String generateAccountInviteLink(@Nonnull InstitutionId institutionId,
																					@Nonnull ClientDeviceTypeId clientDeviceTypeId,
																					@Nonnull UUID accountInviteCode) {
		requireNonNull(institutionId);
		requireNonNull(clientDeviceTypeId);
		requireNonNull(accountInviteCode);

		return constructUrl(determineBaseUrl(institutionId, clientDeviceTypeId),
				format("accounts/claim-invite/%s", accountInviteCode), new HashMap<String, Object>());
	}

	@Nonnull
	public String generatePasswordResetLink(@Nonnull InstitutionId institutionId,
																					@Nonnull ClientDeviceTypeId clientDeviceTypeId,
																					@Nonnull UUID passwordResetToken) {
		requireNonNull(institutionId);
		requireNonNull(clientDeviceTypeId);
		requireNonNull(passwordResetToken);

		return constructUrl(determineBaseUrl(institutionId, clientDeviceTypeId),
				format("accounts/reset-password/%s", passwordResetToken), new HashMap<String, Object>());
	}

	@Nonnull
	public String generateCmsMyContentLink(@Nonnull InstitutionId institutionId) {
		return constructUrl(determineBaseUrl(institutionId, ClientDeviceTypeId.WEB_BROWSER),
				"cms/on-your-time/");
	}

	@Nonnull
	public String generateGroupSessionsAdminListLink(@Nonnull InstitutionId institutionId) {
		return constructUrl(determineBaseUrl(institutionId, ClientDeviceTypeId.WEB_BROWSER),
				"group-sessions/scheduled");
	}

	@Nonnull
	public String generateInteractionOptionLink(@Nonnull InstitutionId institutionId,
																							@Nonnull InteractionOption interactionOption,
																							@Nonnull InteractionInstance interactionInstance) {
		return constructUrl(determineBaseUrl(institutionId, ClientDeviceTypeId.WEB_BROWSER),
				format("interaction/%s/option/%s", interactionInstance.getInteractionInstanceId(), interactionOption.getInteractionOptionId()),
				new HashMap<String, Object>());
	}

	@Nonnull
	protected String determineBaseUrl(@Nonnull InstitutionId institutionId,
																		@Nonnull ClientDeviceTypeId clientDeviceTypeId) {
		requireNonNull(institutionId);
		requireNonNull(clientDeviceTypeId);

		if (clientDeviceTypeId == ClientDeviceTypeId.WEB_BROWSER) {
			String contextWebappBaseUrl = null;

			try {
				contextWebappBaseUrl = getCurrentContext().getWebappBaseUrl().orElse(null);
			} catch (Exception ignored) {
				// If we're not in a current context, it's OK
			}

			return (contextWebappBaseUrl == null ? getConfiguration().getWebappBaseUrl(institutionId) : contextWebappBaseUrl) + "/";
		}

		throw new IllegalStateException(format("Unexpected %s value %s encountered", ClientDeviceTypeId.class.getSimpleName(), clientDeviceTypeId.name()));
	}

	@Nonnull
	protected String constructUrl(@Nonnull String baseUrl,
																@Nonnull String urlPath) {
		requireNonNull(baseUrl);
		requireNonNull(urlPath);

		return constructUrl(baseUrl, urlPath, Collections.emptyMap());
	}

	@Nonnull
	protected String constructUrl(@Nonnull String baseUrl,
																@Nonnull String urlPath,
																@Nonnull Map<String, Object> queryParameters) {
		requireNonNull(baseUrl);
		requireNonNull(urlPath);
		requireNonNull(queryParameters);

		String url = baseUrl.trim();

		if (!url.endsWith("/"))
			url += "/";

		url += urlPath;

		if (queryParameters.size() > 0) {
			url += "?";

			List<String> queryParameterEntries = new ArrayList<>(queryParameters.size());

			for (Entry<String, Object> entry : queryParameters.entrySet()) {
				String name = trimToNull(entry.getKey());

				if (name == null)
					continue;

				String value = entry.getValue() == null ? null : trimToNull(entry.getValue().toString());

				if (value == null)
					value = "";

				queryParameterEntries.add(format("%s=%s", name, WebUtility.urlEncode(value)));
			}

			url += queryParameterEntries.stream().collect(Collectors.joining("&"));
		}

		return url;
	}

	@Nonnull
	protected ClientDeviceTypeId determineCurrentClientDeviceTypeId() {
		ClientDeviceTypeId clientDeviceTypeId = getCurrentContext().getRemoteClient().get().getTypeId().orElse(null);
		return clientDeviceTypeId == null ? ClientDeviceTypeId.WEB_BROWSER : clientDeviceTypeId;
	}

	@Nonnull
	protected Configuration getConfiguration() {
		return configuration;
	}

	@Nonnull
	protected CurrentContext getCurrentContext() {
		return currentContextProvider.get();
	}
}
