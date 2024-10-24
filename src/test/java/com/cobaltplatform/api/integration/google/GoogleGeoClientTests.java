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

import com.google.maps.DirectionsApi;
import com.google.maps.GeocodingApiRequest;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;
import com.google.maps.model.GeocodedWaypoint;
import com.google.maps.model.GeocodingResult;
import com.google.maps.model.TravelMode;
import com.google.maps.model.Unit;
import com.google.maps.places.v1.AutocompletePlacesRequest;
import com.google.maps.places.v1.AutocompletePlacesResponse;
import com.google.maps.places.v1.GetPlaceRequest;
import com.google.maps.places.v1.Place;
import com.google.maps.places.v1.SearchTextRequest;
import com.google.maps.places.v1.SearchTextResponse;
import com.google.maps.routing.v2.ComputeRoutesRequest;
import com.google.maps.routing.v2.ComputeRoutesResponse;
import com.google.maps.routing.v2.RouteModifiers;
import com.google.maps.routing.v2.RouteTravelMode;
import com.google.maps.routing.v2.RoutingPreference;
import com.google.maps.routing.v2.Waypoint;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static com.soklet.util.LoggingUtils.initializeLogback;
import static java.lang.String.format;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class GoogleGeoClientTests {
	@BeforeClass
	public static void initialize() {
		initializeLogback(Paths.get("config/local/logback.xml"));
	}

	@Test
	public void testGeocode() throws Exception {
		try (GoogleGeoClient googleGeoClient = acquireGoogleGeoClient()) {
			List<GeocodingResult> results = googleGeoClient.geocode((geoApiContext -> {
				return new GeocodingApiRequest(geoApiContext)
						.address("112 Fayette St 2nd Fl Conshohocken PA 19428")
						.region("US");
			}));

			System.out.println(results);

			Assert.assertTrue("No geocoding results found", results.size() > 0);
		}
	}

	@Test
	public void testFindPlacesBySearchText() throws Exception {
		try (GoogleGeoClient googleGeoClient = acquireGoogleGeoClient()) {
			SearchTextRequest searchTextRequest =
					SearchTextRequest.newBuilder()
							.setTextQuery("Transmogrify LLC 112 Fayette St Conshohocken PA 19428")
							.setLanguageCode("en")
							.setRegionCode("US")
							//.setIncludedType("includedType-45971946")
							//.setOpenNow(true)
							//.setMinRating(-543315926)
							//.setMaxResultCount(-1736124056)
							//.addAllPriceLevels(new ArrayList<PriceLevel>())
							//.setStrictTypeFiltering(true)
							//.setLocationBias(SearchTextRequest.LocationBias.newBuilder().build())
							//.setLocationRestriction(SearchTextRequest.LocationRestriction.newBuilder().build())
							//.setEvOptions(SearchTextRequest.EVOptions.newBuilder().build())
							//.setRoutingParameters(RoutingParameters.newBuilder().build())
							//.setSearchAlongRouteParameters(SearchTextRequest.SearchAlongRouteParameters.newBuilder().build())
							.build();

			SearchTextResponse response = googleGeoClient.findPlacesBySearchText(searchTextRequest);
			System.out.println(response);

			Assert.assertTrue("No places found", response.getPlacesList().size() > 0);
		}
	}

	@Test
	public void testGetPlace() throws Exception {
		try (GoogleGeoClient googleGeoClient = acquireGoogleGeoClient()) {
			String placeId = "places/ChIJ97AN5zC-xokRyT9EnG7aPzc";

			GetPlaceRequest request = GetPlaceRequest.newBuilder()
					.setName(placeId)
					.setLanguageCode("en")
					.setRegionCode("US")
					.build();

			Place place = googleGeoClient.getPlace(request).orElse(null);

			System.out.println(place);
		}
	}

	@Test
	public void testRoute() throws Exception {
		try (GoogleGeoClient googleGeoClient = acquireGoogleGeoClient()) {
			String originPlaceId = "ChIJ97AN5zC-xokRyT9EnG7aPzc";
			String destinationPlaceId = "ChIJc_ABK2G-xokRVVrQ0ZZOb9c";

			ComputeRoutesRequest request = ComputeRoutesRequest.newBuilder()
					.setOrigin(Waypoint.newBuilder().setPlaceId(originPlaceId).build())
					.setDestination(Waypoint.newBuilder().setPlaceId(destinationPlaceId).build())
					.setTravelMode(RouteTravelMode.DRIVE)
					.setRoutingPreference(RoutingPreference.TRAFFIC_AWARE)
					.setComputeAlternativeRoutes(true)
					.setRouteModifiers(RouteModifiers.newBuilder()
							.setAvoidTolls(false)
							.setAvoidHighways(true)
							.setAvoidFerries(true)
					).build();

			ComputeRoutesResponse response = googleGeoClient.computeRoutes(request);
			System.out.println(response);

			Assert.assertTrue("No routes found", response.getRoutesCount() > 0);
		}
	}

	@Test
	public void testDirections() throws Exception {
		try (GoogleGeoClient googleGeoClient = acquireGoogleGeoClient()) {
			String originPlaceId = "place_id:ChIJ97AN5zC-xokRyT9EnG7aPzc";
			String destinationPlaceId = "place_id:ChIJc_ABK2G-xokRVVrQ0ZZOb9c";

			DirectionsResult result = googleGeoClient.directions((geoApiContext -> {
				return DirectionsApi.newRequest(geoApiContext)
						.mode(TravelMode.DRIVING)
						.avoid(
								DirectionsApi.RouteRestriction.HIGHWAYS,
								DirectionsApi.RouteRestriction.TOLLS
						)
						.units(Unit.IMPERIAL)
						.region("US")
						.language("en")
						.origin(originPlaceId)
						.destination(destinationPlaceId);
			}));

			List<DirectionsRoute> directionsRoutes = result.routes == null ? List.of() : Arrays.stream(result.routes).toList();
			List<GeocodedWaypoint> geocodedWaypoints = result.geocodedWaypoints == null ? List.of() : Arrays.stream(result.geocodedWaypoints).toList();

			System.out.println(result);

			Assert.assertTrue("No directions found", directionsRoutes.size() > 0);
			Assert.assertTrue("No geocoded waypoints found", geocodedWaypoints.size() > 0);
		}
	}

	@Test
	public void testAutocomplete() throws Exception {
		try (GoogleGeoClient googleGeoClient = acquireGoogleGeoClient()) {
			AutocompletePlacesResponse response = googleGeoClient.autocompletePlaces(AutocompletePlacesRequest.newBuilder()
					.setInput("Transmogri")
					.setLanguageCode("en")
					.setRegionCode("US")
					.build());

			System.out.println(response);

			Assert.assertTrue("No autocomplete suggestions found", response.getSuggestionsList().size() > 0);
		}
	}

	@Nonnull
	protected GoogleGeoClient acquireGoogleGeoClient() {
		final String MAPS_PLATFORM_API_KEY_FILENAME = "localstack/secrets/cobalt-local-google-maps-api-key-COBALT_IC";
		final String GEO_SERVICE_ACCOUNT_PRIVATE_KEY_FILENAME = "localstack/secrets/cobalt-local-google-geo-service-account-private-key-COBALT_IC";

		try {
			Path mapsPlatformApiKeyFile = Path.of(MAPS_PLATFORM_API_KEY_FILENAME);
			Path geoServiceAccountPrivateKeyFile = Path.of(GEO_SERVICE_ACCOUNT_PRIVATE_KEY_FILENAME);

			if (!Files.exists(mapsPlatformApiKeyFile) || Files.isDirectory(mapsPlatformApiKeyFile))
				throw new IOException(format("Please ensure %s exists", mapsPlatformApiKeyFile.toAbsolutePath()));

			if (!Files.exists(geoServiceAccountPrivateKeyFile) || Files.isDirectory(geoServiceAccountPrivateKeyFile))
				throw new IOException(format("Please ensure %s exists", geoServiceAccountPrivateKeyFile.toAbsolutePath()));

			String mapsPlatformApiKey = Files.readString(mapsPlatformApiKeyFile, StandardCharsets.UTF_8);
			String serviceAccountPrivateKeyJson = Files.readString(geoServiceAccountPrivateKeyFile, StandardCharsets.UTF_8);

			return new DefaultGoogleGeoClient(mapsPlatformApiKey, serviceAccountPrivateKeyJson);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}

