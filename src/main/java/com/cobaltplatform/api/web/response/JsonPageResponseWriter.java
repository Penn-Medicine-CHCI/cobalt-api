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

package com.cobaltplatform.api.web.response;

import com.cobaltplatform.api.model.security.AccessTokenStatus;
import com.cobaltplatform.api.util.AccessTokenException;
import com.lokalized.Strings;
import com.cobaltplatform.api.Configuration;
import com.soklet.web.response.PageResponse;
import com.soklet.web.response.writer.PageResponseWriter;
import com.soklet.web.routing.Route;
import com.cobaltplatform.api.util.Formatter;
import com.cobaltplatform.api.util.JsonMapper;
import com.cobaltplatform.api.util.ValidationException;
import com.cobaltplatform.api.util.ValidationException.FieldError;
import com.cobaltplatform.api.web.request.RequestBodyParsingException;
import org.eclipse.jetty.io.EofException;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.soklet.util.IoUtils.copyStreamCloseAfterwards;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class JsonPageResponseWriter implements PageResponseWriter {
	@Nonnull
	private final Configuration configuration;
	@Nonnull
	private final JsonMapper jsonMapper;
	@Nonnull
	private final Formatter formatter;
	@Nonnull
	private final Strings strings;

	@Inject
	public JsonPageResponseWriter(@Nonnull Configuration configuration,
																@Nonnull JsonMapper jsonMapper,
																@Nonnull Formatter formatter,
																@Nonnull Strings strings) {
		requireNonNull(configuration);
		requireNonNull(jsonMapper);
		requireNonNull(formatter);
		requireNonNull(strings);

		this.configuration = configuration;
		this.jsonMapper = jsonMapper;
		this.formatter = formatter;
		this.strings = strings;
	}

	@Override
	public void writeResponse(@Nonnull HttpServletRequest httpServletRequest,
														@Nonnull HttpServletResponse httpServletResponse,
														@Nonnull Optional<PageResponse> response,
														@Nonnull Optional<Route> route,
														@Nonnull Optional<Exception> exception) throws IOException {
		requireNonNull(httpServletRequest);
		requireNonNull(httpServletResponse);
		requireNonNull(response);
		requireNonNull(route);
		requireNonNull(exception);

		Object model;

		if (exception.isPresent()) {
			Map<String, Object> modelAsMap = new HashMap<>();
			int status = httpServletResponse.getStatus();
			String message = getStrings().get("Sorry, we were unable to complete your request.");
			ErrorCode code = ErrorCode.INTERNAL_SERVER_ERROR;

			if (exception.get() instanceof ValidationException) {
				ValidationException validationException = (ValidationException) exception.get();

				List<String> errors = new ArrayList<>();
				errors.addAll(validationException.getGlobalErrors());
				errors.addAll(validationException.getFieldErrors().stream().map(FieldError::getError).collect(toList()));

				message = errors.stream().collect(joining("\n"));

				modelAsMap.put("fieldErrors", validationException.getFieldErrors().stream().map(fieldError -> {
					Map<String, Object> fieldErrorAsMap = new HashMap<>();
					fieldErrorAsMap.put("field", fieldError.getField());
					fieldErrorAsMap.put("error", fieldError.getError());
					return fieldErrorAsMap;
				}).collect(toList()));

				modelAsMap.put("globalErrors", validationException.getGlobalErrors());
				modelAsMap.put("metadata", validationException.getMetadata());

				status = 422;
				code = ErrorCode.VALIDATION_FAILED;
			} else if (exception.get() instanceof RequestBodyParsingException || status == 400) {
				message = getStrings().get("The request was incorrectly formatted.");
				code = ErrorCode.BAD_REQUEST;
			} else if (exception.get() instanceof AccessTokenException) {
				AccessTokenException accessTokenException = (AccessTokenException) exception.get();
				modelAsMap.put("accessTokenStatus", accessTokenException.getAccessTokenStatus());
				modelAsMap.put("signOnUrl", accessTokenException.getSignOnUrl());
				message = getStrings().get("You must be authenticated to perform this action.");
				status = 401;
				code = ErrorCode.AUTHENTICATION_REQUIRED;
			} else if (status == 401) {
				modelAsMap.put("accessTokenStatus", AccessTokenStatus.FULLY_EXPIRED);
				message = getStrings().get("You must be authenticated to perform this action.");
				code = ErrorCode.AUTHENTICATION_REQUIRED;
			} else if (status == 403) {
				message = getStrings().get("You are not authorized to perform this action.");
				code = ErrorCode.AUTHORIZATION_REQUIRED;
			} else if (status == 404) {
				message = getStrings().get("The resource you requested was not found.");
				code = ErrorCode.NOT_FOUND;
			} else if (status == 405) {
				message = getStrings().get("The HTTP method you specified is not valid for this resource.");
				code = ErrorCode.METHOD_NOT_ALLOWED;
			}

			modelAsMap.put("message", message);
			modelAsMap.put("code", code);

			if (getConfiguration().getShouldDisplayStackTraces())
				modelAsMap.put("stackTrace", getFormatter().formatStackTrace(exception.get()));

			model = modelAsMap;

			httpServletResponse.setStatus(status);
		} else {
			model = response.get().model().orElse(null);
		}

		httpServletResponse.setContentType("application/json;charset=UTF-8");

		if (model != null) {
			// Write to a string first instead of directly to OutputStream.
			// This way if an error occurs, we can render a correct error response instead of terminating the write midstream
			String json = getJsonMapper().toJson(model);

			try {
				copyStreamCloseAfterwards(new ByteArrayInputStream(json.getBytes(UTF_8)),
						httpServletResponse.getOutputStream());
			} catch (EofException e) {
				// Ignored
			}
		}
	}

	public enum ErrorCode {
		BAD_REQUEST,
		VALIDATION_FAILED,
		AUTHENTICATION_REQUIRED,
		AUTHORIZATION_REQUIRED,
		NOT_FOUND,
		METHOD_NOT_ALLOWED,
		INTERNAL_SERVER_ERROR
	}

	@Nonnull
	protected Configuration getConfiguration() {
		return configuration;
	}

	@Nonnull
	protected JsonMapper getJsonMapper() {
		return jsonMapper;
	}

	@Nonnull
	protected Formatter getFormatter() {
		return formatter;
	}

	@Nonnull
	protected Strings getStrings() {
		return strings;
	}
}