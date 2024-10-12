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
import com.cobaltplatform.api.integration.google.response.PlaceSearchTextResponse.NormalizedPlace;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.gax.rpc.HeaderProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.common.io.CharStreams;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.maps.DirectionsApi;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.GeocodingApiRequest;
import com.google.maps.errors.ApiException;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.GeocodingResult;
import com.google.maps.model.TravelMode;
import com.google.maps.model.Unit;
import com.google.maps.places.v1.Place;
import com.google.maps.places.v1.Place.AddressComponent;
import com.google.maps.places.v1.PlacesClient;
import com.google.maps.places.v1.PlacesSettings;
import com.google.maps.places.v1.SearchTextRequest;
import com.google.maps.places.v1.SearchTextResponse;
import com.google.maps.routing.v2.RoutesClient;
import com.google.maps.routing.v2.RoutesSettings;
import com.google.type.LatLng;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class DefaultGoogleGeoClient implements GoogleGeoClient, Closeable {
	@Nonnull
	private final String mapsPlatformApiKey;
	@Nonnull
	private final GeoApiContext geoApiContext;
	@Nonnull
	private final String projectId;
	@Nonnull
	private final GoogleCredentials googleCredentials;
	@Nonnull
	private final PlacesClient placesClient;
	@Nonnull
	private final RoutesClient routesClient;

	public DefaultGoogleGeoClient(@Nonnull String mapsPlatformApiKey,
																@Nonnull String serviceAccountPrivateKeyJson) {
		// ByteArrayInputStream does not need to be closed
		this(mapsPlatformApiKey, new ByteArrayInputStream(serviceAccountPrivateKeyJson.getBytes(StandardCharsets.UTF_8)));
	}

	public DefaultGoogleGeoClient(@Nonnull String mapsPlatformApiKey,
																@Nonnull InputStream serviceAccountPrivateKeyJsonInputStream) {
		requireNonNull(mapsPlatformApiKey);
		requireNonNull(serviceAccountPrivateKeyJsonInputStream);

		try {
			String serviceAccountPrivateKeyJson = CharStreams.toString(new InputStreamReader(requireNonNull(serviceAccountPrivateKeyJsonInputStream), StandardCharsets.UTF_8));

			// Confirm that this is well-formed JSON and extract the project ID
			Map<String, Object> jsonObject = new Gson().fromJson(serviceAccountPrivateKeyJson, new TypeToken<Map<String, Object>>() {
			}.getType());

			this.mapsPlatformApiKey = mapsPlatformApiKey;
			this.geoApiContext = new GeoApiContext.Builder()
					.apiKey(mapsPlatformApiKey)
					.build();

			this.projectId = requireNonNull((String) jsonObject.get("project_id"));
			this.googleCredentials = acquireGoogleCredentials(serviceAccountPrivateKeyJson);
			this.placesClient = createPlacesClient(this.googleCredentials);
			this.routesClient = createRoutesClient(this.googleCredentials);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public void close() throws IOException {
		try {
			getGeoApiContext().close();
		} catch (Exception ignored) {
			// Do nothing
		}

		try {
			getPlacesClient().close();
		} catch (Exception ignored) {
			// Do nothing
		}

		try {
			getRoutesClient().close();
		} catch (Exception ignored) {
			// Do nothing
		}
	}

	@Nonnull
	@Override
	public void geocode(@Nonnull String address) {
		requireNonNull(address);

		GeocodingApiRequest request = GeocodingApi.geocode(getGeoApiContext(), address).region("us");
		GeocodingResult[] geocodingResults;

		try {
			geocodingResults = request.await();
		} catch (ApiException | InterruptedException | IOException e) {
			throw new RuntimeException(format("An error occurred while attempting to geocode address '%s'", address), e);
		}

		System.out.println(new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(geocodingResults));
	}

	@Nonnull
	@Override
	public PlaceSearchTextResponse findPlacesBySearchText(@Nonnull PlaceSearchTextRequest placeSearchTextRequest) {
		// AutocompletePlacesRequest request = AutocompletePlacesRequest.newBuilder().build();

		SearchTextRequest searchTextRequest =
				SearchTextRequest.newBuilder()
						.setTextQuery(placeSearchTextRequest.getTextQuery())
						.setLanguageCode(placeSearchTextRequest.getLanguageCode())
						.setRegionCode(placeSearchTextRequest.getRegionCode())
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

		SearchTextResponse searchTextResponse = placesClient.searchText(searchTextRequest);

		List<Place> rawPlaces = searchTextResponse.getPlacesList();
		List<NormalizedPlace> normalizedPlaces = new ArrayList<>(rawPlaces.size());

		for (Place place : rawPlaces) {
			LatLng location = place.getLocation();
			List<AddressComponent> addressComponents = place.getAddressComponentsList();

			String googlePlaceId = place.getId();
			String formattedAddress = place.getFormattedAddress();
			Double latitude = location.getLatitude();
			Double longitude = location.getLongitude();
			String streetNumber = null;
			String route = null;
			String streetNumberAndRoute = null; // 123 Fake St.
			String premise = null; // e.g. Building Name
			String subpremise = null; // e.g. "2nd Fl" or "Suite 3"
			String locality = null;
			String administrativeAreaLevel3 = null;
			String region = null;
			String regionSubdivision = null;
			String postalCode = null;
			String postalCodeSuffix = null;
			String countryCode = null;

			for (AddressComponent addressComponent : addressComponents) {
				Set<String> types = new HashSet<>(addressComponent.getTypesList());

				if (types.contains("premise")) {
					premise = addressComponent.getLongText(); // e.g. "Chrysler Building"
				} else if (types.contains("subpremise")) {
					subpremise = addressComponent.getLongText(); // e.g. "2nd Fl" or "Suite 3"

					// If it's just a number and nothing else, put a "#" in front for consistency
					if (subpremise != null && subpremise.matches("\\d+"))
						subpremise = format("# %s", subpremise);
				} else if (types.contains("street_number")) {
					streetNumber = addressComponent.getLongText(); // e.g. "123"
				} else if (types.contains("route")) {
					route = addressComponent.getLongText(); // e.g. "Fake St."
				} else if (types.contains("locality")) {
					locality = addressComponent.getLongText(); // e.g. "Philadelphia"
				} else if (types.contains("administrative_area_level_1")) {
					region = addressComponent.getShortText(); // e.g. "PA"
				} else if (types.contains("administrative_area_level_2")) {
					regionSubdivision = addressComponent.getShortText(); // e.g. "Montgomery County"
				} else if (types.contains("administrative_area_level_3")) {
					administrativeAreaLevel3 = addressComponent.getLongText(); // Sometimes city (administrative_area_level_1) is missing, this can be used to replace it
				} else if (types.contains("postal_code")) {
					postalCode = addressComponent.getLongText(); // e.g. "19119"
				} else if (types.contains("postal_code_suffix")) {
					postalCodeSuffix = addressComponent.getLongText(); // e.g. "1725"
				} else if (types.contains("country")) {
					countryCode = addressComponent.getShortText(); // e.g. "US"
				}
			}

			// Handle edge case where locality (city) can be missing, but it's available via administrative area level 3
			if(locality == null)
				locality = administrativeAreaLevel3;

			List<String> streetNumberAndRouteComponents = new ArrayList<>();

			if (streetNumber != null)
				streetNumberAndRouteComponents.add(streetNumber);
			if (route != null)
				streetNumberAndRouteComponents.add(route);

			streetNumberAndRoute = streetNumberAndRouteComponents.size() == 0 ? null : streetNumberAndRouteComponents.stream().collect(Collectors.joining(" "));

			// Figure out address lines 1, 2, and 3
			List<String> addressLines = new ArrayList<>();

			if (premise != null)
				addressLines.add(premise);

			if (streetNumberAndRoute != null)
				addressLines.add(streetNumberAndRoute);

			if (subpremise != null)
				addressLines.add(subpremise);

			String streetAddress1 = addressLines.size() > 0 ? addressLines.get(0) : null;
			String streetAddress2 = addressLines.size() > 1 ? addressLines.get(1) : null;
			String streetAddress3 = addressLines.size() > 2 ? addressLines.get(2) : null;

			NormalizedPlace normalizedPlace = new NormalizedPlace();
			normalizedPlace.setGooglePlaceId(googlePlaceId);
			normalizedPlace.setLatitude(latitude);
			normalizedPlace.setLongitude(longitude);
			normalizedPlace.setPremise(premise);
			normalizedPlace.setSubpremise(subpremise);
			normalizedPlace.setFormattedAddress(formattedAddress);
			normalizedPlace.setStreetAddress1(streetAddress1);
			normalizedPlace.setStreetAddress2(streetAddress2);
			normalizedPlace.setStreetAddress3(streetAddress3);
			normalizedPlace.setLocality(locality);
			normalizedPlace.setRegion(region);
			normalizedPlace.setRegionSubdivision(regionSubdivision);
			normalizedPlace.setPostalCode(postalCode);
			normalizedPlace.setPostalCodeSuffix(postalCodeSuffix);
			normalizedPlace.setCountryCode(countryCode);
			normalizedPlace.setGoogleMapsUrl(place.getGoogleMapsUri());

			normalizedPlaces.add(normalizedPlace);
		}

		//System.out.println(new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(response));

		PlaceSearchTextResponse placeSearchTextResponse = new PlaceSearchTextResponse();
		placeSearchTextResponse.setNormalizedPlaces(normalizedPlaces);
		placeSearchTextResponse.setRawPlaces(rawPlaces);

		return placeSearchTextResponse;
	}

	@Nonnull
	@Override
	public void directions(@Nonnull String originPlaceId,
												 @Nonnull String destinationPlaceId) {
		requireNonNull(originPlaceId);
		requireNonNull(destinationPlaceId);

		if (!originPlaceId.startsWith("place_id:"))
			originPlaceId = "place_id:" + originPlaceId;

		if (!destinationPlaceId.startsWith("place_id:"))
			destinationPlaceId = "place_id:" + destinationPlaceId;

		DirectionsApiRequest request = DirectionsApi.newRequest(getGeoApiContext())
				.mode(TravelMode.DRIVING)
				.avoid(
						DirectionsApi.RouteRestriction.HIGHWAYS,
						DirectionsApi.RouteRestriction.TOLLS,
						DirectionsApi.RouteRestriction.FERRIES
				)
				.units(Unit.IMPERIAL)
				.region("us")
				.origin(originPlaceId)
				.destination(destinationPlaceId);

		DirectionsResult directionsResult;

		try {
			directionsResult = request.await();
		} catch (ApiException | InterruptedException | IOException e) {
			throw new RuntimeException(format("An error occurred while attempting to get directions from " +
					"origin place ID %s to destination place ID %s", originPlaceId, destinationPlaceId), e);
		}

		System.out.println(new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(directionsResult));
	}

	@Nonnull
	@Override
	public void route(@Nonnull String originPlaceId,
										@Nonnull String destinationPlaceId) {
		requireNonNull(originPlaceId);
		requireNonNull(destinationPlaceId);

		// RoutesClient routesClient = RoutesClient.create();

//
//		ComputeRoutesRequest request = ComputeRoutesRequest.newBuilder()
//				.setOrigin(createWaypointForLatLng(37.420761, -122.081356))
//				.setDestination(createWaypointForLatLng(37.420999, -122.086894)).setTravelMode(RouteTravelMode.DRIVE)
//				.setRoutingPreference(RoutingPreference.TRAFFIC_AWARE).setComputeAlternativeRoutes(true)
//				.setRouteModifiers(
//						RouteModifiers.newBuilder().setAvoidTolls(false).setAvoidHighways(true).setAvoidFerries(true))
//				.setPolylineQuality(PolylineQuality.OVERVIEW).build();
//		ComputeRoutesResponse response;
//		try {
//			logger.info("About to send request: " + request.toString());
//			response = blockingStub.withDeadlineAfter(2000, TimeUnit.MILLISECONDS).computeRoutes(request);
//		} catch (StatusRuntimeException e) {
//			logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
//			return;
//		}
//		logger.info("Response: " + response.toString());
	}

	@Nonnull
	protected GoogleCredentials acquireGoogleCredentials(@Nonnull String serviceAccountPrivateKeyJson) {
		requireNonNull(serviceAccountPrivateKeyJson);

		try (InputStream inputStream = new ByteArrayInputStream(serviceAccountPrivateKeyJson.getBytes(StandardCharsets.UTF_8))) {
			return ServiceAccountCredentials.fromStream(inputStream);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Nonnull
	protected PlacesClient createPlacesClient(@Nonnull GoogleCredentials googleCredentials) {
		requireNonNull(googleCredentials);

		try {
			return PlacesClient.create(PlacesSettings.newBuilder()
					.setCredentialsProvider(FixedCredentialsProvider.create(googleCredentials))
					// TODO: figure out if we can specify these headers per-call instead of per-client
					// See https://developers.google.com/maps/documentation/places/web-service/place-details for options
					.setHeaderProvider(new HeaderProvider() {
						final String FIELD_MASK_HEADER_VALUE = List.of(
								"places.id",
								"places.name",
								"places.addressComponents",
								"places.formattedAddress",
								"places.location",
								"places.googleMapsUri"
						).stream().collect(Collectors.joining(","));

						@Override
						public Map<String, String> getHeaders() {
							return Map.of("X-Goog-FieldMask", FIELD_MASK_HEADER_VALUE);
						}
					})
					.build());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Nonnull
	protected RoutesClient createRoutesClient(@Nonnull GoogleCredentials googleCredentials) {
		requireNonNull(googleCredentials);

		try {
			return RoutesClient.create(RoutesSettings.newBuilder()
					.setCredentialsProvider(FixedCredentialsProvider.create(googleCredentials))
					.build());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Nonnull
	protected GoogleCredentials getGoogleCredentials() {
		return this.googleCredentials;
	}

	@Nonnull
	protected String getMapsPlatformApiKey() {
		return this.mapsPlatformApiKey;
	}

	@Nonnull
	protected GeoApiContext getGeoApiContext() {
		return this.geoApiContext;
	}

	@Nonnull
	protected String getProjectId() {
		return this.projectId;
	}

	@Nonnull
	protected PlacesClient getPlacesClient() {
		return this.placesClient;
	}

	@Nonnull
	protected RoutesClient getRoutesClient() {
		return this.routesClient;
	}
}
