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

import com.cobaltplatform.api.util.JsonMapper.MappingFormat;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify LLC.
 */
@Singleton
@ThreadSafe
public class JavascriptExecutor {
	@Nonnull
	private static final Engine SHARED_ENGINE;
	@Nonnull
	private final JsonMapper jsonMapper;
	@Nonnull
	private final Logger logger;

	static {
		// Avoids performance warning log output that is not applicable to our use-case
		System.setProperty("polyglot.engine.WarnInterpreterOnly", "false");
		// Appears to be threadsafe (the GraalVM Context instances are not)
		SHARED_ENGINE = Engine.newBuilder("js").build();
	}

	public JavascriptExecutor() {
		this(new JsonMapper.Builder().mappingFormat(MappingFormat.PRETTY_PRINTED).build());
	}

	public JavascriptExecutor(@Nonnull JsonMapper jsonMapper) {
		requireNonNull(jsonMapper);
		this.jsonMapper = jsonMapper;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	/**
	 * Executes the provided Javascript code over the given inputs and returns an output object.
	 * <p>
	 * Javascript code will automatically have `input` and `output` objects exposed to it.
	 * <p>
	 * At the end of execution, the `output` object will be marshaled to a new instance of the given {@code outputType}
	 * and returned to the Java caller.  The Javascript code should not explicitly {@code return} any values.
	 *
	 * @param javascript the Javascript code to execute
	 * @param input      values to expose to the Javascript code via the {@code input} Javascript object
	 * @param outputType the Java type token to which the Javascript {@code output} object is marshaled after execution completes
	 * @param <T>        the Java type represented by the {@code outputType} token
	 * @return An instance of {@code outputType} populated with the Javascript {@code output} object's values
	 * @throws JavascriptExecutionException If an error occurs while executing the Javascript
	 */
	@Nonnull
	public <T> T execute(@Nonnull String javascript,
											 @Nonnull Map<String, Object> input,
											 @Nonnull Class<T> outputType) throws JavascriptExecutionException {
		requireNonNull(javascript);
		requireNonNull(input);
		requireNonNull(outputType);

		long startTime = System.currentTimeMillis();

		Context context = Context.newBuilder("js").engine(getEngine()).build();

		// Convert context fields into JSON to embed in the JS
		List<String> contextDeclarations = new ArrayList<>(input.size());

		for (Entry<String, Object> entry : input.entrySet())
			contextDeclarations.add(format("input.%s=%s;", entry.getKey(), getJsonMapper().toJson(entry.getValue())));

		// JS is understood to modify `output` object (add fields etc.) and that is what is returned...
		String executedJavascript = format("""
				(function() {
				const input = {};
				const output = {};
				
				// Input values set
				%s
				
				// User-provided JS				    
				%s
				
				return JSON.stringify(output);
				})();
				""", contextDeclarations.stream().collect(Collectors.joining("\n")), javascript).trim();

		try {
			Object outputValue = context.eval("js", executedJavascript);
			T result = getJsonMapper().fromJson(outputValue.toString(), outputType);

			return result;
		} catch (PolyglotException polyglotException) {
			if (polyglotException.isGuestException()) {
				Value thrown = polyglotException.getGuestObject();

				// If custom JS threw a string: throw "..." or throw new Error("...")
				if (thrown != null && thrown.isString())
					throw new JavascriptExecutionException(polyglotException, new HashMap<>(input), javascript, executedJavascript /* plus msg somewhere */);

				// If custom JS threw an object: throw { ... }
				if (thrown != null && thrown.hasMembers()) {
					// Convert to JSON for logging/transport
					// (simple approach: call JSON.stringify on the guest value)
					String json = context.eval("js", "JSON.stringify").execute(thrown).asString();

					// Attempt to parse out a validation exception if we see an object with { "type": "VALIDATION_EXCEPTION" }
					ValidationException validationException = null;

					try {
						CustomJsValidationException result = getJsonMapper().fromJson(json, CustomJsValidationException.class);

						if (result != null && result.getType() == CustomJsExceptionType.VALIDATION_EXCEPTION) {
							List<ValidationException.FieldError> fieldErrors = result.getFieldErrors() == null ? List.of() : result.getFieldErrors();
							List<String> globalErrors = result.getGlobalErrors() == null ? List.of() : result.getGlobalErrors();
							Map<String, Object> metadata = result.getMetadata() == null ? Map.of() : result.getMetadata();

							validationException = new ValidationException(globalErrors, fieldErrors);
							validationException.setMetadata(metadata);
						}
					} catch (Exception ignored) {
						// Can't parse; fall through
					}

					if (validationException != null && validationException.hasErrors())
						throw validationException;

					// Now we have a real string payload
					throw new JavascriptExecutionException(
							new RuntimeException("JS threw object: " + json, polyglotException),
							new HashMap<>(input),
							javascript,
							executedJavascript
					);
				}
			}

			throw new JavascriptExecutionException(polyglotException, new HashMap<>(input), javascript, executedJavascript);
		} catch (Exception e) {
			throw new JavascriptExecutionException(e, new HashMap<>(input), javascript, executedJavascript);
		} finally {
			getLogger().debug("JS function execution took {}ms.", System.currentTimeMillis() - startTime);
		}
	}

	private enum CustomJsExceptionType {
		VALIDATION_EXCEPTION
	}

	@NotThreadSafe
	private static abstract class CustomJsException {
		@Nullable
		private CustomJsExceptionType type;

		@Nullable
		public CustomJsExceptionType getType() {
			return this.type;
		}

		public void setType(@Nullable CustomJsExceptionType type) {
			this.type = type;
		}
	}

	@NotThreadSafe
	private static final class CustomJsValidationException extends CustomJsException {
		@Nullable
		private List<ValidationException.FieldError> fieldErrors;
		@Nullable
		private List<String> globalErrors;
		@Nullable
		private Map<String, Object> metadata;

		@Nullable
		public List<ValidationException.FieldError> getFieldErrors() {
			return this.fieldErrors;
		}

		public void setFieldErrors(@Nullable List<ValidationException.FieldError> fieldErrors) {
			this.fieldErrors = fieldErrors;
		}

		@Nullable
		public List<String> getGlobalErrors() {
			return this.globalErrors;
		}

		public void setGlobalErrors(@Nullable List<String> globalErrors) {
			this.globalErrors = globalErrors;
		}

		@Nullable
		public Map<String, Object> getMetadata() {
			return this.metadata;
		}

		public void setMetadata(@Nullable Map<String, Object> metadata) {
			this.metadata = metadata;
		}
	}

	@Nonnull
	protected Engine getEngine() {
		return SHARED_ENGINE;
	}

	@Nonnull
	protected JsonMapper getJsonMapper() {
		return this.jsonMapper;
	}

	@Nonnull
	protected Logger getLogger() {
		return this.logger;
	}
}