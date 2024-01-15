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

package com.cobaltplatform.api.service;

import com.cobaltplatform.api.model.api.request.CreateShortUrlRequest;
import com.cobaltplatform.api.model.db.ShortUrl;
import com.cobaltplatform.api.util.Base62;
import com.cobaltplatform.api.util.ValidationException;
import com.cobaltplatform.api.util.ValidationException.FieldError;
import com.cobaltplatform.api.util.db.DatabaseProvider;
import com.lokalized.Strings;
import com.pyranid.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@Singleton
@ThreadSafe
public class ShortUrlService {
	@Nonnull
	private final DatabaseProvider databaseProvider;
	@Nonnull
	private final Strings strings;
	@Nonnull
	private final Logger logger;

	@Inject
	public ShortUrlService(@Nonnull DatabaseProvider databaseProvider,
												 @Nonnull Strings strings) {
		requireNonNull(databaseProvider);
		requireNonNull(strings);

		this.databaseProvider = databaseProvider;
		this.strings = strings;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	public Optional<ShortUrl> findShortUrlById(@Nullable Long shortUrlId) {
		if (shortUrlId == null)
			return Optional.empty();

		return getDatabase().queryForObject("SELECT * FROM short_url WHERE short_url_id=?", ShortUrl.class, shortUrlId);
	}

	@Nonnull
	public Optional<ShortUrl> findShortUrlByIdentifier(@Nullable String identifier) {
		identifier = trimToNull(identifier);

		if (identifier == null)
			return Optional.empty();

		return getDatabase().queryForObject("SELECT * FROM short_url WHERE identifier=?", ShortUrl.class, identifier);
	}

	@Nonnull
	public Long createShortUrl(@Nonnull CreateShortUrlRequest request) {
		requireNonNull(request);

		String url = trimToNull(request.getUrl());
		ValidationException validationException = new ValidationException();

		if (url == null)
			validationException.add(new FieldError("url", getStrings().get("URL is required.")));

		if (validationException.hasErrors())
			throw validationException;

		Long shortUrlId = getDatabase().queryForObject("SELECT nextval('short_url_id_seq')", Long.class).get();
		String identifier = encodeIdentifier(shortUrlId);

		getDatabase().execute("INSERT INTO short_url (short_url_id, identifier, url) VALUES (?,?,?)", shortUrlId, identifier, url);

		return shortUrlId;
	}

	@Nonnull
	protected String encodeIdentifier(@Nonnull Long shortUrlId) {
		requireNonNull(shortUrlId);

		Base62 base62 = Base62.createInstance();
		byte[] encoded = base62.encode(String.valueOf(shortUrlId).getBytes(StandardCharsets.UTF_8));
		return new String(encoded, StandardCharsets.UTF_8);
	}

	@Nonnull
	protected String decodeIdentifier(@Nonnull String identifier) {
		requireNonNull(identifier);

		Base62 base62 = Base62.createInstance();
		byte[] decoded = base62.decode(identifier.getBytes(StandardCharsets.UTF_8));
		return new String(decoded, StandardCharsets.UTF_8);
	}

	@Nonnull
	protected Database getDatabase() {
		return this.databaseProvider.get();
	}

	@Nonnull
	protected Strings getStrings() {
		return strings;
	}

	@Nonnull
	protected Logger getLogger() {
		return logger;
	}
}
