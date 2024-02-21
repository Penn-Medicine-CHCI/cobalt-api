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

package com.cobaltplatform.api.integration.epic;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
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
import java.util.Map;

import static com.soklet.util.LoggingUtils.initializeLogback;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class EpicIntegratedCareTests {
	@BeforeClass
	public static void initialize() {
		initializeLogback(Paths.get("config/local/logback.xml"));
	}

	@Test
	public void testIcPatientRead() throws Exception {
		EpicTestConfiguration epicTestConfiguration = epicTestConfigurationFromFile(Path.of("resources/test/epic-ic-tst-internal-credentials.json"));
		EpicConfiguration epicConfiguration = new EpicConfiguration.Builder(
				epicTestConfiguration.getEpicEmpCredentials(),
				epicTestConfiguration.getEpicEmpCredentials().getClientId(),
				epicTestConfiguration.getBaseUrl()
		).build();

		EpicClient epicClient = new DefaultEpicClient(epicConfiguration);
		epicClient.patientSearchFhirR4("UID", "8641707922");
	}

	@Test
	public void testIcEncounterSearch() throws Exception {
		EpicTestConfiguration epicTestConfiguration = epicTestConfigurationFromFile(Path.of("resources/test/epic-ic-tst-internal-credentials.json"));
		EpicConfiguration epicConfiguration = new EpicConfiguration.Builder(
				epicTestConfiguration.getEpicEmpCredentials(),
				epicTestConfiguration.getEpicEmpCredentials().getClientId(),
				epicTestConfiguration.getBaseUrl()
		).build();

		EpicClient epicClient = new DefaultEpicClient(epicConfiguration);
		epicClient.encounterSearchFhirR4("eEMS.-CbIiYrvQbAjuUYABA3");
	}

	@Nonnull
	protected EpicTestConfiguration epicTestConfigurationFromFile(@Nonnull Path file) {
		requireNonNull(file);

		if (!Files.exists(file))
			throw new IllegalArgumentException(format("No such file %s", file.toAbsolutePath()));

		try {
			String credentialsJson = Files.readString(file, StandardCharsets.UTF_8);
			Map<String, String> credentials = new Gson().fromJson(credentialsJson, new TypeToken<Map<String, String>>() {
				// Ignored
			});

			String clientId = credentials.get("clientId");
			String userId = credentials.get("userId");
			String userIdType = credentials.get("userIdType");
			String username = credentials.get("username");
			String password = credentials.get("password");
			String baseUrl = credentials.get("baseUrl");

			EpicEmpCredentials epicEmpCredentials = new EpicEmpCredentials(clientId, userId, userIdType, username, password);

			return new EpicTestConfiguration(epicEmpCredentials, baseUrl);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@ThreadSafe
	private static class EpicTestConfiguration {
		@Nonnull
		private final EpicEmpCredentials epicEmpCredentials;
		@Nonnull
		private final String baseUrl;

		public EpicTestConfiguration(@Nonnull EpicEmpCredentials epicEmpCredentials,
																 @Nonnull String baseUrl) {
			requireNonNull(epicEmpCredentials);
			requireNonNull(baseUrl);

			this.epicEmpCredentials = epicEmpCredentials;
			this.baseUrl = baseUrl;
		}

		@Nonnull
		public EpicEmpCredentials getEpicEmpCredentials() {
			return this.epicEmpCredentials;
		}

		@Nonnull
		public String getBaseUrl() {
			return this.baseUrl;
		}
	}
}
