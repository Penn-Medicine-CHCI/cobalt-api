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
import com.cobaltplatform.api.model.api.response.VideoApiResponse.VideoApiResponseFactory;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.Video;
import com.cobaltplatform.api.service.AuthorizationService;
import com.cobaltplatform.api.service.VideoService;
import com.soklet.web.annotation.GET;
import com.soklet.web.annotation.PathParameter;
import com.soklet.web.annotation.Resource;
import com.soklet.web.exception.AuthorizationException;
import com.soklet.web.exception.NotFoundException;
import com.soklet.web.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.Map;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Resource
@Singleton
@ThreadSafe
public class VideoResource {
	@Nonnull
	private final AuthorizationService authorizationService;
	@Nonnull
	private final VideoService videoService;
	@Nonnull
	private final VideoApiResponseFactory videoApiResponseFactory;
	@Nonnull
	private final Provider<CurrentContext> currentContextProvider;
	@Nonnull
	private final Logger logger;

	@Inject
	public VideoResource(@Nonnull AuthorizationService authorizationService,
											 @Nonnull VideoService videoService,
											 @Nonnull VideoApiResponseFactory videoApiResponseFactory,
											 @Nonnull Provider<CurrentContext> currentContextProvider) {
		requireNonNull(authorizationService);
		requireNonNull(videoService);
		requireNonNull(videoApiResponseFactory);
		requireNonNull(currentContextProvider);

		this.authorizationService = authorizationService;
		this.videoService = videoService;
		this.videoApiResponseFactory = videoApiResponseFactory;
		this.currentContextProvider = currentContextProvider;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	@GET("/videos/{videoId}")
	public ApiResponse video(@Nonnull @PathParameter UUID videoId) {
		requireNonNull(videoId);

		InstitutionId institutionId = getCurrentContext().getInstitutionId();
		Video video = getVideoService().findVideoById(videoId).orElse(null);

		if (video == null)
			throw new NotFoundException();

		// Currently, you don't need to be authenticated to view a video, e.g. a sizzle reel on the sign-in screen.
		// ...so we only check against institution as defined by your URL, not via signed-in account.
		// Later, we might want to explicitly mark videos as "public" or "private" to support more aggressive privacy checks.
		if (!getAuthorizationService().canViewVideo(video, institutionId))
			throw new AuthorizationException();

		return new ApiResponse(Map.of(
				"video", getVideoApiResponseFactory().create(video)
		));
	}

	@Nonnull
	protected AuthorizationService getAuthorizationService() {
		return this.authorizationService;
	}

	@Nonnull
	protected VideoService getVideoService() {
		return this.videoService;
	}

	@Nonnull
	protected VideoApiResponseFactory getVideoApiResponseFactory() {
		return this.videoApiResponseFactory;
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
