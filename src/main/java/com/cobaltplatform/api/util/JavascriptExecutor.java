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

import javax.annotation.Nonnull;
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

	static {
		// Avoids performance warning log output that is not applicable to our use-case
		System.setProperty("polyglot.engine.WarnInterpreterOnly", "false");
		// Appears to be threadsafe (the GraalVM Context instances are not)
		SHARED_ENGINE = Engine.newBuilder("js").build();
	}

	public JavascriptExecutor() {
		this.jsonMapper = new JsonMapper.Builder().mappingFormat(MappingFormat.PRETTY_PRINTED).build();
	}

	public JavascriptExecutor(@Nonnull JsonMapper jsonMapper) {
		requireNonNull(jsonMapper);
		this.jsonMapper = jsonMapper;
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
		} catch (Exception e) {
			throw new JavascriptExecutionException(e, new HashMap<>(input) /* defensive copy */, javascript, executedJavascript);
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
}
