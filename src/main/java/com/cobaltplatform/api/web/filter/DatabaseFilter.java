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

import com.cobaltplatform.api.context.DatabaseContext;
import com.cobaltplatform.api.context.DatabaseContextExecutor;
import com.cobaltplatform.api.service.SystemService;
import com.cobaltplatform.api.util.db.DatabaseProvider;
import com.cobaltplatform.api.util.db.ReadReplica;
import com.cobaltplatform.api.util.db.RequiresManualTransactionManagement;
import com.pyranid.Database;
import com.pyranid.StatementLog;
import com.soklet.web.request.RequestContext;
import com.soklet.web.routing.Route;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify LLC.
 */
@Singleton
@ThreadSafe
public class DatabaseFilter implements Filter {
	@Nonnull
	private static final Pattern WHITESPACE_PATTERN;
	@Nonnull
	private Provider<SystemService> systemServiceProvider;
	@Nonnull
	private final DatabaseProvider databaseProvider;
	@Nonnull
	private final DatabaseContextExecutor databaseContextExecutor;
	@Nonnull
	private final Logger logger;

	@Inject
	public DatabaseFilter(@Nonnull Provider<SystemService> systemServiceProvider,
												@Nonnull DatabaseProvider databaseProvider,
												@Nonnull DatabaseContextExecutor databaseContextExecutor) {
		requireNonNull(systemServiceProvider);
		requireNonNull(databaseProvider);
		requireNonNull(databaseContextExecutor);

		this.systemServiceProvider = systemServiceProvider;
		this.databaseProvider = databaseProvider;
		this.databaseContextExecutor = databaseContextExecutor;
		this.logger = LoggerFactory.getLogger("com.cobaltplatform.api.sql.REQUEST_SQL");
	}

	static {
		WHITESPACE_PATTERN = Pattern.compile("\\s+");
	}

	@Override
	public void init(@Nonnull FilterConfig filterConfig) throws ServletException {
		requireNonNull(filterConfig);
		// Nothing for now
	}

	@Override
	public void doFilter(@Nonnull ServletRequest servletRequest,
											 @Nonnull ServletResponse servletResponse,
											 @Nonnull FilterChain filterChain) throws IOException, ServletException {
		requireNonNull(servletRequest);
		requireNonNull(servletResponse);
		requireNonNull(filterChain);

		HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
		boolean staticFile = httpServletRequest.getRequestURI().startsWith("/static/");
		boolean optionsRequest = Objects.equals("OPTIONS", httpServletRequest.getMethod());
		boolean performingAutoRefresh = Objects.equals(httpServletRequest.getHeader("X-Cobalt-Autorefresh"), "true");

		// Don't apply to some requests
		if (staticFile || optionsRequest || performingAutoRefresh) {
			filterChain.doFilter(servletRequest, servletResponse);
			return;
		}

		RequestContext requestContext = null;

		try {
			requestContext = RequestContext.get();
		} catch (Throwable ignored) {
			// Nothing to do, continue on
		}

		// If a Resource Method has either @ReadReplica or @RequiresManualTransactionManagement applied, don't wrap this request in a transaction
		if (requestContext != null) {
			Route route = requestContext.route().orElse(null);
			Method resourceMethod = route != null && route.resourceMethod() != null ? route.resourceMethod() : null;

			if (resourceMethod != null) {
				boolean readReplica = resourceMethod.getAnnotation(ReadReplica.class) != null;
				boolean requiresManualTransactionManagement = resourceMethod.getAnnotation(RequiresManualTransactionManagement.class) != null;

				if (readReplica || requiresManualTransactionManagement) {
					filterChain.doFilter(servletRequest, servletResponse);
					return;
				}
			}
		}

		DatabaseContext databaseContext = new DatabaseContext();

		try {
			getDatabase().transaction(() -> {
				// This transaction wraps our HTTP resource methods (those annotated with @GET, @POST, etc.)
				// We already know the current account (if one has been authenticated) at this point.
				// Apply the current context (account, resource method, etc.) to the current transaction for automated DB footprint capture
				getSystemService().applyFootprintForCurrentContextToCurrentTransaction();

				getDatabaseContextExecutor().execute(databaseContext, () -> {
					filterChain.doFilter(servletRequest, servletResponse);
				});
			});
		} finally {
			Long totalTime = 0L;
			List<StatementLog> originalStatementLogs = databaseContext.getStatementLogs();
			List<StatementLog> sortedStatementLogs = new ArrayList<>(originalStatementLogs);
			List<String> displayableStatementLogs = new ArrayList<>(sortedStatementLogs.size());

//			Collections.sort(sortedStatementLogs, (statementLog1, statementLog2) -> {
//				return statementLog2.totalTime().compareTo(statementLog1.totalTime());
//			});

			for (StatementLog statementLog : sortedStatementLogs) {
				String normalizedSql = normalizeSql(statementLog.sql());
				String displayableStatementLog = format("%.1fms: %s", (statementLog.totalTime() / (double) 1000000), normalizedSql);

				if (statementLog.parameters() != null && statementLog.parameters().size() > 0)
					displayableStatementLog += " " + statementLog.parameters();

				displayableStatementLogs.add(displayableStatementLog);
				totalTime += statementLog.totalTime();
			}

			if (displayableStatementLogs.size() > 0) {
				String queryText = displayableStatementLogs.size() == 1 ? "statement" : "statements";
				getLogger().debug("SQL statements for this request:\n{}\nExecuted {} {} in {}ms.", displayableStatementLogs.stream().collect(Collectors.joining("\n")),
						sortedStatementLogs.size(), queryText, (int) (totalTime / (double) 1000000));
			}
		}
	}

	@Override
	public void destroy() {
		// Nothing for now
	}

	@Nonnull
	protected SystemService getSystemService() {
		return this.systemServiceProvider.get();
	}

	@Nonnull
	protected Database getDatabase() {
		return this.databaseProvider.get();
	}

	@Nonnull
	protected DatabaseContextExecutor getDatabaseContextExecutor() {
		return this.databaseContextExecutor;
	}

	@Nonnull
	public Logger getLogger() {
		return this.logger;
	}

	@Nonnull
	protected String normalizeSql(@Nonnull String sql) {
		requireNonNull(sql);
		return WHITESPACE_PATTERN.matcher(sql.replace('\n', ' ').replace('\r', ' ')).replaceAll(" ").trim();
	}
}
