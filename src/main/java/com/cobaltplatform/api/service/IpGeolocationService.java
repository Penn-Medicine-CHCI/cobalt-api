/*
 * Copyright 2026 Cobalt Innovations, Inc.
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

import com.cobaltplatform.api.integration.ipstack.IpstackClient;
import com.cobaltplatform.api.integration.ipstack.IpstackStandardLookupRequest;
import com.cobaltplatform.api.integration.ipstack.IpstackStandardLookupResponse;
import com.cobaltplatform.api.model.db.IpGeolocationStatus.IpGeolocationStatusId;
import com.cobaltplatform.api.util.JsonMapper;
import com.cobaltplatform.api.util.ValidationException;
import com.cobaltplatform.api.util.ValidationException.FieldError;
import com.cobaltplatform.api.util.db.DatabaseProvider;
import com.pyranid.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * Manual batch-oriented IP geolocation processing.
 *
 * Rows are intentionally queued in SQL first and then resolved in small batches on demand.
 *
 * @author Cobalt Innovations, Inc.
 */
@Singleton
@ThreadSafe
public class IpGeolocationService {
	@Nonnull
	private static final Integer DEFAULT_BATCH_SIZE;
	@Nonnull
	private static final Integer MAX_BATCH_SIZE;

	@Nonnull
	private final DatabaseProvider databaseProvider;
	@Nonnull
	private final IpstackClient ipstackClient;
	@Nonnull
	private final JsonMapper jsonMapper;
	@Nonnull
	private final Logger logger;

	static {
		DEFAULT_BATCH_SIZE = 100;
		MAX_BATCH_SIZE = 1_000;
	}

	@Inject
	public IpGeolocationService(@Nonnull DatabaseProvider databaseProvider,
															@Nonnull IpstackClient ipstackClient,
															@Nonnull JsonMapper jsonMapper) {
		requireNonNull(databaseProvider);
		requireNonNull(ipstackClient);
		requireNonNull(jsonMapper);

		this.databaseProvider = databaseProvider;
		this.ipstackClient = ipstackClient;
		this.jsonMapper = jsonMapper;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	public ProcessingResult processPendingIpGeolocations(@Nullable Integer limit,
																											 @Nullable Boolean includeFailed) {
		int effectiveLimit = normalizedLimit(limit);
		boolean shouldIncludeFailed = Boolean.TRUE.equals(includeFailed);

		requeueStaleInProgressRows();

		List<ClaimedIpGeolocation> claimedIpGeolocations = claimIpGeolocations(effectiveLimit, shouldIncludeFailed);
		ProcessingResult processingResult = new ProcessingResult();
		processingResult.setClaimedCount(claimedIpGeolocations.size());

		for (ClaimedIpGeolocation claimedIpGeolocation : claimedIpGeolocations) {
			String ipAddress = trimToNull(claimedIpGeolocation.getIpAddress());

			if (ipAddress == null) {
				markInvalid(ipAddress, "Claimed IP geolocation row did not contain an IP address.");
				processingResult.setSkippedInvalidCount(processingResult.getSkippedInvalidCount() + 1);
				continue;
			}

			if (Boolean.TRUE.equals(claimedIpGeolocation.getPrivateOrReserved())) {
				markPrivate(ipAddress);
				processingResult.setSkippedPrivateCount(processingResult.getSkippedPrivateCount() + 1);
				continue;
			}

			try {
				IpstackStandardLookupResponse response = getIpstackClient().performStandardLookup(
						IpstackStandardLookupRequest.withIpAddress(ipAddress)
								.hostname(true)
								.build());

				if (response.hasError()) {
					markFailure(ipAddress, response);
					processingResult.setFailedCount(processingResult.getFailedCount() + 1);
				} else {
					markSuccess(ipAddress, response);
					processingResult.setSucceededCount(processingResult.getSucceededCount() + 1);
				}
			} catch (Exception e) {
				getLogger().warn("Unable to geolocate IP address {}", ipAddress, e);
				markFailure(ipAddress, e);
				processingResult.setFailedCount(processingResult.getFailedCount() + 1);
			}
		}

		return processingResult;
	}

	protected void requeueStaleInProgressRows() {
		getWritableDatabase().execute("""
				UPDATE ip_geolocation
				SET ip_geolocation_status_id=?
				WHERE ip_geolocation_status_id=?
				AND last_lookup_attempted_at <= now() - INTERVAL '15 minutes'
				""",
			IpGeolocationStatusId.PENDING,
			IpGeolocationStatusId.IN_PROGRESS);
	}

	@Nonnull
	protected List<ClaimedIpGeolocation> claimIpGeolocations(@Nonnull Integer limit,
																											 @Nonnull Boolean includeFailed) {
		requireNonNull(limit);
		requireNonNull(includeFailed);

		String candidateStatuses = includeFailed
				? format("'%s','%s'", IpGeolocationStatusId.PENDING.name(), IpGeolocationStatusId.FAILED.name())
				: format("'%s'", IpGeolocationStatusId.PENDING.name());

		return getWritableDatabase().queryForList(format("""
				WITH claimed AS (
					SELECT ip_address
					FROM ip_geolocation
					WHERE ip_geolocation_status_id IN (%s)
					ORDER BY last_lookup_attempted_at NULLS FIRST, created ASC
					FOR UPDATE SKIP LOCKED
					LIMIT ?
				)
				UPDATE ip_geolocation ipg
				SET ip_geolocation_status_id=?,
					last_lookup_attempted_at=now(),
					provider_error_code=NULL,
					provider_error_type=NULL,
					provider_error_message=NULL
				FROM claimed
				WHERE ipg.ip_address=claimed.ip_address
				RETURNING
					host(ipg.ip_address) AS ip_address,
					CASE
						WHEN family(ipg.ip_address)=4 AND (
							ipg.ip_address << '0.0.0.0/8'::cidr
							OR ipg.ip_address << '10.0.0.0/8'::cidr
							OR ipg.ip_address << '100.64.0.0/10'::cidr
							OR ipg.ip_address << '127.0.0.0/8'::cidr
							OR ipg.ip_address << '169.254.0.0/16'::cidr
							OR ipg.ip_address << '172.16.0.0/12'::cidr
							OR ipg.ip_address << '192.0.0.0/24'::cidr
							OR ipg.ip_address << '192.0.2.0/24'::cidr
							OR ipg.ip_address << '192.168.0.0/16'::cidr
							OR ipg.ip_address << '198.18.0.0/15'::cidr
							OR ipg.ip_address << '198.51.100.0/24'::cidr
							OR ipg.ip_address << '203.0.113.0/24'::cidr
							OR ipg.ip_address << '224.0.0.0/4'::cidr
							OR ipg.ip_address << '240.0.0.0/4'::cidr
						) THEN TRUE
						WHEN family(ipg.ip_address)=6 AND (
							ipg.ip_address << '::/128'::cidr
							OR ipg.ip_address << '::1/128'::cidr
							OR ipg.ip_address << '2001:db8::/32'::cidr
							OR ipg.ip_address << 'fc00::/7'::cidr
							OR ipg.ip_address << 'fe80::/10'::cidr
							OR ipg.ip_address << 'ff00::/8'::cidr
						) THEN TRUE
						ELSE FALSE
					END AS private_or_reserved
				""", candidateStatuses),
			ClaimedIpGeolocation.class,
			limit,
			IpGeolocationStatusId.IN_PROGRESS);
	}

	protected void markPrivate(@Nonnull String ipAddress) {
		requireNonNull(ipAddress);

		getWritableDatabase().execute("""
				UPDATE ip_geolocation
				SET ip_geolocation_status_id=?,
					provider_error_type=?,
					provider_error_message=?
				WHERE ip_address=CAST(? AS INET)
				""",
			IpGeolocationStatusId.SKIPPED_PRIVATE,
			"PRIVATE_OR_RESERVED_IP",
			"IP address is private or reserved and will not be sent to IPStack.",
			ipAddress);
	}

	protected void markInvalid(@Nullable String ipAddress,
														 @Nonnull String message) {
		requireNonNull(message);

		if (ipAddress == null)
			return;

		getWritableDatabase().execute("""
				UPDATE ip_geolocation
				SET ip_geolocation_status_id=?,
					provider_error_type=?,
					provider_error_message=?
				WHERE ip_address=CAST(? AS INET)
				""",
			IpGeolocationStatusId.SKIPPED_INVALID,
			"INVALID_IP",
			message,
			ipAddress);
	}

	protected void markFailure(@Nonnull String ipAddress,
														 @Nonnull IpstackStandardLookupResponse response) {
		requireNonNull(ipAddress);
		requireNonNull(response);

		IpstackStandardLookupResponse.ErrorDetails error = response.getError();

		getWritableDatabase().execute("""
				UPDATE ip_geolocation
				SET ip_geolocation_status_id=?,
					provider_error_code=?,
					provider_error_type=?,
					provider_error_message=?,
					provider_raw_json=CAST(? AS JSONB)
				WHERE ip_address=CAST(? AS INET)
				""",
			IpGeolocationStatusId.FAILED,
			error == null ? null : error.getCode(),
			error == null ? "IPSTACK_ERROR" : trimToNull(error.getType()),
			error == null ? "IPStack returned an unsuccessful response." : trimToNull(error.getMessage()),
			response.getRawJson(),
			ipAddress);
	}

	protected void markFailure(@Nonnull String ipAddress,
														 @Nonnull Exception exception) {
		requireNonNull(ipAddress);
		requireNonNull(exception);

		getWritableDatabase().execute("""
				UPDATE ip_geolocation
				SET ip_geolocation_status_id=?,
					provider_error_code=NULL,
					provider_error_type=?,
					provider_error_message=?,
					provider_raw_json=NULL
				WHERE ip_address=CAST(? AS INET)
				""",
			IpGeolocationStatusId.FAILED,
			exception.getClass().getSimpleName(),
			trimToNull(exception.getMessage()),
			ipAddress);
	}

	protected void markSuccess(@Nonnull String ipAddress,
														 @Nonnull IpstackStandardLookupResponse response) {
		requireNonNull(ipAddress);
		requireNonNull(response);

		IpstackStandardLookupResponse.Location location = response.getLocation();
		IpstackStandardLookupResponse.TimeZone timeZone = response.getTimeZone();
		IpstackStandardLookupResponse.Currency currency = response.getCurrency();
		IpstackStandardLookupResponse.Connection connection = response.getConnection();
		IpstackStandardLookupResponse.Security security = response.getSecurity();

		getWritableDatabase().execute("""
				UPDATE ip_geolocation
				SET ip_geolocation_status_id=?,
					provider_name='IPSTACK',
					ip_type=?,
					continent_code=?,
					continent_name=?,
					country_code=?,
					country_name=?,
					region_code=?,
					region_name=?,
					city=?,
					postal_code=?,
					latitude=?,
					longitude=?,
					msa=?,
					dma=?,
					radius=?,
					ip_routing_type=?,
					connection_type=?,
					location_geoname_id=?,
					location_capital=?,
					location_languages=CAST(? AS JSONB),
					location_country_flag=?,
					location_country_flag_emoji=?,
					location_country_flag_emoji_unicode=?,
					location_calling_code=?,
					location_is_eu=?,
					time_zone_id=?,
					time_zone_current_time=?,
					time_zone_gmt_offset=?,
					time_zone_code=?,
					time_zone_is_daylight_saving=?,
					currency_code=?,
					currency_name=?,
					currency_plural=?,
					currency_symbol=?,
					currency_symbol_native=?,
					connection_asn=?,
					connection_isp=?,
					connection_sld=?,
					connection_tld=?,
					connection_carrier=?,
					connection_home=?,
					connection_organization_type=?,
					connection_isic_code=?,
					connection_naics_code=?,
					hostname=?,
					security_is_proxy=?,
					security_proxy_type=?,
					security_is_crawler=?,
					security_crawler_name=?,
					security_crawler_type=?,
					security_is_tor=?,
					security_threat_level=?,
					security_threat_types=CAST(? AS JSONB),
					security_proxy_last_detected=?,
					security_proxy_level=?,
					security_vpn_service=?,
					security_anonymizer_status=?,
					security_hosting_facility=?,
					provider_error_code=NULL,
					provider_error_type=NULL,
					provider_error_message=NULL,
					provider_raw_json=CAST(? AS JSONB),
					last_lookup_succeeded_at=now()
				WHERE ip_address=CAST(? AS INET)
				""",
			IpGeolocationStatusId.SUCCEEDED,
			trimToNull(response.getType()),
			trimToNull(response.getContinentCode()),
			trimToNull(response.getContinentName()),
			trimToNull(response.getCountryCode()),
			trimToNull(response.getCountryName()),
			trimToNull(response.getRegionCode()),
			trimToNull(response.getRegionName()),
			trimToNull(response.getCity()),
			trimToNull(response.getPostalCode()),
			response.getLatitude(),
			response.getLongitude(),
			trimToNull(response.getMsa()),
			trimToNull(response.getDma()),
			response.getRadius(),
			trimToNull(response.getIpRoutingType()),
			trimToNull(response.getConnectionType()),
			location == null ? null : location.getGeonameId(),
			location == null ? null : trimToNull(location.getCapital()),
			location == null || location.getLanguages() == null ? null : getJsonMapper().toJson(location.getLanguages()),
			location == null ? null : trimToNull(location.getCountryFlag()),
			location == null ? null : trimToNull(location.getCountryFlagEmoji()),
			location == null ? null : trimToNull(location.getCountryFlagEmojiUnicode()),
			location == null ? null : trimToNull(location.getCallingCode()),
			location == null ? null : location.getEu(),
			timeZone == null ? null : trimToNull(timeZone.getId()),
			timeZone == null ? null : trimToNull(timeZone.getCurrentTime()),
			timeZone == null ? null : timeZone.getGmtOffset(),
			timeZone == null ? null : trimToNull(timeZone.getCode()),
			timeZone == null ? null : timeZone.getDaylightSaving(),
			currency == null ? null : trimToNull(currency.getCode()),
			currency == null ? null : trimToNull(currency.getName()),
			currency == null ? null : trimToNull(currency.getPlural()),
			currency == null ? null : trimToNull(currency.getSymbol()),
			currency == null ? null : trimToNull(currency.getSymbolNative()),
			connection == null ? null : connection.getAsn(),
			connection == null ? null : trimToNull(connection.getIsp()),
			connection == null ? null : trimToNull(connection.getSld()),
			connection == null ? null : trimToNull(connection.getTld()),
			connection == null ? null : trimToNull(connection.getCarrier()),
			connection == null ? null : connection.getHome(),
			connection == null ? null : trimToNull(connection.getOrganizationType()),
			connection == null ? null : trimToNull(connection.getIsicCode()),
			connection == null ? null : trimToNull(connection.getNaicsCode()),
			trimToNull(response.getHostname()),
			security == null ? null : security.getProxy(),
			security == null ? null : trimToNull(security.getProxyType()),
			security == null ? null : security.getCrawler(),
			security == null ? null : trimToNull(security.getCrawlerName()),
			security == null ? null : trimToNull(security.getCrawlerType()),
			security == null ? null : security.getTor(),
			security == null ? null : trimToNull(security.getThreatLevel()),
			security == null || security.getThreatTypes() == null ? null : getJsonMapper().toJson(security.getThreatTypes()),
			security == null ? null : trimToNull(security.getProxyLastDetected()),
			security == null ? null : trimToNull(security.getProxyLevel()),
			security == null ? null : trimToNull(security.getVpnService()),
			security == null ? null : trimToNull(security.getAnonymizerStatus()),
			security == null ? null : security.getHostingFacility(),
			response.getRawJson(),
			ipAddress);
	}

	@Nonnull
	protected Integer normalizedLimit(@Nullable Integer limit) {
		int effectiveLimit = limit == null ? getDefaultBatchSize() : limit;

		if (effectiveLimit < 1 || effectiveLimit > getMaxBatchSize())
			throw new ValidationException(new FieldError("limit", format("Limit must be between 1 and %s.", getMaxBatchSize())));

		return effectiveLimit;
	}

	@Nonnull
	protected Database getWritableDatabase() {
		return getDatabaseProvider().getWritableMasterDatabase();
	}

	@Nonnull
	protected DatabaseProvider getDatabaseProvider() {
		return this.databaseProvider;
	}

	@Nonnull
	protected IpstackClient getIpstackClient() {
		return this.ipstackClient;
	}

	@Nonnull
	protected JsonMapper getJsonMapper() {
		return this.jsonMapper;
	}

	@Nonnull
	protected Logger getLogger() {
		return this.logger;
	}

	@Nonnull
	protected Integer getDefaultBatchSize() {
		return DEFAULT_BATCH_SIZE;
	}

	@Nonnull
	protected Integer getMaxBatchSize() {
		return MAX_BATCH_SIZE;
	}

	@NotThreadSafe
	protected static class ClaimedIpGeolocation {
		@Nullable
		private String ipAddress;
		@Nullable
		private Boolean privateOrReserved;

		@Nullable
		public String getIpAddress() {
			return this.ipAddress;
		}

		public void setIpAddress(@Nullable String ipAddress) {
			this.ipAddress = ipAddress;
		}

		@Nullable
		public Boolean getPrivateOrReserved() {
			return this.privateOrReserved;
		}

		public void setPrivateOrReserved(@Nullable Boolean privateOrReserved) {
			this.privateOrReserved = privateOrReserved;
		}
	}

	@NotThreadSafe
	public static class ProcessingResult {
		private int claimedCount;
		private int succeededCount;
		private int failedCount;
		private int skippedInvalidCount;
		private int skippedPrivateCount;

		public int getClaimedCount() {
			return this.claimedCount;
		}

		public void setClaimedCount(int claimedCount) {
			this.claimedCount = claimedCount;
		}

		public int getSucceededCount() {
			return this.succeededCount;
		}

		public void setSucceededCount(int succeededCount) {
			this.succeededCount = succeededCount;
		}

		public int getFailedCount() {
			return this.failedCount;
		}

		public void setFailedCount(int failedCount) {
			this.failedCount = failedCount;
		}

		public int getSkippedInvalidCount() {
			return this.skippedInvalidCount;
		}

		public void setSkippedInvalidCount(int skippedInvalidCount) {
			this.skippedInvalidCount = skippedInvalidCount;
		}

		public int getSkippedPrivateCount() {
			return this.skippedPrivateCount;
		}

		public void setSkippedPrivateCount(int skippedPrivateCount) {
			this.skippedPrivateCount = skippedPrivateCount;
		}
	}
}
