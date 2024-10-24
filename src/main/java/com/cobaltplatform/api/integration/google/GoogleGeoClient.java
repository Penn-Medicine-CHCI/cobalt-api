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

import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApiRequest;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.GeocodingResult;
import com.google.maps.places.v1.AutocompletePlacesRequest;
import com.google.maps.places.v1.AutocompletePlacesResponse;
import com.google.maps.places.v1.GetPlaceRequest;
import com.google.maps.places.v1.Place;
import com.google.maps.places.v1.SearchTextRequest;
import com.google.maps.places.v1.SearchTextResponse;
import com.google.maps.routing.v2.ComputeRoutesRequest;
import com.google.maps.routing.v2.ComputeRoutesResponse;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * @author Transmogrify, LLC.
 */
public interface GoogleGeoClient extends AutoCloseable {
	// Maps Geocoding API for basic address geocoding
	// https://developers.google.com/maps/documentation/geocoding
	@Nonnull
	List<GeocodingResult> geocode(@Nonnull Function<GeoApiContext, GeocodingApiRequest> geocodingApiRequestProvider);

	// Directions API for legacy directions between 2 places
	// https://developers.google.com/maps/documentation/directions
	@Nonnull
	DirectionsResult directions(@Nonnull Function<GeoApiContext, DirectionsApiRequest> directionsApiRequestProvider);

	// Routes API for directions between 2 places
	// https://developers.google.com/maps/documentation/routes
	@Nonnull
	ComputeRoutesResponse computeRoutes(@Nonnull ComputeRoutesRequest request);

	// Places API (New) Text Search for place matches for a potentially-partial input string
	// https://developers.google.com/maps/documentation/places/web-service/search-text
	@Nonnull
	SearchTextResponse findPlacesBySearchText(@Nonnull SearchTextRequest searchTextRequest);

	// Places API (New) Place Autocomplete for "autocomplete" place matches for a potentially-partial input string
	// https://developers.google.com/maps/documentation/places/web-service/place-autocomplete
	@Nonnull
	AutocompletePlacesResponse autocompletePlaces(@Nonnull AutocompletePlacesRequest request);

	// Places API (New) for place details
	// https://developers.google.com/maps/documentation/places/web-service/place-details
	@Nonnull
	Optional<Place> getPlace(@Nonnull GetPlaceRequest request);

	@Override
	default void close() throws Exception {
		// Nothing to do
	}
}
