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

package com.cobaltplatform.api.web.resource;

import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.model.db.ShortUrl;
import com.cobaltplatform.api.service.ShortUrlService;
import com.soklet.web.annotation.GET;
import com.soklet.web.annotation.PathParameter;
import com.soklet.web.annotation.Resource;
import com.soklet.web.exception.NotFoundException;
import com.soklet.web.response.RedirectResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Resource
@Singleton
@ThreadSafe
public class ShortUrlResource {
	@Nonnull
	private final ShortUrlService shortUrlService;
	@Nonnull
	private final Provider<CurrentContext> currentContextProvider;
	@Nonnull
	private final Logger logger;

	@Inject
	public ShortUrlResource(@Nonnull ShortUrlService shortUrlService,
													@Nonnull Provider<CurrentContext> currentContextProvider) {
		requireNonNull(shortUrlService);
		requireNonNull(currentContextProvider);

		this.shortUrlService = shortUrlService;
		this.currentContextProvider = currentContextProvider;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	@GET("/short-urls/{shortCode}/redirect")
	public RedirectResponse shortUrlRedirect(@Nonnull @PathParameter String shortCode) {
		requireNonNull(shortCode);

		ShortUrl shortUrl = getShortUrlService().findShortUrlByShortCode(shortCode).orElse(null);

		if (shortUrl == null)
			throw new NotFoundException();

		return new RedirectResponse(shortUrl.getFullUrl());
	}

	@Nonnull
	protected ShortUrlService getShortUrlService() {
		return shortUrlService;
	}

	@Nonnull
	protected CurrentContext getCurrentContext() {
		return currentContextProvider.get();
	}

	@Nonnull
	protected Logger getLogger() {
		return logger;
	}
}
