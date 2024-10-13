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
			// TODO: finish up
			googleGeoClient.geocode("112 Fayette St 2nd Fl Conshohocken PA 19428");
		}
	}

	@Test
	public void testFindPlacesBySearchText() throws Exception {
		try (GoogleGeoClient googleGeoClient = acquireGoogleGeoClient()) {
			PlaceSearchTextRequest request = new PlaceSearchTextRequest();
			request.setTextQuery("Transmogrify LLC 112 Fayette St Conshohocken PA 19428");
			request.setLanguageCode("en");
			request.setRegionCode("US");

			PlaceSearchTextResponse response = googleGeoClient.findPlacesBySearchText(request);

			Assert.assertTrue("No places found", response.getNormalizedPlaces().size() > 0);
		}
	}

	@Test
	public void testRoute() throws Exception {
		try (GoogleGeoClient googleGeoClient = acquireGoogleGeoClient()) {
			String originPlaceId = "ChIJ97AN5zC-xokRyT9EnG7aPzc";
			String destinationPlaceId = "ChIJc_ABK2G-xokRVVrQ0ZZOb9c";

			// TODO: finish up
			googleGeoClient.route(originPlaceId, destinationPlaceId);
		}
	}

	@Test
	public void testDirections() throws Exception {
		try (GoogleGeoClient googleGeoClient = acquireGoogleGeoClient()) {
			String originPlaceId = "ChIJ97AN5zC-xokRyT9EnG7aPzc";
			String destinationPlaceId = "ChIJc_ABK2G-xokRVVrQ0ZZOb9c";

			// TODO: finish up
			googleGeoClient.directions(originPlaceId, destinationPlaceId);
		}
	}

	@Test
	public void testAutocomplete() throws Exception {
		try (GoogleGeoClient googleGeoClient = acquireGoogleGeoClient()) {
			// TODO: finish up
			googleGeoClient.autocompletePlaces("Transmogri");
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

