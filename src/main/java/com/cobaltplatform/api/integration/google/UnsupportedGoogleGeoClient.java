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
import javax.annotation.concurrent.ThreadSafe;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class UnsupportedGoogleGeoClient implements GoogleGeoClient {
	@Nonnull
	@Override
	public String getMapsPlatformApiKey() {
		throw new UnsupportedOperationException();
	}

	@Nonnull
	@Override
	public List<GeocodingResult> geocode(@Nonnull Function<GeoApiContext, GeocodingApiRequest> geocodingApiRequestProvider) {
		throw new UnsupportedOperationException();
	}

	@Nonnull
	@Override
	public DirectionsResult directions(@Nonnull Function<GeoApiContext, DirectionsApiRequest> directionsApiRequestProvider) {
		throw new UnsupportedOperationException();
	}

	@Nonnull
	@Override
	public ComputeRoutesResponse computeRoutes(@Nonnull ComputeRoutesRequest request) {
		throw new UnsupportedOperationException();
	}

	@Nonnull
	@Override
	public SearchTextResponse findPlacesBySearchText(@Nonnull SearchTextRequest searchTextRequest) {
		throw new UnsupportedOperationException();
	}

	@Nonnull
	@Override
	public AutocompletePlacesResponse autocompletePlaces(@Nonnull AutocompletePlacesRequest request) {
		throw new UnsupportedOperationException();
	}

	@Nonnull
	@Override
	public Optional<Place> getPlace(@Nonnull GetPlaceRequest request) {
		throw new UnsupportedOperationException();
	}
}
