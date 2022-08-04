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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class JavascriptExecutionException extends Exception {
	@Nonnull
	private final Map<String, Object> input;
	@Nonnull
	private final String sourceJavascript;
	@Nonnull
	private final String executedJavascript;

	public JavascriptExecutionException(@Nonnull Map<String, Object> input,
																			@Nonnull String sourceJavascript,
																			@Nonnull String executedJavascript) {
		this(null, null, input, sourceJavascript, executedJavascript);
	}

	public JavascriptExecutionException(@Nullable Throwable cause,
																			@Nonnull Map<String, Object> input,
																			@Nonnull String sourceJavascript,
																			@Nonnull String executedJavascript) {
		this(null, cause, input, sourceJavascript, executedJavascript);
	}

	public JavascriptExecutionException(@Nullable String message,
																			@Nullable Throwable cause,
																			@Nonnull Map<String, Object> input,
																			@Nonnull String sourceJavascript,
																			@Nonnull String executedJavascript) {
		super(message, cause);

		requireNonNull(input);
		requireNonNull(sourceJavascript);
		requireNonNull(executedJavascript);

		this.input = input;
		this.sourceJavascript = sourceJavascript;
		this.executedJavascript = executedJavascript;
	}

	@Nonnull
	public Map<String, Object> getInput() {
		return this.input;
	}

	@Nonnull
	public String getSourceJavascript() {
		return this.sourceJavascript;
	}

	@Nonnull
	public String getExecutedJavascript() {
		return this.executedJavascript;
	}
}