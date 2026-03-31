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

package com.cobaltplatform.api.integration.ipstack;

import com.google.gson.annotations.SerializedName;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.List;

/**
 * See documentation at https://ipstack.com/documentation.
 *
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class IpstackStandardLookupResponse {
	@Nullable
	private String rawJson;
	@Nullable
	private Boolean success;
	@Nullable
	private ErrorDetails error;
	@Nullable
	private String ip;
	@Nullable
	private String hostname;
	@Nullable
	private String type;
	@SerializedName("continent_code")
	@Nullable
	private String continentCode;
	@SerializedName("continent_name")
	@Nullable
	private String continentName;
	@SerializedName("country_code")
	@Nullable
	private String countryCode;
	@SerializedName("country_name")
	@Nullable
	private String countryName;
	@SerializedName("region_code")
	@Nullable
	private String regionCode;
	@SerializedName("region_name")
	@Nullable
	private String regionName;
	@Nullable
	private String city;
	@SerializedName("zip")
	@Nullable
	private String postalCode;
	@Nullable
	private Double latitude;
	@Nullable
	private Double longitude;
	@Nullable
	private String msa;
	@Nullable
	private String dma;
	@Nullable
	private Double radius;
	@SerializedName("ip_routing_type")
	@Nullable
	private String ipRoutingType;
	@SerializedName("connection_type")
	@Nullable
	private String connectionType;
	@Nullable
	private Location location;
	@SerializedName("time_zone")
	@Nullable
	private TimeZone timeZone;
	@Nullable
	private Currency currency;
	@Nullable
	private Connection connection;
	@Nullable
	private Security security;

	@NotThreadSafe
	public static class ErrorDetails {
		@Nullable
		private Integer code;
		@Nullable
		private String type;
		@SerializedName("info")
		@Nullable
		private String message;

		@Nullable
		public Integer getCode() {
			return this.code;
		}

		public void setCode(@Nullable Integer code) {
			this.code = code;
		}

		@Nullable
		public String getType() {
			return this.type;
		}

		public void setType(@Nullable String type) {
			this.type = type;
		}

		@Nullable
		public String getMessage() {
			return this.message;
		}

		public void setMessage(@Nullable String message) {
			this.message = message;
		}
	}

	@NotThreadSafe
	public static class Location {
		@SerializedName("geoname_id")
		@Nullable
		private Long geonameId;
		@Nullable
		private String capital;
		@Nullable
		private List<Language> languages;
		@SerializedName("country_flag")
		@Nullable
		private String countryFlag;
		@SerializedName("country_flag_emoji")
		@Nullable
		private String countryFlagEmoji;
		@SerializedName("country_flag_emoji_unicode")
		@Nullable
		private String countryFlagEmojiUnicode;
		@SerializedName("calling_code")
		@Nullable
		private String callingCode;
		@SerializedName("is_eu")
		@Nullable
		private Boolean eu;

		@NotThreadSafe
		public static class Language {
			@Nullable
			private String code;
			@Nullable
			private String name;
			@SerializedName("native")
			@Nullable
			private String nativeName;

			@Nullable
			public String getCode() {
				return this.code;
			}

			public void setCode(@Nullable String code) {
				this.code = code;
			}

			@Nullable
			public String getName() {
				return this.name;
			}

			public void setName(@Nullable String name) {
				this.name = name;
			}

			@Nullable
			public String getNativeName() {
				return this.nativeName;
			}

			public void setNativeName(@Nullable String nativeName) {
				this.nativeName = nativeName;
			}
		}

		@Nullable
		public Long getGeonameId() {
			return this.geonameId;
		}

		public void setGeonameId(@Nullable Long geonameId) {
			this.geonameId = geonameId;
		}

		@Nullable
		public String getCapital() {
			return this.capital;
		}

		public void setCapital(@Nullable String capital) {
			this.capital = capital;
		}

		@Nullable
		public List<Language> getLanguages() {
			return this.languages;
		}

		public void setLanguages(@Nullable List<Language> languages) {
			this.languages = languages;
		}

		@Nullable
		public String getCountryFlag() {
			return this.countryFlag;
		}

		public void setCountryFlag(@Nullable String countryFlag) {
			this.countryFlag = countryFlag;
		}

		@Nullable
		public String getCountryFlagEmoji() {
			return this.countryFlagEmoji;
		}

		public void setCountryFlagEmoji(@Nullable String countryFlagEmoji) {
			this.countryFlagEmoji = countryFlagEmoji;
		}

		@Nullable
		public String getCountryFlagEmojiUnicode() {
			return this.countryFlagEmojiUnicode;
		}

		public void setCountryFlagEmojiUnicode(@Nullable String countryFlagEmojiUnicode) {
			this.countryFlagEmojiUnicode = countryFlagEmojiUnicode;
		}

		@Nullable
		public String getCallingCode() {
			return this.callingCode;
		}

		public void setCallingCode(@Nullable String callingCode) {
			this.callingCode = callingCode;
		}

		@Nullable
		public Boolean getEu() {
			return this.eu;
		}

		public void setEu(@Nullable Boolean eu) {
			this.eu = eu;
		}
	}

	@NotThreadSafe
	public static class TimeZone {
		@Nullable
		private String id;
		@SerializedName("current_time")
		@Nullable
		private String currentTime;
		@SerializedName("gmt_offset")
		@Nullable
		private Integer gmtOffset;
		@Nullable
		private String code;
		@SerializedName("is_daylight_saving")
		@Nullable
		private Boolean daylightSaving;

		@Nullable
		public String getId() {
			return this.id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public String getCurrentTime() {
			return this.currentTime;
		}

		public void setCurrentTime(@Nullable String currentTime) {
			this.currentTime = currentTime;
		}

		@Nullable
		public Integer getGmtOffset() {
			return this.gmtOffset;
		}

		public void setGmtOffset(@Nullable Integer gmtOffset) {
			this.gmtOffset = gmtOffset;
		}

		@Nullable
		public String getCode() {
			return this.code;
		}

		public void setCode(@Nullable String code) {
			this.code = code;
		}

		@Nullable
		public Boolean getDaylightSaving() {
			return this.daylightSaving;
		}

		public void setDaylightSaving(@Nullable Boolean daylightSaving) {
			this.daylightSaving = daylightSaving;
		}
	}

	@NotThreadSafe
	public static class Currency {
		@Nullable
		private String code;
		@Nullable
		private String name;
		@Nullable
		private String plural;
		@Nullable
		private String symbol;
		@SerializedName("symbol_native")
		@Nullable
		private String symbolNative;

		@Nullable
		public String getCode() {
			return this.code;
		}

		public void setCode(@Nullable String code) {
			this.code = code;
		}

		@Nullable
		public String getName() {
			return this.name;
		}

		public void setName(@Nullable String name) {
			this.name = name;
		}

		@Nullable
		public String getPlural() {
			return this.plural;
		}

		public void setPlural(@Nullable String plural) {
			this.plural = plural;
		}

		@Nullable
		public String getSymbol() {
			return this.symbol;
		}

		public void setSymbol(@Nullable String symbol) {
			this.symbol = symbol;
		}

		@Nullable
		public String getSymbolNative() {
			return this.symbolNative;
		}

		public void setSymbolNative(@Nullable String symbolNative) {
			this.symbolNative = symbolNative;
		}
	}

	@NotThreadSafe
	public static class Connection {
		@Nullable
		private Long asn;
		@Nullable
		private String isp;
		@Nullable
		private String sld;
		@Nullable
		private String tld;
		@Nullable
		private String carrier;
		@Nullable
		private Boolean home;
		@SerializedName("organization_type")
		@Nullable
		private String organizationType;
		@SerializedName("isic_code")
		@Nullable
		private String isicCode;
		@SerializedName("naics_code")
		@Nullable
		private String naicsCode;

		@Nullable
		public Long getAsn() {
			return this.asn;
		}

		public void setAsn(@Nullable Long asn) {
			this.asn = asn;
		}

		@Nullable
		public String getIsp() {
			return this.isp;
		}

		public void setIsp(@Nullable String isp) {
			this.isp = isp;
		}

		@Nullable
		public String getSld() {
			return this.sld;
		}

		public void setSld(@Nullable String sld) {
			this.sld = sld;
		}

		@Nullable
		public String getTld() {
			return this.tld;
		}

		public void setTld(@Nullable String tld) {
			this.tld = tld;
		}

		@Nullable
		public String getCarrier() {
			return this.carrier;
		}

		public void setCarrier(@Nullable String carrier) {
			this.carrier = carrier;
		}

		@Nullable
		public Boolean getHome() {
			return this.home;
		}

		public void setHome(@Nullable Boolean home) {
			this.home = home;
		}

		@Nullable
		public String getOrganizationType() {
			return this.organizationType;
		}

		public void setOrganizationType(@Nullable String organizationType) {
			this.organizationType = organizationType;
		}

		@Nullable
		public String getIsicCode() {
			return this.isicCode;
		}

		public void setIsicCode(@Nullable String isicCode) {
			this.isicCode = isicCode;
		}

		@Nullable
		public String getNaicsCode() {
			return this.naicsCode;
		}

		public void setNaicsCode(@Nullable String naicsCode) {
			this.naicsCode = naicsCode;
		}
	}

	@NotThreadSafe
	public static class Security {
		@SerializedName("is_proxy")
		@Nullable
		private Boolean proxy;
		@SerializedName("proxy_type")
		@Nullable
		private String proxyType;
		@SerializedName("is_crawler")
		@Nullable
		private Boolean crawler;
		@SerializedName("crawler_name")
		@Nullable
		private String crawlerName;
		@SerializedName("crawler_type")
		@Nullable
		private String crawlerType;
		@SerializedName("is_tor")
		@Nullable
		private Boolean tor;
		@SerializedName("threat_level")
		@Nullable
		private String threatLevel;
		@SerializedName("threat_types")
		@Nullable
		private List<String> threatTypes;
		@SerializedName("proxy_last_detected")
		@Nullable
		private String proxyLastDetected;
		@SerializedName("proxy_level")
		@Nullable
		private String proxyLevel;
		@SerializedName("vpn_service")
		@Nullable
		private String vpnService;
		@SerializedName("anonymizer_status")
		@Nullable
		private String anonymizerStatus;
		@SerializedName("hosting_facility")
		@Nullable
		private Boolean hostingFacility;

		@Nullable
		public Boolean getProxy() {
			return this.proxy;
		}

		public void setProxy(@Nullable Boolean proxy) {
			this.proxy = proxy;
		}

		@Nullable
		public String getProxyType() {
			return this.proxyType;
		}

		public void setProxyType(@Nullable String proxyType) {
			this.proxyType = proxyType;
		}

		@Nullable
		public Boolean getCrawler() {
			return this.crawler;
		}

		public void setCrawler(@Nullable Boolean crawler) {
			this.crawler = crawler;
		}

		@Nullable
		public String getCrawlerName() {
			return this.crawlerName;
		}

		public void setCrawlerName(@Nullable String crawlerName) {
			this.crawlerName = crawlerName;
		}

		@Nullable
		public String getCrawlerType() {
			return this.crawlerType;
		}

		public void setCrawlerType(@Nullable String crawlerType) {
			this.crawlerType = crawlerType;
		}

		@Nullable
		public Boolean getTor() {
			return this.tor;
		}

		public void setTor(@Nullable Boolean tor) {
			this.tor = tor;
		}

		@Nullable
		public String getThreatLevel() {
			return this.threatLevel;
		}

		public void setThreatLevel(@Nullable String threatLevel) {
			this.threatLevel = threatLevel;
		}

		@Nullable
		public List<String> getThreatTypes() {
			return this.threatTypes;
		}

		public void setThreatTypes(@Nullable List<String> threatTypes) {
			this.threatTypes = threatTypes;
		}

		@Nullable
		public String getProxyLastDetected() {
			return this.proxyLastDetected;
		}

		public void setProxyLastDetected(@Nullable String proxyLastDetected) {
			this.proxyLastDetected = proxyLastDetected;
		}

		@Nullable
		public String getProxyLevel() {
			return this.proxyLevel;
		}

		public void setProxyLevel(@Nullable String proxyLevel) {
			this.proxyLevel = proxyLevel;
		}

		@Nullable
		public String getVpnService() {
			return this.vpnService;
		}

		public void setVpnService(@Nullable String vpnService) {
			this.vpnService = vpnService;
		}

		@Nullable
		public String getAnonymizerStatus() {
			return this.anonymizerStatus;
		}

		public void setAnonymizerStatus(@Nullable String anonymizerStatus) {
			this.anonymizerStatus = anonymizerStatus;
		}

		@Nullable
		public Boolean getHostingFacility() {
			return this.hostingFacility;
		}

		public void setHostingFacility(@Nullable Boolean hostingFacility) {
			this.hostingFacility = hostingFacility;
		}
	}

	public boolean hasError() {
		return Boolean.FALSE.equals(getSuccess()) || getError() != null;
	}

	@Nullable
	public String getRawJson() {
		return this.rawJson;
	}

	public void setRawJson(@Nullable String rawJson) {
		this.rawJson = rawJson;
	}

	@Nullable
	public Boolean getSuccess() {
		return this.success;
	}

	public void setSuccess(@Nullable Boolean success) {
		this.success = success;
	}

	@Nullable
	public ErrorDetails getError() {
		return this.error;
	}

	public void setError(@Nullable ErrorDetails error) {
		this.error = error;
	}

	@Nullable
	public String getIp() {
		return this.ip;
	}

	public void setIp(@Nullable String ip) {
		this.ip = ip;
	}

	@Nullable
	public String getHostname() {
		return this.hostname;
	}

	public void setHostname(@Nullable String hostname) {
		this.hostname = hostname;
	}

	@Nullable
	public String getType() {
		return this.type;
	}

	public void setType(@Nullable String type) {
		this.type = type;
	}

	@Nullable
	public String getContinentCode() {
		return this.continentCode;
	}

	public void setContinentCode(@Nullable String continentCode) {
		this.continentCode = continentCode;
	}

	@Nullable
	public String getContinentName() {
		return this.continentName;
	}

	public void setContinentName(@Nullable String continentName) {
		this.continentName = continentName;
	}

	@Nullable
	public String getCountryCode() {
		return this.countryCode;
	}

	public void setCountryCode(@Nullable String countryCode) {
		this.countryCode = countryCode;
	}

	@Nullable
	public String getCountryName() {
		return this.countryName;
	}

	public void setCountryName(@Nullable String countryName) {
		this.countryName = countryName;
	}

	@Nullable
	public String getRegionCode() {
		return this.regionCode;
	}

	public void setRegionCode(@Nullable String regionCode) {
		this.regionCode = regionCode;
	}

	@Nullable
	public String getRegionName() {
		return this.regionName;
	}

	public void setRegionName(@Nullable String regionName) {
		this.regionName = regionName;
	}

	@Nullable
	public String getCity() {
		return this.city;
	}

	public void setCity(@Nullable String city) {
		this.city = city;
	}

	@Nullable
	public String getPostalCode() {
		return this.postalCode;
	}

	public void setPostalCode(@Nullable String postalCode) {
		this.postalCode = postalCode;
	}

	@Nullable
	public Double getLatitude() {
		return this.latitude;
	}

	public void setLatitude(@Nullable Double latitude) {
		this.latitude = latitude;
	}

	@Nullable
	public Double getLongitude() {
		return this.longitude;
	}

	public void setLongitude(@Nullable Double longitude) {
		this.longitude = longitude;
	}

	@Nullable
	public String getMsa() {
		return this.msa;
	}

	public void setMsa(@Nullable String msa) {
		this.msa = msa;
	}

	@Nullable
	public String getDma() {
		return this.dma;
	}

	public void setDma(@Nullable String dma) {
		this.dma = dma;
	}

	@Nullable
	public Double getRadius() {
		return this.radius;
	}

	public void setRadius(@Nullable Double radius) {
		this.radius = radius;
	}

	@Nullable
	public String getIpRoutingType() {
		return this.ipRoutingType;
	}

	public void setIpRoutingType(@Nullable String ipRoutingType) {
		this.ipRoutingType = ipRoutingType;
	}

	@Nullable
	public String getConnectionType() {
		return this.connectionType;
	}

	public void setConnectionType(@Nullable String connectionType) {
		this.connectionType = connectionType;
	}

	@Nullable
	public Location getLocation() {
		return this.location;
	}

	public void setLocation(@Nullable Location location) {
		this.location = location;
	}

	@Nullable
	public TimeZone getTimeZone() {
		return this.timeZone;
	}

	public void setTimeZone(@Nullable TimeZone timeZone) {
		this.timeZone = timeZone;
	}

	@Nullable
	public Currency getCurrency() {
		return this.currency;
	}

	public void setCurrency(@Nullable Currency currency) {
		this.currency = currency;
	}

	@Nullable
	public Connection getConnection() {
		return this.connection;
	}

	public void setConnection(@Nullable Connection connection) {
		this.connection = connection;
	}

	@Nullable
	public Security getSecurity() {
		return this.security;
	}

	public void setSecurity(@Nullable Security security) {
		this.security = security;
	}
}
