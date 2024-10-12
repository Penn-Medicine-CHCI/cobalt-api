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

package com.cobaltplatform.api.integration.google;

import com.cobaltplatform.api.integration.google.request.PlaceSearchTextRequest;
import com.cobaltplatform.api.integration.google.response.PlaceSearchTextResponse;

import javax.annotation.Nonnull;

/**
 * @author Transmogrify, LLC.
 */
public interface GoogleGeoClient extends AutoCloseable {
	// Maps Geocoding API for basic address geocoding
	// https://developers.google.com/maps/documentation/geocoding

	// Directions API for legacy directions between 2 places
	// https://developers.google.com/maps/documentation/directions

	// Routes API for directions between 2 places
	// https://developers.google.com/maps/documentation/routes

	// Places API (New) Autocomplete (New) for autocompleting place matches for a potentially-partial input string
	// https://developers.google.com/maps/documentation/places/web-service/place-autocomplete

	@Nonnull
	void geocode(@Nonnull String address);

	@Nonnull
	void directions(@Nonnull String originPlaceId,
									@Nonnull String destinationPlaceId);

	@Nonnull
	void route(@Nonnull String originPlaceId,
						 @Nonnull String destinationPlaceId);

	@Nonnull
	PlaceSearchTextResponse findPlacesBySearchText(@Nonnull PlaceSearchTextRequest placeSearchTextRequest);

	@Override
	default void close() throws Exception {
		// Nothing to do
	}
}
