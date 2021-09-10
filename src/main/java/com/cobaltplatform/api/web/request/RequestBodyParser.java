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

package com.cobaltplatform.api.web.request;

import com.cobaltplatform.api.util.JsonMapper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;

import static java.util.Objects.requireNonNull;

/**
 * This class exists solely to throw a special RequestBodyParsingException which can be specially handled at the
 * response-writing level.
 * <p>
 * This is in contrast to using {@link JsonMapper} directly, which would throw a more generic exception.
 *
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class RequestBodyParser {
	@Nonnull
	private final JsonMapper jsonMapper;

	@Inject
	public RequestBodyParser(@Nonnull JsonMapper jsonMapper) {
		requireNonNull(jsonMapper);
		this.jsonMapper = jsonMapper;
	}

	@Nonnull
	public <T> T parse(@Nullable String json, @Nonnull Class<T> type) {
		requireNonNull(type);

		if (json == null)
			throw new RequestBodyParsingException("No request body was specified");

		try {
			return jsonMapper.fromJson(json, type);
		} catch (Exception e) {
			throw new RequestBodyParsingException("Unable to parse request body", e);
		}
	}
}