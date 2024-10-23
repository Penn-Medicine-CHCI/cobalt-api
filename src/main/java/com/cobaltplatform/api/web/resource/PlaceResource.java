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

import com.cobaltplatform.api.Configuration;
import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.model.places.PlacePrediction;
import com.cobaltplatform.api.model.security.AuthenticationRequired;
import com.cobaltplatform.api.service.PlaceService;
import com.soklet.web.annotation.GET;
import com.soklet.web.annotation.QueryParameter;
import com.soklet.web.annotation.Resource;
import com.soklet.web.response.ApiResponse;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify LLC.
 */
@Resource
@Singleton
@ThreadSafe
public class PlaceResource {
	@Nonnull
	private final Provider<CurrentContext> currentContextProvider;
	@Nonnull
	private final Configuration configuration;

	@Nonnull
	private final PlaceService placeService;

	@Inject
	public PlaceResource(@Nonnull Provider<CurrentContext> currentContextProvider,
											 @Nonnull Configuration configuration,
											 @Nonnull PlaceService placeService) {
		requireNonNull(currentContextProvider);
		requireNonNull(configuration);
		requireNonNull(placeService);

		this.currentContextProvider = currentContextProvider;
		this.configuration = configuration;
		this.placeService = placeService;
	}

	@Nonnull
	@GET("/places/autocomplete")
	@AuthenticationRequired
	public ApiResponse autocomplete(@Nonnull @QueryParameter String searchText) {
		requireNonNull(searchText);
		List<PlacePrediction> placePredictions = new ArrayList<>();

		placePredictions = placeService.autocompletePlace(searchText);

		final List<PlacePrediction> placePredictionsFinal = placePredictions;
		return new ApiResponse(new HashMap<String, Object>() {{
			put("places", placePredictionsFinal);
		}});
	}

	@Nonnull
	protected CurrentContext getCurrentContext() {
		return this.currentContextProvider.get();
	}

	@Nonnull
	protected Configuration getConfiguration() {
		return this.configuration;
	}

	@Nonnull
	public PlaceService getPlaceService() {
		return placeService;
	}
}
