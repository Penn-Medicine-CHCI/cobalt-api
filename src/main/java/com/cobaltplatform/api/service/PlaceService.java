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

import com.cobaltplatform.api.Configuration;
import com.cobaltplatform.api.error.ErrorReporter;
import com.cobaltplatform.api.integration.google.DefaultGoogleGeoClient;
import com.cobaltplatform.api.integration.google.GoogleGeoClient;
import com.cobaltplatform.api.model.db.Address;
import com.cobaltplatform.api.model.places.PlacePrediction;
import com.cobaltplatform.api.util.db.DatabaseProvider;
import com.google.maps.PlacesApi;
import com.google.maps.model.PlaceDetails;
import com.google.maps.places.v1.AutocompletePlacesRequest;
import com.google.maps.places.v1.AutocompletePlacesResponse;
import com.google.maps.places.v1.GetPlaceRequest;
import com.google.maps.places.v1.Place;
import com.google.maps.places.v1.SearchTextRequest;
import com.google.maps.places.v1.SearchTextResponse;
import com.lokalized.Strings;
import com.pyranid.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Singleton
@ThreadSafe
public class PlaceService {
	@Nonnull
	private final DatabaseProvider databaseProvider;
	@Nonnull
	private final ErrorReporter errorReporter;
	@Nonnull
	private final Logger logger;
	@Nonnull
	private final Configuration configuration;

	@Inject
	public PlaceService(@Nonnull DatabaseProvider databaseProvider,
											@Nonnull ErrorReporter errorReporter,
											@Nonnull Strings strings,
											@Nonnull Configuration configuration) {
		requireNonNull(databaseProvider);
		requireNonNull(errorReporter);
		requireNonNull(strings);
		requireNonNull(configuration);

		this.databaseProvider = databaseProvider;
		this.errorReporter = errorReporter;
		this.logger = LoggerFactory.getLogger(getClass());
		this.configuration = configuration;
	}

	@Nonnull
	protected GoogleGeoClient acquireGoogleGeoClient() {
		try {
			Path mapsPlatformApiKeyFile = Path.of(getConfiguration().getMapsPlatformApiKeyFilename());
			Path geoServiceAccountPrivateKeyFile = Path.of(getConfiguration().getGeoServiceAccountPrivateKeyFilename());

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

	@Nonnull
	public List<PlacePrediction> autocompletePlace(@Nullable String searchTerm) {
		requireNonNull(searchTerm);

		if (searchTerm == null)
			return Collections.emptyList();

		List<PlacePrediction> placePredictions = new ArrayList<>();

		try (GoogleGeoClient googleGeoClient = acquireGoogleGeoClient()) {
			AutocompletePlacesResponse response = googleGeoClient.autocompletePlaces(AutocompletePlacesRequest.newBuilder()
					.setInput(searchTerm)
					.setLanguageCode("en")
					.setRegionCode("US")
					.build());

			for (AutocompletePlacesResponse.Suggestion suggestion : response.getSuggestionsList()) {
				PlacePrediction placePrediction = new PlacePrediction();

				placePrediction.setPlaceId(suggestion.getPlacePrediction().getPlaceId());
				placePrediction.setText(suggestion.getPlacePrediction().getText().getText());
				placePredictions.add(placePrediction);
			}
		} catch (Exception e) {
			getLogger().warn(format("Unable to complete place autocomplete for search term %s", searchTerm), e);
		}

		return placePredictions;
	}

	@Nonnull
	public Place findPlaceByPlaceId(@Nullable String placeIdRequest) {
		requireNonNull(placeIdRequest);
		Place place = null;

		try (GoogleGeoClient googleGeoClient = acquireGoogleGeoClient()) {
			String placeId = format("places/%s", placeIdRequest);

			GetPlaceRequest request = GetPlaceRequest.newBuilder()
					.setName(placeId)
					.setLanguageCode("en")
					.setRegionCode("US")
					.build();

			place = googleGeoClient.getPlace(request).orElse(null);
		} catch (Exception e) {
			getLogger().warn(format("Unable to find place for place ID %s", placeIdRequest), e);
		}

		return place;
	}

	@Nonnull
	public Place findPlaceByPlaceAddress(@Nullable Address address) {
		requireNonNull(address);
		Place place = null;

		try (GoogleGeoClient googleGeoClient = acquireGoogleGeoClient()) {
			SearchTextRequest searchTextRequest = SearchTextRequest.newBuilder()
					.setTextQuery(format("%s, %s, %s, %s", address.getStreetAddress1(), address.getLocality(), address.getRegion(), address.getPostalCode()))
					.setLanguageCode("en")
					.setRegionCode("US")
					.build();

			SearchTextResponse response = googleGeoClient.findPlacesBySearchText(searchTextRequest);

			if (response.getPlacesList().size() > 0)
				place =  response.getPlacesList().get(0);

		} catch (Exception e) {
			getLogger().warn(format("Unable to find place for address ID %s", address.getAddressId()), e);
		}

		return place;
	}

	@Nonnull
	protected Database getDatabase() {
		return this.databaseProvider.get();
	}

	@Nonnull
	protected ErrorReporter getErrorReporter() {
		return this.errorReporter;
	}

	@Nonnull
	protected Logger getLogger() {
		return this.logger;
	}

	@Nonnull
	public Configuration getConfiguration() {
		return configuration;
	}
}