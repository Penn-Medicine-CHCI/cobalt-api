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
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public final class ProcessUtility {
	@Nonnull
	private static final Boolean RUNNING_ON_WINDOWS;

	static {
		RUNNING_ON_WINDOWS = System.getProperty("os.name").toLowerCase().startsWith("windows");
	}

	private ProcessUtility() {
		// Non-instantiable
	}

	/**
	 * Runs a process.
	 *
	 * @param command e.g. {@code "git rev-parse HEAD"}
	 * @return the result of the process execution
	 * @throws ProcessException if an error occurs
	 */
	@Nonnull
	public static ProcessResult run(@Nonnull String command) {
		return run(command, (processBuilder -> {
			// Do nothing
		}));
	}

	/**
	 * Runs a process.
	 *
	 * @param command                  e.g. {@code "git rev-parse HEAD"}
	 * @param processBuilderConfigurer permits customization of the {@link ProcessBuilder}
	 * @return the result of the process execution
	 * @throws ProcessException if an error occurs
	 */
	@Nonnull
	public static ProcessResult run(@Nonnull String command,
																	@Nonnull Consumer<ProcessBuilder> processBuilderConfigurer) {
		requireNonNull(command);
		requireNonNull(processBuilderConfigurer);

		ProcessBuilder processBuilder = new ProcessBuilder();

		if (getRunningOnWindows())
			processBuilder.command("cmd.exe", "/c", command);
		else
			processBuilder.command("sh", "-c", command);

		processBuilder.directory(new File("."));
		processBuilder.redirectErrorStream(true);

		// Allow callers to customize if needed
		processBuilderConfigurer.accept(processBuilder);

		try {
			Process process = processBuilder.start();

			try (InputStream inputStream = process.getInputStream()) {
				CompletableFuture<TextConsumerResult> textConsumerFuture = new CompletableFuture<>();
				TextConsumer textConsumer = new TextConsumer(inputStream, textConsumerFuture);

				ExecutorService executorService = Executors.newSingleThreadExecutor();
				executorService.submit(textConsumer);

				int exitCode = process.waitFor();

				TextConsumerResult textConsumerResult = textConsumerFuture.join();
				ExecutorServiceUtility.shutdownAndAwaitTermination(executorService);

				Exception textException = textConsumerResult.getException().orElse(null);

				if (textException != null)
					throw new ProcessException(format("Unable to read stdout/stderr from process '%s'", command), textException);

				return new ProcessResult(exitCode, textConsumerResult.getText().orElse(null));
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new ProcessException(format("Process '%s' was interrupted", command), e);
		} catch (IOException e) {
			throw new ProcessException(format("Unable to complete process '%s'", command), e);
		}
	}

	@Immutable
	protected static class TextConsumerResult {
		@Nullable
		private String text;
		@Nullable
		private Exception exception;

		public TextConsumerResult(@Nullable String text,
															@Nullable Exception exception) {
			this.text = text;
			this.exception = exception;
		}

		@Nonnull
		public Optional<String> getText() {
			return Optional.ofNullable(text);
		}

		@Nonnull
		public Optional<Exception> getException() {
			return Optional.ofNullable(exception);
		}
	}

	@ThreadSafe
	protected static class TextConsumer implements Runnable {
		@Nonnull
		private final InputStream inputStream;
		@Nonnull
		private final CompletableFuture<TextConsumerResult> textConsumerFuture;

		public TextConsumer(@Nonnull InputStream inputStream,
												@Nonnull CompletableFuture<TextConsumerResult> textConsumerFuture) {
			requireNonNull(inputStream);
			requireNonNull(textConsumerFuture);

			this.inputStream = inputStream;
			this.textConsumerFuture = textConsumerFuture;
		}

		@Override
		public void run() {
			Exception exception = null;
			List<String> lines = new ArrayList<>();

			try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(getInputStream()))) {
				String line;
				while ((line = bufferedReader.readLine()) != null)
					lines.add(line);
			} catch (IOException e) {
				exception = e;
			}

			String text = trimToNull(lines.stream().collect(Collectors.joining("\n")));
			getTextConsumerFuture().complete(new TextConsumerResult(text, exception));
		}

		@Nonnull
		protected InputStream getInputStream() {
			return inputStream;
		}

		@Nonnull
		protected CompletableFuture<TextConsumerResult> getTextConsumerFuture() {
			return textConsumerFuture;
		}
	}

	@Immutable
	public static class ProcessResult {
		@Nonnull
		private final Integer exitCode;
		@Nullable
		private final String output;

		public ProcessResult(@Nonnull Integer exitCode,
												 @Nullable String output) {
			requireNonNull(exitCode);

			this.exitCode = exitCode;
			this.output = trimToNull(output);
		}

		@Nonnull
		public Integer getExitCode() {
			return exitCode;
		}

		@Nonnull
		public Optional<String> getOutput() {
			return Optional.ofNullable(output);
		}
	}

	@NotThreadSafe
	public static class ProcessException extends RuntimeException {
		public ProcessException(@Nullable String message) {
			super(message);
		}

		public ProcessException(@Nullable String message,
														@Nullable Throwable cause) {
			super(message, cause);
		}
	}

	@Nonnull
	protected static Boolean getRunningOnWindows() {
		return RUNNING_ON_WINDOWS;
	}
}
