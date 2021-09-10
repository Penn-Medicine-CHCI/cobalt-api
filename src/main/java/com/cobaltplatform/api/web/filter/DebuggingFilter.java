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

import com.cobaltplatform.api.util.ValidationUtility;
import com.cobaltplatform.api.util.WebUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Singleton;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Random;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Singleton
@ThreadSafe
public class DebuggingFilter implements Filter {
	@Nonnull
	private static final String SIMULATE_DELAY_REQUEST_PROPERTY_NAME = "X-Simulate-Delay";
	@Nonnull
	private static final String RANDOM_ERROR_PROBABILITY_REQUEST_PROPERTY_NAME = "X-Random-Error-Probability";

	@Nonnull
	private final Logger logger;

	public DebuggingFilter() {
		this.logger = LoggerFactory.getLogger(getClass());
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

		String simulateDelayValue = WebUtility.extractValueFromRequest(httpServletRequest, SIMULATE_DELAY_REQUEST_PROPERTY_NAME).orElse(null);

		if (ValidationUtility.isValidInteger(simulateDelayValue)) {
			long simulateDelay = Long.parseLong(simulateDelayValue);

			// Enforce bounds
			if (simulateDelay < 0)
				simulateDelay = 0;
			else if (simulateDelay > 60_000)
				simulateDelay = 60_000;

			if (simulateDelay > 0) {
				try {
					getLogger().debug("Simulating {}ms delay...", simulateDelay);
					Thread.sleep(simulateDelay);
					getLogger().debug("Simulated delay finished.");
				} catch (InterruptedException e) {
					getLogger().warn("Simulated delay was interrupted", e);
				}
			}
		}

		String randomErrorProbabilityValue = WebUtility.extractValueFromRequest(httpServletRequest, RANDOM_ERROR_PROBABILITY_REQUEST_PROPERTY_NAME).orElse(null);

		if (ValidationUtility.isValidDouble(randomErrorProbabilityValue)) {
			double probability = Double.valueOf(randomErrorProbabilityValue);

			// Enforce bounds
			if (probability < 0)
				probability = 0;
			else if (probability > 1)
				probability = 1;

			boolean throwError = new Random().nextDouble() <= probability;

			if (throwError)
				throw new RuntimeException("A simulated error occurred.");
		}

		filterChain.doFilter(servletRequest, servletResponse);
	}

	@Override
	public void destroy() {
		// Nothing for now
	}

	@Nonnull
	protected Logger getLogger() {
		return logger;
	}
}