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

import com.cobaltplatform.api.integration.epic.request.GetProviderAppointmentsRequest;
import com.cobaltplatform.api.integration.epic.response.GetProviderAppointmentsResponse;
import com.cobaltplatform.api.util.GsonUtility;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
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

	@Test
	public void testProviderSlotBlocks() throws Exception {
		EpicTestConfiguration epicTestConfiguration = epicTestConfigurationFromFile(Path.of("resources/test/epic-ic-prod-credentials.json"));
		EpicConfiguration epicConfiguration = new EpicConfiguration.Builder(
				epicTestConfiguration.getEpicEmpCredentials(),
				epicTestConfiguration.getEpicEmpCredentials().getClientId(),
				epicTestConfiguration.getBaseUrl()
		).build();

		EpicClient epicClient = new DefaultEpicClient(epicConfiguration);
		ProviderForSlotBlocks providerForSlotBlocks = epicTestConfiguration.getExtraConfiguration().getProviderForSlotBlocks();

		GetProviderAppointmentsRequest.Provider provider = new GetProviderAppointmentsRequest.Provider();
		provider.setID(providerForSlotBlocks.getId());
		provider.setIDType(providerForSlotBlocks.getIdType());
		provider.setDepartmentID(providerForSlotBlocks.getDepartmentId());
		provider.setDepartmentIDType(providerForSlotBlocks.getDepartmentIdType());

		GetProviderAppointmentsRequest providerAppointmentsRequest = new GetProviderAppointmentsRequest();
		providerAppointmentsRequest.setUserID(epicConfiguration.getEpicEmpCredentials().get().getUserId());
		providerAppointmentsRequest.setUserIDType(epicConfiguration.getEpicEmpCredentials().get().getUserIdType());
		providerAppointmentsRequest.setProviders(List.of(provider));
		providerAppointmentsRequest.setStartDate(epicClient.formatDateWithSlashes(providerForSlotBlocks.getStartDate()));
		providerAppointmentsRequest.setEndDate(epicClient.formatDateWithSlashes(providerForSlotBlocks.getEndDate()));

		GetProviderAppointmentsResponse providerAppointmentsResponse = epicClient.getProviderAppointments(providerAppointmentsRequest);

		// TODO: run tests

		// 	@Nonnull
		//	GetProviderAppointmentsResponse getProviderAppointments(@Nonnull GetProviderAppointmentsRequest request);
		//
		//	// epic/2012/Scheduling/Provider/GETPROVIDERAVAILABILITY/Availability
		//	@Nonnull
		//	GetProviderAvailabilityResponse getProviderAvailability(@Nonnull GetProviderAvailabilityRequest request);
		//
		//	// epic/2017/PatientAccess/External/GetScheduleDaysForProvider2/Scheduling/Open/Provider/GetScheduleDays2
		//	@Nonnull
		//	GetScheduleDaysForProviderResponse getScheduleDaysForProvider(@Nonnull GetScheduleDaysForProviderRequest request);
	}

	@Nonnull
	protected EpicTestConfiguration epicTestConfigurationFromFile(@Nonnull Path file) {
		requireNonNull(file);

		if (!Files.exists(file))
			throw new IllegalArgumentException(format("No such file %s", file.toAbsolutePath()));

		GsonBuilder gsonBuilder = new GsonBuilder()
				.setPrettyPrinting()
				.disableHtmlEscaping();

		GsonUtility.applyDefaultTypeAdapters(gsonBuilder);

		Gson gson = gsonBuilder.create();

		try {
			String credentialsJson = Files.readString(file, StandardCharsets.UTF_8);
			Map<String, Object> credentials = gson.fromJson(credentialsJson, new TypeToken<Map<String, Object>>() {
				// Ignored
			});

			String clientId = (String) credentials.get("clientId");
			String userId = (String) credentials.get("userId");
			String userIdType = (String) credentials.get("userIdType");
			String username = (String) credentials.get("username");
			String password = (String) credentials.get("password");
			String baseUrl = (String) credentials.get("baseUrl");

			EpicEmpCredentials epicEmpCredentials = new EpicEmpCredentials(clientId, userId, userIdType, username, password);

			Map<String, Object> rawExtraConfiguration = (Map<String, Object>) credentials.get("extraConfiguration");
			ExtraConfiguration extraConfiguration = new ExtraConfiguration();

			if (rawExtraConfiguration != null)
				extraConfiguration = gson.fromJson(gson.toJson(rawExtraConfiguration), ExtraConfiguration.class);

			return new EpicTestConfiguration(epicEmpCredentials, baseUrl, extraConfiguration);
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
		@Nonnull
		private final ExtraConfiguration extraConfiguration;

		public EpicTestConfiguration(@Nonnull EpicEmpCredentials epicEmpCredentials,
																 @Nonnull String baseUrl,
																 @Nonnull ExtraConfiguration extraConfiguration) {
			requireNonNull(epicEmpCredentials);
			requireNonNull(baseUrl);
			requireNonNull(extraConfiguration);

			this.epicEmpCredentials = epicEmpCredentials;
			this.baseUrl = baseUrl;
			this.extraConfiguration = extraConfiguration;
		}

		@Nonnull
		public EpicEmpCredentials getEpicEmpCredentials() {
			return this.epicEmpCredentials;
		}

		@Nonnull
		public String getBaseUrl() {
			return this.baseUrl;
		}

		@Nonnull
		public ExtraConfiguration getExtraConfiguration() {
			return this.extraConfiguration;
		}
	}

	@NotThreadSafe
	private static class ExtraConfiguration {
		@Nullable
		private ProviderForSlotBlocks providerForSlotBlocks;

		@Nullable
		public ProviderForSlotBlocks getProviderForSlotBlocks() {
			return this.providerForSlotBlocks;
		}

		public void setProviderForSlotBlocks(@Nullable ProviderForSlotBlocks providerForSlotBlocks) {
			this.providerForSlotBlocks = providerForSlotBlocks;
		}
	}

	@NotThreadSafe
	private static class ProviderForSlotBlocks {
		@Nullable
		private String name;
		@Nullable
		private String id;
		@Nullable
		private String idType;
		@Nullable
		private String departmentId;
		@Nullable
		private String departmentIdType;
		@Nullable
		private LocalDate startDate;
		@Nullable
		private LocalDate endDate;

		@Nullable
		public String getName() {
			return this.name;
		}

		public void setName(@Nullable String name) {
			this.name = name;
		}

		@Nullable
		public String getId() {
			return this.id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public String getIdType() {
			return this.idType;
		}

		public void setIdType(@Nullable String idType) {
			this.idType = idType;
		}

		@Nullable
		public String getDepartmentId() {
			return this.departmentId;
		}

		public void setDepartmentId(@Nullable String departmentId) {
			this.departmentId = departmentId;
		}

		@Nullable
		public String getDepartmentIdType() {
			return this.departmentIdType;
		}

		public void setDepartmentIdType(@Nullable String departmentIdType) {
			this.departmentIdType = departmentIdType;
		}

		@Nullable
		public LocalDate getStartDate() {
			return this.startDate;
		}

		public void setStartDate(@Nullable LocalDate startDate) {
			this.startDate = startDate;
		}

		@Nullable
		public LocalDate getEndDate() {
			return this.endDate;
		}

		public void setEndDate(@Nullable LocalDate endDate) {
			this.endDate = endDate;
		}
	}
}
