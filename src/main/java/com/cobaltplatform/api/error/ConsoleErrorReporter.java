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
public class ConsoleErrorReporter extends AbstractErrorReporter {
	@Nonnull
	private static final ThreadLocal<ErrorReporterScope> SCOPE_HOLDER;

	static {
		SCOPE_HOLDER = new ThreadLocal<>();
	}

	@Override
	public void startScope() {
		if (currentScope().isPresent()) {
			getLogger().warn("Error scope is already present, ignoring request to start");
			return;
		}

		getScopeHolder().set(new ErrorReporterScope());
	}

	@Override
	public void endScope() {
		if (!currentScope().isPresent()) {
			getLogger().warn("Error scope is not present, ignoring request to end");
			return;
		}

		getScopeHolder().remove();
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

		// No-op
	}

	@Override
	public void applyCurrentContext(@Nullable CurrentContext currentContext) {
		if (currentScope().isEmpty()) {
			getLogger().warn("Invalid call to apply current context; no scope is defined. Ignoring...");
			return;
		}

		// No-op
	}

	@Override
	public void addBreadcrumb(@Nonnull String message) {
		if (!currentScope().isPresent()) {
			getLogger().warn("Scope is not present, ignoring request to add breadcrumb '{");
			return;
		}

		getLogger().debug("Fake-adding error breadcrumb '{}'", message);
	}

	@Override
	public void report(@Nonnull String message) {
		getLogger().debug("Fake-sending error report for message '{}'", message);
	}

	@Override
	protected void reportNormalizedThrowable(@Nonnull Throwable throwable) {
		// Nothing to do
	}

	@Nonnull
	protected ThreadLocal<ErrorReporterScope> getScopeHolder() {
		return SCOPE_HOLDER;
	}
}