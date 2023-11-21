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

package com.cobaltplatform.api.model.db;

import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.util.GsonUtility;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pyranid.DatabaseColumn;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class AnalyticsGoogleBigQueryEvent {
	@Nonnull
	private static final Gson GSON;

	static {
		GsonBuilder gsonBuilder = new GsonBuilder()
				.setPrettyPrinting()
				.disableHtmlEscaping();

		GsonUtility.applyDefaultTypeAdapters(gsonBuilder);

		GSON = gsonBuilder.create();
	}

	@Nullable
	@DatabaseColumn("analytics_google_bigquery_event_id")
	private UUID googleAnalyticsBigQueryEventId;
	@Nullable
	private InstitutionId institutionId;
	@Nullable
	private UUID accountId;
	@Nullable
	private String userPseudoId;
	@Nullable
	private String eventBundleSequenceId;
	@Nullable
	private String name;
	@Nullable
	private LocalDate date;
	@Nullable
	private Instant timestamp;
	@Nullable
	@DatabaseColumn("event")
	private String eventAsJson;
	@Nullable
	@DatabaseColumn("bigquery_user")
	private String userAsJson;
	@Nullable
	@DatabaseColumn("traffic_source")
	private String trafficSourceAsJson;
	@Nullable
	@DatabaseColumn("collected_traffic_source")
	private String collectedTrafficSourceAsJson;
	@Nullable
	@DatabaseColumn("geo")
	private String geoAsJson;
	@Nullable
	@DatabaseColumn("device")
	private String deviceAsJson;

	@Nonnull
	public Optional<Event> getEvent() {
		return this.eventAsJson == null ? Optional.empty() : Optional.of(GSON.fromJson(this.eventAsJson, Event.class));
	}

	@Nonnull
	public Optional<User> getUser() {
		return this.userAsJson == null ? Optional.empty() : Optional.of(GSON.fromJson(this.userAsJson, User.class));
	}

	@Nonnull
	public Optional<TrafficSource> getTrafficSource() {
		return this.trafficSourceAsJson == null ? Optional.empty() : Optional.of(GSON.fromJson(this.trafficSourceAsJson, TrafficSource.class));
	}

	@Nonnull
	public Optional<CollectedTrafficSource> getCollectedTrafficSource() {
		return this.collectedTrafficSourceAsJson == null ? Optional.empty() : Optional.of(GSON.fromJson(this.collectedTrafficSourceAsJson, CollectedTrafficSource.class));
	}

	@Nonnull
	public Optional<Geo> getGeo() {
		return this.geoAsJson == null ? Optional.empty() : Optional.of(GSON.fromJson(this.geoAsJson, Geo.class));
	}

	@Nonnull
	public Optional<Device> getDevice() {
		return this.deviceAsJson == null ? Optional.empty() : Optional.of(GSON.fromJson(this.deviceAsJson, Device.class));
	}

	@Override
	@Nonnull
	public String toString() {
		return GSON.toJson(this);
	}

	public static abstract class AnalyticsGoogleBigQueryProperty {
		@Override
		@Nonnull
		public String toString() {
			return toJson();
		}

		@Nonnull
		public String toJson() {
			return GSON.toJson(this);
		}
	}

	@NotThreadSafe
	public static class Event extends AnalyticsGoogleBigQueryProperty {
		@Nullable
		private LocalDate date;
		@Nullable
		private Instant timestamp;
		@Nullable
		private Instant previousTimestamp;
		@Nullable
		private String name;
		@Nullable
		private Double valueInUsd;
		@Nullable
		private Long bundleSequenceId;
		@Nullable
		private Long serverTimestampOffset;
		@Nullable
		private Map<String, EventParamValue> parameters;

		public enum EventParamValueType {
			STRING,
			INTEGER,
			FLOAT,
			DOUBLE
		}

		@NotThreadSafe
		public static class EventParamValue extends AnalyticsGoogleBigQueryProperty {
			@Nullable
			private Object value;
			@Nullable
			private EventParamValueType type;

			@Nullable
			public Object getValue() {
				return this.value;
			}

			public void setValue(@Nullable Object value) {
				this.value = value;
			}

			@Nullable
			public EventParamValueType getType() {
				return this.type;
			}

			public void setType(@Nullable EventParamValueType type) {
				this.type = type;
			}
		}

		@Nullable
		public LocalDate getDate() {
			return this.date;
		}

		public void setDate(@Nullable LocalDate date) {
			this.date = date;
		}

		@Nullable
		public Instant getTimestamp() {
			return this.timestamp;
		}

		public void setTimestamp(@Nullable Instant timestamp) {
			this.timestamp = timestamp;
		}

		@Nullable
		public Instant getPreviousTimestamp() {
			return this.previousTimestamp;
		}

		public void setPreviousTimestamp(@Nullable Instant previousTimestamp) {
			this.previousTimestamp = previousTimestamp;
		}

		@Nullable
		public String getName() {
			return this.name;
		}

		public void setName(@Nullable String name) {
			this.name = name;
		}

		@Nullable
		public Double getValueInUsd() {
			return this.valueInUsd;
		}

		public void setValueInUsd(@Nullable Double valueInUsd) {
			this.valueInUsd = valueInUsd;
		}

		@Nullable
		public Long getBundleSequenceId() {
			return this.bundleSequenceId;
		}

		public void setBundleSequenceId(@Nullable Long bundleSequenceId) {
			this.bundleSequenceId = bundleSequenceId;
		}

		@Nullable
		public Long getServerTimestampOffset() {
			return this.serverTimestampOffset;
		}

		public void setServerTimestampOffset(@Nullable Long serverTimestampOffset) {
			this.serverTimestampOffset = serverTimestampOffset;
		}

		@Nullable
		public Map<String, EventParamValue> getParameters() {
			return this.parameters;
		}

		public void setParameters(@Nullable Map<String, EventParamValue> parameters) {
			this.parameters = parameters;
		}
	}

	@NotThreadSafe
	public static class User extends AnalyticsGoogleBigQueryProperty {
		@Nullable
		private Boolean isActiveUser;
		@Nullable
		private String userId;
		@Nullable
		private String userPseudoId;
		@Nullable
		private Instant userFirstTouchTimestamp;

		@Nullable
		public Boolean getIsActiveUser() {
			return this.isActiveUser;
		}

		public void setIsActiveUser(@Nullable Boolean isActiveUser) {
			this.isActiveUser = isActiveUser;
		}

		@Nullable
		public String getUserId() {
			return this.userId;
		}

		public void setUserId(@Nullable String userId) {
			this.userId = userId;
		}

		@Nullable
		public String getUserPseudoId() {
			return this.userPseudoId;
		}

		public void setUserPseudoId(@Nullable String userPseudoId) {
			this.userPseudoId = userPseudoId;
		}

		@Nullable
		public Instant getUserFirstTouchTimestamp() {
			return this.userFirstTouchTimestamp;
		}

		public void setUserFirstTouchTimestamp(@Nullable Instant userFirstTouchTimestamp) {
			this.userFirstTouchTimestamp = userFirstTouchTimestamp;
		}
	}

	@NotThreadSafe
	public static class TrafficSource extends AnalyticsGoogleBigQueryProperty {
		@Nullable
		private String name;
		@Nullable
		private String medium;
		@Nullable
		private String source;

		@Nullable
		public String getName() {
			return this.name;
		}

		public void setName(@Nullable String name) {
			this.name = name;
		}

		@Nullable
		public String getMedium() {
			return this.medium;
		}

		public void setMedium(@Nullable String medium) {
			this.medium = medium;
		}

		@Nullable
		public String getSource() {
			return this.source;
		}

		public void setSource(@Nullable String source) {
			this.source = source;
		}
	}

	@NotThreadSafe
	public static class CollectedTrafficSource extends AnalyticsGoogleBigQueryProperty {
		@Nullable
		private String manualCampaignId;
		@Nullable
		private String manualCampaignName;
		@Nullable
		private String manualSource;
		@Nullable
		private String manualMedium;
		@Nullable
		private String manualTerm;
		@Nullable
		private String manualContent;
		@Nullable
		private String gclid;
		@Nullable
		private String dclid;
		@Nullable
		private String srsltid;

		@Nullable
		public String getManualCampaignId() {
			return this.manualCampaignId;
		}

		public void setManualCampaignId(@Nullable String manualCampaignId) {
			this.manualCampaignId = manualCampaignId;
		}

		@Nullable
		public String getManualCampaignName() {
			return this.manualCampaignName;
		}

		public void setManualCampaignName(@Nullable String manualCampaignName) {
			this.manualCampaignName = manualCampaignName;
		}

		@Nullable
		public String getManualSource() {
			return this.manualSource;
		}

		public void setManualSource(@Nullable String manualSource) {
			this.manualSource = manualSource;
		}

		@Nullable
		public String getManualMedium() {
			return this.manualMedium;
		}

		public void setManualMedium(@Nullable String manualMedium) {
			this.manualMedium = manualMedium;
		}

		@Nullable
		public String getManualTerm() {
			return this.manualTerm;
		}

		public void setManualTerm(@Nullable String manualTerm) {
			this.manualTerm = manualTerm;
		}

		@Nullable
		public String getManualContent() {
			return this.manualContent;
		}

		public void setManualContent(@Nullable String manualContent) {
			this.manualContent = manualContent;
		}

		@Nullable
		public String getGclid() {
			return this.gclid;
		}

		public void setGclid(@Nullable String gclid) {
			this.gclid = gclid;
		}

		@Nullable
		public String getDclid() {
			return this.dclid;
		}

		public void setDclid(@Nullable String dclid) {
			this.dclid = dclid;
		}

		@Nullable
		public String getSrsltid() {
			return this.srsltid;
		}

		public void setSrsltid(@Nullable String srsltid) {
			this.srsltid = srsltid;
		}
	}

	@NotThreadSafe
	public static class Device extends AnalyticsGoogleBigQueryProperty {
		@Nullable
		private String category;
		@Nullable
		private String mobileBrandName;
		@Nullable
		private String mobileModelName;
		@Nullable
		private String mobileMarketingName;
		@Nullable
		private String mobileOsHardwareModel;
		@Nullable
		private String operatingSystem;
		@Nullable
		private String operatingSystemVersion;
		@Nullable
		private String vendorId;
		@Nullable
		private String advertisingId;
		@Nullable
		private String language;
		@Nullable
		private Long timeZoneOffsetSeconds;
		@Nullable
		private Boolean isLimitedAdTracking;
		@Nullable
		private String browser;
		@Nullable
		private String browserVersion;
		@Nullable
		private String webInfoBrowser;
		@Nullable
		private String webInfoBrowserVersion;
		@Nullable
		private String webInfoBrowserHostname;

		@Nullable
		public String getCategory() {
			return this.category;
		}

		public void setCategory(@Nullable String category) {
			this.category = category;
		}

		@Nullable
		public String getMobileBrandName() {
			return this.mobileBrandName;
		}

		public void setMobileBrandName(@Nullable String mobileBrandName) {
			this.mobileBrandName = mobileBrandName;
		}

		@Nullable
		public String getMobileModelName() {
			return this.mobileModelName;
		}

		public void setMobileModelName(@Nullable String mobileModelName) {
			this.mobileModelName = mobileModelName;
		}

		@Nullable
		public String getMobileMarketingName() {
			return this.mobileMarketingName;
		}

		public void setMobileMarketingName(@Nullable String mobileMarketingName) {
			this.mobileMarketingName = mobileMarketingName;
		}

		@Nullable
		public String getMobileOsHardwareModel() {
			return this.mobileOsHardwareModel;
		}

		public void setMobileOsHardwareModel(@Nullable String mobileOsHardwareModel) {
			this.mobileOsHardwareModel = mobileOsHardwareModel;
		}

		@Nullable
		public String getOperatingSystem() {
			return this.operatingSystem;
		}

		public void setOperatingSystem(@Nullable String operatingSystem) {
			this.operatingSystem = operatingSystem;
		}

		@Nullable
		public String getOperatingSystemVersion() {
			return this.operatingSystemVersion;
		}

		public void setOperatingSystemVersion(@Nullable String operatingSystemVersion) {
			this.operatingSystemVersion = operatingSystemVersion;
		}

		@Nullable
		public String getVendorId() {
			return this.vendorId;
		}

		public void setVendorId(@Nullable String vendorId) {
			this.vendorId = vendorId;
		}

		@Nullable
		public String getAdvertisingId() {
			return this.advertisingId;
		}

		public void setAdvertisingId(@Nullable String advertisingId) {
			this.advertisingId = advertisingId;
		}

		@Nullable
		public String getLanguage() {
			return this.language;
		}

		public void setLanguage(@Nullable String language) {
			this.language = language;
		}

		@Nullable
		public Long getTimeZoneOffsetSeconds() {
			return this.timeZoneOffsetSeconds;
		}

		public void setTimeZoneOffsetSeconds(@Nullable Long timeZoneOffsetSeconds) {
			this.timeZoneOffsetSeconds = timeZoneOffsetSeconds;
		}

		@Nullable
		public Boolean getIsLimitedAdTracking() {
			return this.isLimitedAdTracking;
		}

		public void setIsLimitedAdTracking(@Nullable Boolean isLimitedAdTracking) {
			this.isLimitedAdTracking = isLimitedAdTracking;
		}

		@Nullable
		public String getBrowser() {
			return this.browser;
		}

		public void setBrowser(@Nullable String browser) {
			this.browser = browser;
		}

		@Nullable
		public String getBrowserVersion() {
			return this.browserVersion;
		}

		public void setBrowserVersion(@Nullable String browserVersion) {
			this.browserVersion = browserVersion;
		}

		@Nullable
		public String getWebInfoBrowser() {
			return this.webInfoBrowser;
		}

		public void setWebInfoBrowser(@Nullable String webInfoBrowser) {
			this.webInfoBrowser = webInfoBrowser;
		}

		@Nullable
		public String getWebInfoBrowserVersion() {
			return this.webInfoBrowserVersion;
		}

		public void setWebInfoBrowserVersion(@Nullable String webInfoBrowserVersion) {
			this.webInfoBrowserVersion = webInfoBrowserVersion;
		}

		@Nullable
		public String getWebInfoBrowserHostname() {
			return this.webInfoBrowserHostname;
		}

		public void setWebInfoBrowserHostname(@Nullable String webInfoBrowserHostname) {
			this.webInfoBrowserHostname = webInfoBrowserHostname;
		}
	}

	@NotThreadSafe
	public static class Geo extends AnalyticsGoogleBigQueryProperty {
		@Nullable
		private String continent;
		@Nullable
		private String subContinent;
		@Nullable
		private String country;
		@Nullable
		private String region;
		@Nullable
		private String metro;
		@Nullable
		private String city;

		@Nullable
		public String getContinent() {
			return this.continent;
		}

		public void setContinent(@Nullable String continent) {
			this.continent = continent;
		}

		@Nullable
		public String getSubContinent() {
			return this.subContinent;
		}

		public void setSubContinent(@Nullable String subContinent) {
			this.subContinent = subContinent;
		}

		@Nullable
		public String getCountry() {
			return this.country;
		}

		public void setCountry(@Nullable String country) {
			this.country = country;
		}

		@Nullable
		public String getRegion() {
			return this.region;
		}

		public void setRegion(@Nullable String region) {
			this.region = region;
		}

		@Nullable
		public String getMetro() {
			return this.metro;
		}

		public void setMetro(@Nullable String metro) {
			this.metro = metro;
		}

		@Nullable
		public String getCity() {
			return this.city;
		}

		public void setCity(@Nullable String city) {
			this.city = city;
		}
	}

	@Nullable
	public UUID getGoogleAnalyticsBigQueryEventId() {
		return this.googleAnalyticsBigQueryEventId;
	}

	public void setGoogleAnalyticsBigQueryEventId(@Nullable UUID googleAnalyticsBigQueryEventId) {
		this.googleAnalyticsBigQueryEventId = googleAnalyticsBigQueryEventId;
	}

	@Nullable
	public InstitutionId getInstitutionId() {
		return this.institutionId;
	}

	public void setInstitutionId(@Nullable InstitutionId institutionId) {
		this.institutionId = institutionId;
	}

	@Nullable
	public UUID getAccountId() {
		return this.accountId;
	}

	public void setAccountId(@Nullable UUID accountId) {
		this.accountId = accountId;
	}

	@Nullable
	public String getUserPseudoId() {
		return this.userPseudoId;
	}

	public void setUserPseudoId(@Nullable String userPseudoId) {
		this.userPseudoId = userPseudoId;
	}

	@Nullable
	public String getEventBundleSequenceId() {
		return this.eventBundleSequenceId;
	}

	public void setEventBundleSequenceId(@Nullable String eventBundleSequenceId) {
		this.eventBundleSequenceId = eventBundleSequenceId;
	}

	@Nullable
	public String getName() {
		return this.name;
	}

	public void setName(@Nullable String name) {
		this.name = name;
	}

	@Nullable
	public LocalDate getDate() {
		return this.date;
	}

	public void setDate(@Nullable LocalDate date) {
		this.date = date;
	}

	@Nullable
	public Instant getTimestamp() {
		return this.timestamp;
	}

	public void setTimestamp(@Nullable Instant timestamp) {
		this.timestamp = timestamp;
	}

	@Nullable
	public String getEventAsJson() {
		return this.eventAsJson;
	}

	public void setEventAsJson(@Nullable String eventAsJson) {
		this.eventAsJson = eventAsJson;
	}

	@Nullable
	public String getUserAsJson() {
		return this.userAsJson;
	}

	public void setUserAsJson(@Nullable String userAsJson) {
		this.userAsJson = userAsJson;
	}

	@Nullable
	public String getTrafficSourceAsJson() {
		return this.trafficSourceAsJson;
	}

	public void setTrafficSourceAsJson(@Nullable String trafficSourceAsJson) {
		this.trafficSourceAsJson = trafficSourceAsJson;
	}

	@Nullable
	public String getCollectedTrafficSourceAsJson() {
		return this.collectedTrafficSourceAsJson;
	}

	public void setCollectedTrafficSourceAsJson(@Nullable String collectedTrafficSourceAsJson) {
		this.collectedTrafficSourceAsJson = collectedTrafficSourceAsJson;
	}

	@Nullable
	public String getGeoAsJson() {
		return this.geoAsJson;
	}

	public void setGeoAsJson(@Nullable String geoAsJson) {
		this.geoAsJson = geoAsJson;
	}

	@Nullable
	public String getDeviceAsJson() {
		return this.deviceAsJson;
	}

	public void setDeviceAsJson(@Nullable String deviceAsJson) {
		this.deviceAsJson = deviceAsJson;
	}
}