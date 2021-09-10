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

import com.lokalized.Strings;
import com.cobaltplatform.api.Configuration;
import com.cobaltplatform.api.util.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify LLC.
 */
@Singleton
@ThreadSafe
public class MaintenanceFilter implements Filter {
	@Nonnull
	private final Configuration configuration;
	@Nonnull
	private final Strings strings;
	@Nonnull
	private final Logger logger;

	@Inject
	public MaintenanceFilter(@Nonnull Configuration configuration,
													 @Nonnull Strings strings) {
		requireNonNull(configuration);
		requireNonNull(strings);

		this.configuration = configuration;
		this.strings = strings;
		this.logger = LoggerFactory.getLogger(getClass());
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
		boolean systemRequest = httpServletRequest.getRequestURI().startsWith("/system/");
		boolean optionsRequest = Objects.equals("OPTIONS", httpServletRequest.getMethod());
		boolean downForMaintenance = getConfiguration().getDownForMaintenance();

		// Don't apply to certain requests
		if (staticFile || systemRequest || optionsRequest || !downForMaintenance) {
			filterChain.doFilter(servletRequest, servletResponse);
			return;
		}

		String maintenanceMessage = getStrings().get("We're sorry for the inconvenience, but Cobalt is temporarily offline for maintenance and will be back online shortly.\n" +
				"If you need immediate help, please contact one of the following numbers:\n" +
				"Call 911 (24/7 emergency)\n" +
				"Call 800-273-8255 (24/7 National suicide prevention line)\n" +
				"Text 741 741 (24/7 Crisis Text Line)\n" +
				"...or go to your nearest emergency department or crisis center."
		);

		ValidationException validationException = new ValidationException(maintenanceMessage);
		validationException.setMetadata(Map.of("downForMaintenance", true));

		throw validationException;
	}

	@Override
	public void destroy() {
		// Nothing for now
	}

	@Nonnull
	protected Configuration getConfiguration() {
		return configuration;
	}

	@Nonnull
	protected Strings getStrings() {
		return strings;
	}

	@Nonnull
	public Logger getLogger() {
		return logger;
	}
}