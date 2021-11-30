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

package com.cobaltplatform.api.web.filter;

import com.cobaltplatform.api.Configuration;
import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.AccountSource;
import com.cobaltplatform.api.model.db.Role.RoleId;
import com.cobaltplatform.api.model.security.AccessTokenStatus;
import com.cobaltplatform.api.model.security.AuthenticationRequired;
import com.cobaltplatform.api.model.security.ContentSecurityLevel;
import com.cobaltplatform.api.model.security.IcSignedRequestRequired;
import com.cobaltplatform.api.util.AccessTokenException;
import com.soklet.web.exception.AuthenticationException;
import com.soklet.web.exception.AuthorizationException;
import com.soklet.web.request.RequestContext;
import com.soklet.web.routing.Route;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Singleton
@ThreadSafe
public class AuthorizationFilter implements Filter {
	@Nonnull
	private final Provider<CurrentContext> currentContextProvider;
	@Nonnull
	private final Provider<RequestContext> requestContextProvider;
	@Nonnull
	private final Logger logger;
	@NonNull
	private final Provider<Configuration> configurationProvider;

	@Inject
	public AuthorizationFilter(@Nonnull Provider<CurrentContext> currentContextProvider,
														 @Nonnull Provider<RequestContext> requestContextProvider,
														 @Nonnull Provider<Configuration> configurationProvider) {
		requireNonNull(currentContextProvider);
		requireNonNull(requestContextProvider);
		requireNonNull(configurationProvider);

		this.currentContextProvider = currentContextProvider;
		this.requestContextProvider = requestContextProvider;
		this.logger = LoggerFactory.getLogger(getClass());
		this.configurationProvider = configurationProvider;
	}

	@Override
	public void init(@Nonnull FilterConfig filterConfig) throws ServletException {
		requireNonNull(filterConfig);
		// Nothing for now
	}

	@Override
	public void doFilter(@Nonnull ServletRequest servletRequest, @Nonnull ServletResponse servletResponse,
											 @Nonnull FilterChain filterChain) throws IOException, ServletException {
		requireNonNull(servletRequest);
		requireNonNull(servletResponse);
		requireNonNull(filterChain);

		HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
		boolean staticFile = httpServletRequest.getRequestURI().startsWith("/static/");
		boolean optionsRequest = Objects.equals("OPTIONS", httpServletRequest.getMethod());

		// Don't apply to some requests
		if (staticFile || optionsRequest) {
			filterChain.doFilter(servletRequest, servletResponse);
			return;
		}

		Optional<Route> route = getRequestContext().route();

		if (route.isPresent()) {
			Method resourceMethod = route.get().resourceMethod();
			AuthenticationRequired authenticationRequired = resourceMethod.getAnnotation(AuthenticationRequired.class);

			if (authenticationRequired != null) {
				Account account = getCurrentContext().getAccount().orElse(null);

				if (account == null)
					throw new AuthenticationException(format("Authentication failed. Resource method %s requires you to be authenticated.", resourceMethod));

				if (authenticationRequired.value() != null && authenticationRequired.value().length > 0) {
					List<RoleId> roleIds = Arrays.asList(authenticationRequired.value());

					if (!roleIds.contains(account.getRoleId()))
						throw new AuthorizationException(format("Authorization failed. Resource method %s requires one of the following roles: %s, but current user has %s",
								resourceMethod, roleIds, account.getRoleId().name()));
				}

				AccessTokenStatus accessTokenStatus = getCurrentContext().getAccessTokenStatus().orElse(null);

				if ((authenticationRequired.contentSecurityLevel() == ContentSecurityLevel.HIGH && accessTokenStatus != AccessTokenStatus.FULLY_ACTIVE) ||
						accessTokenStatus == AccessTokenStatus.FULLY_EXPIRED) {

					String signOnUrl = getConfiguration().getEnvironment().equals("prod") ?
							getCurrentContext().getAccountSource().getProdSsoUrl() : getConfiguration().getEnvironment().equals("dev") ?
							getCurrentContext().getAccountSource().getDevSsoUrl() : getCurrentContext().getAccountSource().getLocalSsoUrl();
					String contentSecurityLevel = null;

					if (authenticationRequired.contentSecurityLevel() == ContentSecurityLevel.HIGH)
						contentSecurityLevel = ContentSecurityLevel.HIGH.name();
					else
						contentSecurityLevel = ContentSecurityLevel.LOW.name();

					throw new AccessTokenException(format("Authentication failed. Resource method %s requires %s.%s but your " +
									"access token is not fully active (status %s)", resourceMethod,
							ContentSecurityLevel.class.getSimpleName(), contentSecurityLevel,
							(accessTokenStatus == null ? "unknown" : accessTokenStatus.name())),
							accessTokenStatus == null ? AccessTokenStatus.FULLY_EXPIRED : accessTokenStatus, signOnUrl);
				}

			}

			if (resourceMethod.getAnnotation(IcSignedRequestRequired.class) != null) {
				if (!getCurrentContext().getSignedByIc())
					throw new AuthenticationException(format("Authentication failed. Request to resource method %s must be signed by IC.", resourceMethod));
			}
		}

		filterChain.doFilter(servletRequest, servletResponse);
	}

	@Override
	public void destroy() {
		// Nothing for now
	}

	@Nonnull
	protected CurrentContext getCurrentContext() {
		return currentContextProvider.get();
	}

	@Nonnull
	protected RequestContext getRequestContext() {
		return requestContextProvider.get();
	}

	@Nonnull
	protected Logger getLogger() {
		return logger;
	}

	@Nonnull
	protected Configuration getConfiguration() {
		return configurationProvider.get();
	}

}
