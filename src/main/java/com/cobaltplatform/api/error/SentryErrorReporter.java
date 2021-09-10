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

package com.cobaltplatform.api.error;

import com.google.common.io.CharStreams;
import com.cobaltplatform.api.Configuration;
import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.model.db.Account;
import io.sentry.Breadcrumb;
import io.sentry.Hub;
import io.sentry.IHub;
import io.sentry.SentryEvent;
import io.sentry.SentryLevel;
import io.sentry.SentryOptions;
import io.sentry.protocol.Message;
import io.sentry.protocol.Request;
import io.sentry.protocol.User;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * Current Scope/Hub are stored in a ThreadLocal that is set up via {@link #startScope()} and torn down via {@link #endScope()}.
 *
 * @author Transmogrify, LLC.
 */
@Singleton
@ThreadSafe
public class SentryErrorReporter extends AbstractErrorReporter {
	@Nonnull
	private static final ThreadLocal<ErrorReporterScope> SCOPE_HOLDER;
	@Nonnull
	private static final ThreadLocal<IHub> HUB_HOLDER;
	@Nonnull
	private static final Set<String> IGNORED_HEADERS;

	@Nonnull
	private final Configuration configuration;
	@Nonnull
	private final IHub fallbackHub;

	static {
		SCOPE_HOLDER = new ThreadLocal<>();
		HUB_HOLDER = new ThreadLocal<>();
		IGNORED_HEADERS = Set.of("COOKIE");
	}

	public SentryErrorReporter(@Nonnull Configuration configuration) {
		super();

		requireNonNull(configuration);

		this.configuration = configuration;
		this.fallbackHub = createHub();
	}

	@Override
	public void startScope() {
		if (currentScope().isPresent()) {
			getLogger().warn("Error scope is already present, ignoring request to start");
			return;
		}

		IHub hub = createHub();
		hub.pushScope();

		getScopeHolder().set(new ErrorReporterScope());
		getHubHolder().set(hub);
	}

	@Override
	public void endScope() {
		if (!currentScope().isPresent()) {
			getLogger().warn("Error scope is not present, ignoring request to end");
			return;
		}

		IHub hub = getHubHolder().get();
		hub.popScope();

		getScopeHolder().remove();
		getHubHolder().remove();
	}

	@Nonnull
	@Override
	public Optional<ErrorReporterScope> currentScope() {
		return Optional.ofNullable(getScopeHolder().get());
	}

	@Override
	public void applyHttpServletRequest(@Nullable HttpServletRequest httpServletRequest) {
		if (currentScope().isEmpty()) {
			getLogger().warn("Invalid call to apply HttpServletRequest; no scope is defined. Ignoring...");
			return;
		}

		getScopeHolder().set(currentScope().get().copyWithHttpServletRequest(httpServletRequest));
	}

	@Override
	public void applyCurrentContext(@Nullable CurrentContext currentContext) {
		if (currentScope().isEmpty()) {
			getLogger().warn("Invalid call to apply current context; no scope is defined. Ignoring...");
			return;
		}

		Account account = currentContext == null ? null : currentContext.getAccount().orElse(null);
		IHub hub = getHubHolder().get();

		if (account != null) {
			User user = new User();
			user.setId(String.valueOf(account.getAccountId()));
			user.setEmail(account.getEmailAddress());
			hub.setUser(user);
		} else {
			hub.setUser(null);
		}

		getScopeHolder().set(currentScope().get().copyWithCurrentContext(currentContext));
	}

	@Nonnull
	protected IHub createHub() {
		// Hack for now, Sentry flags for debugging should be exposed as methods in Configuration object itself
		boolean production = "prod".equals(getConfiguration().getEnvironment());

		SentryOptions options = new SentryOptions();
		options.setDsn(getConfiguration().getSentryDsn());
		options.setRelease(getConfiguration().getGitCommitHash());
		options.setDebug(!production);
		options.setDiagnosticLevel(production ? SentryLevel.ERROR : SentryLevel.DEBUG);
		options.setEnvironment(getConfiguration().getEnvironment());
		options.setServerName(getConfiguration().getNodeIdentifier());

		return new Hub(options);
	}

	@Override
	public void addBreadcrumb(@Nonnull String message) {
		requireNonNull(message);

		if (currentScope().isEmpty()) {
			getLogger().warn("Invalid call to add breadcrumb '{}'; no scope is defined. Ignoring...", message);
			return;
		}

		getLogger().debug("Adding breadcrumb '{}' to Sentry...", message);

		Breadcrumb breadcrumb = new Breadcrumb();
		breadcrumb.setMessage(message);
		breadcrumb.setLevel(SentryLevel.INFO);

		getHubHolder().get().addBreadcrumb(breadcrumb);
	}

	@Override
	public void report(@Nonnull String message) {
		requireNonNull(message);

		getLogger().info("Reporting message '{}' to Sentry...", message);

		Message sentryMessage = new Message();
		sentryMessage.setMessage(message);

		SentryEvent sentryEvent = new SentryEvent();
		sentryEvent.setMessage(sentryMessage);
		sentryEvent.setLevel(SentryLevel.INFO);

		postprocessSentryEvent(sentryEvent);

		getCurrentHub().captureEvent(sentryEvent);
	}

	@Override
	protected void reportNormalizedThrowable(@Nonnull Throwable throwable) {
		requireNonNull(throwable);

		getLogger().error("Sending exception report to Sentry...");

		Message message = new Message();
		message.setMessage(throwable.getMessage() == null ? "Exception caught" : throwable.getMessage());

		SentryEvent sentryEvent = new SentryEvent();
		sentryEvent.setMessage(message);
		sentryEvent.setLevel(SentryLevel.ERROR);
		sentryEvent.setThrowable(throwable);

		postprocessSentryEvent(sentryEvent);

		getCurrentHub().captureEvent(sentryEvent);
	}

	protected void postprocessSentryEvent(@Nonnull SentryEvent sentryEvent) {
		requireNonNull(sentryEvent);

		ErrorReporterScope errorReporterScope = currentScope().orElse(null);

		if (errorReporterScope != null) {
			HttpServletRequest httpServletRequest = errorReporterScope.getHttpServletRequest().orElse(null);

			if (httpServletRequest != null) {
				Request sentryRequest = new Request();
				sentryRequest.setMethod(httpServletRequest.getMethod());
				sentryRequest.setQueryString(httpServletRequest.getQueryString());
				sentryRequest.setUrl(httpServletRequest.getRequestURL().toString());
				sentryRequest.setHeaders(resolveHeadersMap(httpServletRequest));

				String requestBody = null;

				try {
					requestBody = CharStreams.toString(httpServletRequest.getReader());
				} catch (IOException e) {
					getLogger().warn("Unable to extract request body", e);
				}

				sentryRequest.setData(requestBody);

				sentryEvent.setRequest(sentryRequest);
			}
		}
	}

	@Nonnull
	protected Map<String, String> resolveHeadersMap(@Nonnull HttpServletRequest request) {
		requireNonNull(request);

		Map<String, String> headersMap = new HashMap<>();

		for (String headerName : Collections.list(request.getHeaderNames()))
			if (!getIgnoredHeaders().contains(headerName.toUpperCase(Locale.US)))
				headersMap.put(headerName, toString(request.getHeaders(headerName)).orElse(null));

		return headersMap;
	}

	@Nonnull
	protected Optional<String> toString(@Nullable Enumeration<String> enumeration) {
		return Optional.ofNullable(enumeration != null ? String.join(",", Collections.list(enumeration)) : null);
	}

	@Nonnull
	protected IHub getCurrentHub() {
		IHub threadLocalHub = getHubHolder().get();
		return threadLocalHub == null ? getFallbackHub() : threadLocalHub;
	}

	@Nonnull
	protected Set<String> getIgnoredHeaders() {
		return IGNORED_HEADERS;
	}

	@Nonnull
	protected Configuration getConfiguration() {
		return configuration;
	}

	@Nonnull
	protected IHub getFallbackHub() {
		return fallbackHub;
	}

	@Nonnull
	protected ThreadLocal<ErrorReporterScope> getScopeHolder() {
		return SCOPE_HOLDER;
	}

	@Nonnull
	protected ThreadLocal<IHub> getHubHolder() {
		return HUB_HOLDER;
	}
}
