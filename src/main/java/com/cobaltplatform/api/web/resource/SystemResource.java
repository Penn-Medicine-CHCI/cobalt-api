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

package com.cobaltplatform.api.web.resource;

import com.cobaltplatform.api.Configuration;
import com.cobaltplatform.api.cache.Cache;
import com.cobaltplatform.api.cache.DistributedCache;
import com.cobaltplatform.api.cache.LocalCache;
import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.integration.acuity.AcuitySchedulingCache;
import com.cobaltplatform.api.integration.acuity.AcuitySchedulingClient;
import com.cobaltplatform.api.integration.epic.EpicSyncManager;
import com.cobaltplatform.api.messaging.email.EmailMessageManager;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.Role.RoleId;
import com.cobaltplatform.api.model.security.AuthenticationRequired;
import com.cobaltplatform.api.service.SystemService;
import com.cobaltplatform.api.service.Way2HealthService;
import com.cobaltplatform.api.util.Authenticator;
import com.cobaltplatform.api.util.CryptoUtility;
import com.cobaltplatform.api.util.CryptoUtility.KeyFormat;
import com.cobaltplatform.api.util.Formatter;
import com.cobaltplatform.api.web.response.ResponseGenerator;
import com.lokalized.Strings;
import com.soklet.web.annotation.GET;
import com.soklet.web.annotation.POST;
import com.soklet.web.annotation.PUT;
import com.soklet.web.annotation.QueryParameter;
import com.soklet.web.annotation.Resource;
import com.soklet.web.exception.AuthorizationException;
import com.soklet.web.exception.NotFoundException;
import com.soklet.web.response.ApiResponse;
import com.soklet.web.response.BinaryResponse;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.security.PublicKey;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Resource
@Singleton
@ThreadSafe
public class SystemResource {
	@Nonnull
	private final SystemService systemService;
	@Nonnull
	private final EmailMessageManager emailMessageManager;
	@Nonnull
	private final Configuration configuration;
	@Nonnull
	private final Cache localCache;
	@Nonnull
	private final Cache distributedCache;
	@Nonnull
	private final Authenticator authenticator;
	@Nonnull
	private final AcuitySchedulingCache acuitySchedulingCache;
	@Nonnull
	private final AcuitySchedulingClient acuitySchedulingClient;
	@Nonnull
	private final EpicSyncManager epicSyncManager;
	@Nonnull
	private final Way2HealthService way2HealthService;
	@Nonnull
	private final Provider<CurrentContext> currentContextProvider;
	@Nonnull
	private final Formatter formatter;
	@Nonnull
	private final Strings strings;

	@Inject
	public SystemResource(@Nonnull SystemService systemService,
												@Nonnull EmailMessageManager emailMessageManager,
												@Nonnull Configuration configuration,
												@Nonnull @LocalCache Cache localCache,
												@Nonnull @DistributedCache Cache distributedCache,
												@Nonnull Authenticator authenticator,
												@Nonnull AcuitySchedulingCache acuitySchedulingCache,
												@Nonnull AcuitySchedulingClient acuitySchedulingClient,
												@Nonnull EpicSyncManager epicSyncManager,
												@Nonnull Way2HealthService way2HealthService,
												@Nonnull Provider<CurrentContext> currentContextProvider,
												@Nonnull Formatter formatter,
												@Nonnull Strings strings) {
		requireNonNull(systemService);
		requireNonNull(emailMessageManager);
		requireNonNull(configuration);
		requireNonNull(localCache);
		requireNonNull(distributedCache);
		requireNonNull(authenticator);
		requireNonNull(acuitySchedulingCache);
		requireNonNull(acuitySchedulingClient);
		requireNonNull(epicSyncManager);
		requireNonNull(way2HealthService);
		requireNonNull(currentContextProvider);
		requireNonNull(formatter);
		requireNonNull(strings);

		this.systemService = systemService;
		this.emailMessageManager = emailMessageManager;
		this.configuration = configuration;
		this.localCache = localCache;
		this.distributedCache = distributedCache;
		this.authenticator = authenticator;
		this.acuitySchedulingCache = acuitySchedulingCache;
		this.acuitySchedulingClient = acuitySchedulingClient;
		this.epicSyncManager = epicSyncManager;
		this.way2HealthService = way2HealthService;
		this.currentContextProvider = currentContextProvider;
		this.formatter = formatter;
		this.strings = strings;
	}

	@Nonnull
	@GET("/")
	public BinaryResponse index() {
		return ResponseGenerator.utf8Response(getStrings().get("Cobalt Backend"), "text/plain");
	}

	@GET("/favicon.ico")
	public void favicon() {
		throw new NotFoundException();
	}

	@Nonnull
	@GET("/system/configuration")
	public ApiResponse systemConfiguration() {
		ZoneId displayTimezone = ZoneId.of("America/New_York");

		return new ApiResponse(new HashMap<String, Object>() {{
			put("environment", getConfiguration().getEnvironment());
			put("buildTimestamp", getConfiguration().getBuildTimestamp());
			put("buildTimestampDescription", getFormatter().formatTimestamp(getConfiguration().getBuildTimestamp(), FormatStyle.LONG, FormatStyle.LONG, displayTimezone));
			put("deploymentTimestamp", getConfiguration().getDeploymentTimestamp());
			put("deploymentTimestampDescription", getFormatter().formatTimestamp(getConfiguration().getDeploymentTimestamp(), FormatStyle.LONG, FormatStyle.LONG, displayTimezone));
			put("uptime", getFormatter().formatDuration(Duration.between(getConfiguration().getDeploymentTimestamp(), Instant.now())));
			put("nodeIdentifier", getConfiguration().getNodeIdentifier());
			// put("gitBranch", getConfiguration().getGitBranch());
			// put("gitCommitHash", getConfiguration().getGitCommitHash());
		}});
	}

	@Nonnull
	@GET("/system/health-check")
	public BinaryResponse healthCheck() {
		return ResponseGenerator.utf8Response("OK", "text/plain");
	}

	@Nonnull
	@GET("/system/local-cache")
	public ApiResponse localCache() {
		List<String> cacheKeys = new ArrayList<>(getLocalCache().getKeys());
		Collections.sort(cacheKeys);

		return new ApiResponse(new HashMap<String, Object>() {{
			put("cacheKeys", cacheKeys);
		}});
	}

	@Nonnull
	@GET("/system/distributed-cache")
	public ApiResponse distributedCache() {
		if (!getConfiguration().getShouldEnableCacheDebugging())
			throw new UnsupportedOperationException("Cache debugging is disabled in this environment");

		List<String> cacheKeys = new ArrayList<>(getDistributedCache().getKeys());
		Collections.sort(cacheKeys);

		return new ApiResponse(new HashMap<String, Object>() {{
			put("cacheKeys", cacheKeys);
		}});
	}

	@Nonnull
	@PUT("/system/local-cache/invalidate")
	public ApiResponse invalidateLocalCache() {
		getLocalCache().invalidateAll();
		return new ApiResponse(204);
	}

	@Nonnull
	@PUT("/system/distributed-cache/invalidate")
	public ApiResponse invalidateDistributedCache() {
		getDistributedCache().invalidateAll();
		return new ApiResponse(204);
	}

	@Nonnull
	@GET("/system/acuity-scheduling-cache/availability-classes")
	public ApiResponse acuitySchedulingCacheAvailabilityClasses() {
		SortedSet<String> cacheKeys = new TreeSet<>(getAcuitySchedulingCache().getAvailabilityClassesCacheKeys());

		return new ApiResponse(new HashMap<String, Object>() {{
			put("cacheKeys", cacheKeys);
		}});
	}

	@Nonnull
	@PUT("/system/acuity-scheduling-cache/availability-classes/invalidate")
	public ApiResponse invalidateAcuitySchedulingCacheAvailabilityClasses() {
		getAcuitySchedulingCache().invalidateAvailabilityClassesCache();
		return new ApiResponse(204);
	}

	@Nonnull
	@GET("/system/acuity-scheduling-cache/availability-times")
	public ApiResponse acuitySchedulingCacheAvailabilityTimes() {
		SortedSet<String> cacheKeys = new TreeSet<>(getAcuitySchedulingCache().getAvailabilityTimesCacheKeys());

		return new ApiResponse(new HashMap<String, Object>() {{
			put("cacheKeys", cacheKeys);
		}});
	}

	@Nonnull
	@PUT("/system/acuity-scheduling-cache/availability-times/invalidate")
	public ApiResponse invalidateAcuitySchedulingCacheAvailabilityTimes() {
		getAcuitySchedulingCache().invalidateAvailabilityTimesCache();
		return new ApiResponse(204);
	}

	@Nonnull
	@GET("/system/acuity-scheduling/call-frequency-histogram")
	public ApiResponse acuityCallFrequencyHistogram() {
		SortedMap<String, Long> callFrequencyHistogram = getAcuitySchedulingClient().getCallFrequencyHistogram();

		return new ApiResponse(new HashMap<String, Object>() {{
			put("callFrequencyHistogram", callFrequencyHistogram);
		}});
	}

	@Nonnull
	@GET("/system/epic/sync-provider")
	public ApiResponse epicSyncProvider(@Nonnull @QueryParameter UUID providerId,
																			@Nonnull @QueryParameter LocalDate date) {
		getEpicSyncManager().syncProviderAvailability(providerId, date, true);
		return new ApiResponse();
	}

	@Nonnull
	@POST("/system/test-exception")
	public ApiResponse testException() {
		throw new RuntimeException("This is a manually-triggered exception");
	}

	@Nonnull
	@GET("/system/public-key")
	public ApiResponse publicKey() {
		PublicKey publicKey = getConfiguration().getKeyPair().getPublic();

		return new ApiResponse(new HashMap<String, Object>() {{
			put("algorithm", publicKey.getAlgorithm());
			put("jcaAlgorithm", getAuthenticator().getJcaSignatureAlgorithm());
			put("format", publicKey.getFormat());
			put("publicKey", CryptoUtility.stringRepresentation(publicKey, KeyFormat.BASE64));
		}});
	}

	/**
	 * Special endpoint to force re-sync of old availability.  A one-time thing in production to backfill for analytics.
	 */
	@Nonnull
	@POST("/system/sync-past-provider-availability")
	@AuthenticationRequired
	public ApiResponse syncPastProviderAvailability(@Nonnull @QueryParameter Optional<LocalDate> startingAtDate,
																									@Nonnull @QueryParameter Optional<UUID> providerId) {
		requireNonNull(startingAtDate);
		requireNonNull(providerId);

		if (getCurrentContext().getAccount().get().getRoleId() != RoleId.ADMINISTRATOR)
			throw new AuthorizationException();

		// This is slow, do it on a background thread
		new Thread() {
			@Override
			public void run() {
				getSystemService().syncPastProviderAvailability(InstitutionId.COBALT, startingAtDate.orElse(null));
			}
		}.start();

		return new ApiResponse();
	}

	/**
	 * Special endpoint to force re-sync of availability.
	 */
	@Nonnull
	@POST("/system/sync-provider-availability")
	@AuthenticationRequired
	public ApiResponse syncProviderAvailability(@Nonnull @QueryParameter UUID providerId,
																							@Nonnull @QueryParameter Optional<LocalDate> startingAtDate,
																							@Nonnull @QueryParameter Optional<LocalDate> endingAtDate) {
		requireNonNull(providerId);
		requireNonNull(startingAtDate);
		requireNonNull(endingAtDate);

		if (getCurrentContext().getAccount().get().getRoleId() != RoleId.ADMINISTRATOR)
			throw new AuthorizationException();

		// This is slow, do it on a background thread
		new Thread() {
			@Override
			public void run() {
				getSystemService().syncProviderAvailability(providerId, startingAtDate.orElse(null), endingAtDate.orElse(null));
			}
		}.start();

		return new ApiResponse();
	}

	/**
	 * Simplify Way2Health testing by permitting the mock client to be "reset" so incidents can be re-fetched.
	 *
	 * @return API response (nonnull)
	 */
	@Nonnull
	@POST("/system/reset-way2health-incidents")
	public ApiResponse resetWay2HealthIncidents() {
		getWay2HealthService().resetIncidents();
		return new ApiResponse();
	}

	@Nonnull
	protected SystemService getSystemService() {
		return systemService;
	}

	@Nonnull
	protected EmailMessageManager getEmailMessageManager() {
		return emailMessageManager;
	}

	@Nonnull
	protected Configuration getConfiguration() {
		return configuration;
	}

	@Nonnull
	protected Cache getLocalCache() {
		return localCache;
	}

	@Nonnull
	protected Cache getDistributedCache() {
		return distributedCache;
	}

	@Nonnull
	protected Authenticator getAuthenticator() {
		return authenticator;
	}

	@Nonnull
	protected AcuitySchedulingCache getAcuitySchedulingCache() {
		return acuitySchedulingCache;
	}

	@Nonnull
	protected AcuitySchedulingClient getAcuitySchedulingClient() {
		return acuitySchedulingClient;
	}

	@Nonnull
	protected Formatter getFormatter() {
		return formatter;
	}

	@Nonnull
	protected Strings getStrings() {
		return strings;
	}

	@Nonnull
	protected EpicSyncManager getEpicSyncManager() {
		return epicSyncManager;
	}

	@Nonnull
	protected Way2HealthService getWay2HealthService() {
		return way2HealthService;
	}

	@Nonnull
	protected CurrentContext getCurrentContext() {
		return currentContextProvider.get();
	}
}