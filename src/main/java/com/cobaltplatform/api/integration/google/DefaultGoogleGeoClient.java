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

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.gax.rpc.HeaderProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.common.io.CharStreams;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApiRequest;
import com.google.maps.errors.ApiException;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.GeocodingResult;
import com.google.maps.places.v1.AutocompletePlacesRequest;
import com.google.maps.places.v1.AutocompletePlacesResponse;
import com.google.maps.places.v1.GetPlaceRequest;
import com.google.maps.places.v1.Place;
import com.google.maps.places.v1.PlacesClient;
import com.google.maps.places.v1.PlacesSettings;
import com.google.maps.places.v1.SearchTextRequest;
import com.google.maps.places.v1.SearchTextResponse;
import com.google.maps.routing.v2.ComputeRoutesRequest;
import com.google.maps.routing.v2.ComputeRoutesResponse;
import com.google.maps.routing.v2.RoutesClient;
import com.google.maps.routing.v2.RoutesSettings;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
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
	private final PlacesClient placesClientForSearchText; // Multiple PlacesClient instances because X-Goog-FieldMask is different between SearchText and Autocomplete
	@Nonnull
	private final PlacesClient placesClientForAutocomplete; // Multiple PlacesClient instances because X-Goog-FieldMask is different between SearchText and Autocomplete
	@Nonnull
	private final PlacesClient placesClientForGetPlace; // Multiple PlacesClient instances because X-Goog-FieldMask is different between SearchText and Autocomplete
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
			this.placesClientForSearchText = createPlacesClientForSearchText(this.googleCredentials);
			this.placesClientForAutocomplete = createPlacesClientForAutocomplete(this.googleCredentials);
			this.placesClientForGetPlace = createPlacesClientForGetPlace(this.googleCredentials);
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
			getPlacesClientForSearchText().close();
		} catch (Exception ignored) {
			// Do nothing
		}

		try {
			getPlacesClientForAutocomplete().close();
		} catch (Exception ignored) {
			// Do nothing
		}

		try {
			getPlacesClientForGetPlace().close();
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
	public List<GeocodingResult> geocode(@Nonnull Function<GeoApiContext, GeocodingApiRequest> geocodingApiRequestProvider) {
		requireNonNull(geocodingApiRequestProvider);

		GeocodingApiRequest request = geocodingApiRequestProvider.apply(getGeoApiContext());

		if (request == null)
			throw new NullPointerException("Geocoding API Request was null");

		try {
			GeocodingResult[] geocodingResults = request.await();
			return Arrays.stream(geocodingResults).toList();
		} catch (ApiException | InterruptedException | IOException e) {
			throw new RuntimeException(format("An error occurred while attempting to geocode request '%s'", request), e);
		}
	}

	@Nonnull
	@Override
	public AutocompletePlacesResponse autocompletePlaces(@Nonnull AutocompletePlacesRequest request) {
		requireNonNull(request);
		return getPlacesClientForAutocomplete().autocompletePlaces(request);
	}

	@Nonnull
	@Override
	public SearchTextResponse findPlacesBySearchText(@Nonnull SearchTextRequest searchTextRequest) {
		requireNonNull(searchTextRequest);
		return getPlacesClientForSearchText().searchText(searchTextRequest);
	}

	@Nonnull
	@Override
	public DirectionsResult directions(@Nonnull Function<GeoApiContext, DirectionsApiRequest> directionsApiRequestProvider) {
		requireNonNull(directionsApiRequestProvider);

		DirectionsApiRequest request = directionsApiRequestProvider.apply(getGeoApiContext());

		if (request == null)
			throw new NullPointerException("Directions API Request was null");

		try {
			return request.await();
		} catch (ApiException | InterruptedException | IOException e) {
			throw new RuntimeException(format("An error occurred while attempting to perform directions request '%s'", request), e);
		}
	}

	@Nonnull
	@Override
	public ComputeRoutesResponse computeRoutes(@Nonnull ComputeRoutesRequest request) {
		requireNonNull(request);
		return getRoutesClient().computeRoutes(request);
	}

	@Nonnull
	@Override
	public Optional<Place> getPlace(@Nonnull GetPlaceRequest request) {
		requireNonNull(request);
		return Optional.ofNullable(getPlacesClientForGetPlace().getPlace(request));
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
	protected PlacesClient createPlacesClientForGetPlace(@Nonnull GoogleCredentials googleCredentials) {
		requireNonNull(googleCredentials);

		try {
			return PlacesClient.create(PlacesSettings.newBuilder()
					.setCredentialsProvider(FixedCredentialsProvider.create(googleCredentials))
					// TODO: figure out if we can specify these headers per-call instead of per-client
					// See https://developers.google.com/maps/documentation/places/web-service/place-details for options
					.setHeaderProvider(new HeaderProvider() {
						final String FIELD_MASK_HEADER_VALUE = List.of(
								"id",
								"name",
								"addressComponents",
								"formattedAddress",
								"location",
								"googleMapsUri",
								"displayName"
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
	protected PlacesClient createPlacesClientForSearchText(@Nonnull GoogleCredentials googleCredentials) {
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
								"places.googleMapsUri",
								"places.displayName"
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
	protected PlacesClient createPlacesClientForAutocomplete(@Nonnull GoogleCredentials googleCredentials) {
		requireNonNull(googleCredentials);

		try {
			return PlacesClient.create(PlacesSettings.newBuilder()
					.setCredentialsProvider(FixedCredentialsProvider.create(googleCredentials))
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
					// TODO: figure out if we can specify these headers per-call instead of per-client
					// See https://developers.google.com/maps/documentation/routes/choose_fields for options
					.setHeaderProvider(new HeaderProvider() {
						final String FIELD_MASK_HEADER_VALUE = List.of(
								"routes.distanceMeters",
								"routes.duration"
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
	@Override
	public String getMapsPlatformApiKey() {
		return this.mapsPlatformApiKey;
	}

	@Nonnull
	public String getProjectId() {
		return this.projectId;
	}

	@Nonnull
	protected GoogleCredentials getGoogleCredentials() {
		return this.googleCredentials;
	}

	@Nonnull
	protected GeoApiContext getGeoApiContext() {
		return this.geoApiContext;
	}

	@Nonnull
	protected PlacesClient getPlacesClientForSearchText() {
		return this.placesClientForSearchText;
	}

	@Nonnull
	protected PlacesClient getPlacesClientForAutocomplete() {
		return this.placesClientForAutocomplete;
	}

	@Nonnull
	protected PlacesClient getPlacesClientForGetPlace() {
		return this.placesClientForGetPlace;
	}

	@Nonnull
	protected RoutesClient getRoutesClient() {
		return this.routesClient;
	}
}
