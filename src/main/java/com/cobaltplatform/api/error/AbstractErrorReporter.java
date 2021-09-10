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

import com.cobaltplatform.api.util.ValidationException;
import com.soklet.web.exception.AuthenticationException;
import com.soklet.web.exception.AuthorizationException;
import com.soklet.web.exception.NotFoundException;
import com.soklet.web.exception.ResourceMethodExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public abstract class AbstractErrorReporter implements ErrorReporter {
	@Nonnull
	private final Logger logger;

	public AbstractErrorReporter() {
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Override
	public final void report(@Nonnull Throwable throwable) {
		requireNonNull(throwable);

		Throwable normalizedThrowable = normalizeThrowable(throwable);
		ErrorReportingStrategy errorReportingStrategy = errorReportingStrategyForThrowable(normalizedThrowable);

		if (errorReportingStrategy == ErrorReportingStrategy.LOG_MINIMAL) {
			if (normalizedThrowable.getMessage() != null)
				getLogger().error("Exception occurred. {}: {}", normalizedThrowable.getClass().getSimpleName(), normalizedThrowable.getMessage());
			else
				getLogger().error("Exception occurred: {}", normalizedThrowable.getClass().getSimpleName());
		} else if (errorReportingStrategy == ErrorReportingStrategy.LOG_FULL) {
			getLogger().error("Exception occurred", normalizedThrowable);
		} else if (errorReportingStrategy == ErrorReportingStrategy.REPORT_AND_LOG_FULL) {
			getLogger().error("Exception occurred", normalizedThrowable);
			reportNormalizedThrowable(normalizedThrowable);
		} else {
			throw new UnsupportedOperationException(format("Unsupported %s value '%s'", ErrorReportingStrategy.class.getSimpleName(), errorReportingStrategy.name()));
		}
	}

	protected abstract void reportNormalizedThrowable(@Nonnull Throwable throwable);

	@Nonnull
	protected Throwable normalizeThrowable(@Nonnull Throwable throwable) {
		requireNonNull(throwable);

		Throwable normalizedThrowable = throwable;

		if (throwable.getCause() != null) {
			if (throwable instanceof ResourceMethodExecutionException) {
				normalizedThrowable = throwable.getCause();
			} // Unwrap other types here if needed
		}

		return normalizedThrowable;
	}

	@Nonnull
	protected ErrorReportingStrategy errorReportingStrategyForThrowable(@Nonnull Throwable throwable) {
		requireNonNull(throwable);

		if (throwable instanceof ValidationException
				|| throwable instanceof NotFoundException
				|| throwable instanceof AuthenticationException
				|| throwable instanceof AuthorizationException)
			return ErrorReportingStrategy.LOG_MINIMAL;

		return ErrorReportingStrategy.REPORT_AND_LOG_FULL;
	}

	@Nonnull
	protected Logger getLogger() {
		return logger;
	}
}
