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

import com.cobaltplatform.api.context.CurrentContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class ErrorReporterScope {
	@Nullable
	private final CurrentContext currentContext;
	@Nullable
	private final HttpServletRequest httpServletRequest;

	public ErrorReporterScope() {
		this(null, null);
	}

	public ErrorReporterScope(@Nullable CurrentContext currentContext) {
		this(currentContext, null);
	}

	public ErrorReporterScope(@Nullable HttpServletRequest httpServletRequest) {
		this(null, httpServletRequest);
	}

	public ErrorReporterScope(@Nullable CurrentContext currentContext,
														@Nullable HttpServletRequest httpServletRequest) {
		this.currentContext = currentContext;
		this.httpServletRequest = httpServletRequest;
	}

	@Nonnull
	public ErrorReporterScope copyWithCurrentContext(@Nullable CurrentContext currentContext) {
		return new ErrorReporterScope(currentContext, getHttpServletRequest().orElse(null));
	}

	@Nonnull
	public ErrorReporterScope copyWithHttpServletRequest(@Nullable HttpServletRequest httpServletRequest) {
		return new ErrorReporterScope(getCurrentContext().orElse(null), httpServletRequest);
	}

	@Nonnull
	public Optional<CurrentContext> getCurrentContext() {
		return Optional.ofNullable(currentContext);
	}

	@Nullable
	public Optional<HttpServletRequest> getHttpServletRequest() {
		return Optional.ofNullable(httpServletRequest);
	}
}
