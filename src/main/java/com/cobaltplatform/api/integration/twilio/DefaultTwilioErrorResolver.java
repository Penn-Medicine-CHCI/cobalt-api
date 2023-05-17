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

package com.cobaltplatform.api.integration.twilio;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
public class DefaultTwilioErrorResolver implements TwilioErrorResolver {
	@Nonnull
	private final Gson gson;
	@Nonnull
	private final Map<Integer, TwilioError> twilioErrorsByErrorCode;

	public DefaultTwilioErrorResolver() {
		this.gson = new Gson();
		this.twilioErrorsByErrorCode = loadTwilioErrorsByErrorCode();
	}

	@Nonnull
	@Override
	public Optional<TwilioError> resolveTwilioErrorForErrorCode(@Nullable String errorCode) {
		errorCode = trimToNull(errorCode);

		if (errorCode == null)
			return Optional.empty();

		return resolveTwilioErrorForErrorCode(Integer.valueOf(errorCode));
	}

	@Nonnull
	@Override
	public Optional<TwilioError> resolveTwilioErrorForErrorCode(@Nullable Integer errorCode) {
		if (errorCode == null)
			return Optional.empty();

		return Optional.ofNullable(getTwilioErrorsByErrorCode().get(errorCode));
	}

	@Nonnull
	protected Map<Integer, TwilioError> loadTwilioErrorsByErrorCode() {
		try {
			String twilioErrorCodesJson = Files.readString(Path.of("resources/twilio/2023-05-17-error-codes.json"), StandardCharsets.UTF_8);
			List<TwilioError> twilioErrors = getGson().fromJson(twilioErrorCodesJson, new TypeToken<List<TwilioError>>() {
			}.getType());

			return twilioErrors.stream()
					.collect(Collectors.toMap(twilioError -> twilioError.getCode(), Function.identity()));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Nonnull
	protected Gson getGson() {
		return this.gson;
	}

	@Nonnull
	protected Map<Integer, TwilioError> getTwilioErrorsByErrorCode() {
		return this.twilioErrorsByErrorCode;
	}
}
