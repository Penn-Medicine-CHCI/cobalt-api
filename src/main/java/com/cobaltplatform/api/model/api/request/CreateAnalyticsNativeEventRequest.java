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

package com.cobaltplatform.api.model.api.request;

import com.cobaltplatform.api.model.db.AnalyticsNativeEventType.AnalyticsNativeEventTypeId;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class CreateAnalyticsNativeEventRequest {
	@Nullable
	private AnalyticsNativeEventTypeId analyticsNativeEventTypeId;
	@Nullable
	private InstitutionId institutionId;
	@Nullable
	private UUID accountId;
	@Nullable
	private UUID clientDeviceId;
	@Nullable
	private UUID sessionId;
	@Nullable
	private UUID referringMessageId;
	@Nullable
	private String referringCampaign;
	@Nullable
	private Instant timestamp;
	@Nullable
	private String webappUrl;
	@Nullable
	private Map<String, Object> data; // Understood to be a JSON object with type-specific data, e.g. {"topicCenterId": "[uuid]"}
	@Nullable
	private String appName; // Explicitly specified by client. Example for web: "Cobalt Website". Example for native app: "Cobalt App XYZ"
	@Nullable
	private String appVersion; // Explicitly specified by client. Example for web: "cbf1b9a1be984a9f61b79a05f23b19f66d533537" (git commit hash). Example for native app: "1.2.3 (4)"
	// Client device OS name at this moment in time.  May be different from client_device record - which only shows current device state - if user updates their OS
	// If explicitly specified by client.  Example for native app: "iOS"
	@Nullable
	private String clientDeviceOperatingSystemName;
	// Client device OS version at this moment in time.  May be different from client_device record - which only shows current device state - if user updates their OS
	// If explicitly specified by client.  Example for native app: "17.6"
	@Nullable
	private String clientDeviceOperatingSystemVersion;
	@Nullable
	private List<Locale> clientDeviceSupportedLocales; // Client device's supported locales at this moment in time, e.g. ["en-US", "en", "fr-CA"]
	@Nullable
	private Locale clientDeviceLocale; // Client device's locale at this moment in time
	@Nullable
	private ZoneId clientDeviceTimeZone; // Client device's timezone at this moment in time
	@Nullable
	private String ipAddress;
	@Nullable
	private String userAgent; // The value of window.navigator.userAgent
	@Nullable
	private String userAgentDeviceFamily; // Parsed from User-Agent
	@Nullable
	private String userAgentBrowserFamily; // Parsed from User-Agent
	@Nullable
	private String userAgentBrowserVersion; // Parsed from User-Agent
	@Nullable
	private String userAgentOperatingSystemName; // Parsed from User-Agent
	@Nullable
	private String userAgentOperatingSystemVersion; // Parsed from User-Agent
	@Nullable
	private Integer screenColorDepth; // Provided by JS window.screen object on web
	@Nullable
	private Integer screenPixelDepth; // Provided by JS window.screen object on web
	@Nullable
	private Double screenWidth; // Provided by JS window.screen object on web
	@Nullable
	private Double screenHeight; // Provided by JS window.screen object on web
	@Nullable
	private String screenOrientation; // Provided by JS window.screen object on web
	@Nullable
	private Double windowDevicePixelRatio; // Provided by JS window object on web
	@Nullable
	private Double windowWidth; // Provided by JS window object on web
	@Nullable
	private Double windowHeight; // Provided by JS window object on web
	@Nullable
	private Integer navigatorMaxTouchPoints; // Provided by navigator.maxTouchPoints on web
	@Nullable
	private String documentVisibilityState; // Provided by document.visibilityState on web

	@Nullable
	public AnalyticsNativeEventTypeId getAnalyticsNativeEventTypeId() {
		return this.analyticsNativeEventTypeId;
	}

	public void setAnalyticsNativeEventTypeId(@Nullable AnalyticsNativeEventTypeId analyticsNativeEventTypeId) {
		this.analyticsNativeEventTypeId = analyticsNativeEventTypeId;
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
	public UUID getSessionId() {
		return this.sessionId;
	}

	public void setSessionId(@Nullable UUID sessionId) {
		this.sessionId = sessionId;
	}

	@Nullable
	public UUID getReferringMessageId() {
		return this.referringMessageId;
	}

	public void setReferringMessageId(@Nullable UUID referringMessageId) {
		this.referringMessageId = referringMessageId;
	}

	@Nullable
	public String getReferringCampaign() {
		return this.referringCampaign;
	}

	public void setReferringCampaign(@Nullable String referringCampaign) {
		this.referringCampaign = referringCampaign;
	}

	@Nullable
	public UUID getClientDeviceId() {
		return this.clientDeviceId;
	}

	public void setClientDeviceId(@Nullable UUID clientDeviceId) {
		this.clientDeviceId = clientDeviceId;
	}

	@Nullable
	public Instant getTimestamp() {
		return this.timestamp;
	}

	public void setTimestamp(@Nullable Instant timestamp) {
		this.timestamp = timestamp;
	}

	@Nullable
	public String getWebappUrl() {
		return this.webappUrl;
	}

	public void setWebappUrl(@Nullable String webappUrl) {
		this.webappUrl = webappUrl;
	}

	@Nullable
	public Map<String, Object> getData() {
		return this.data;
	}

	public void setData(@Nullable Map<String, Object> data) {
		this.data = data;
	}

	@Nullable
	public String getAppName() {
		return this.appName;
	}

	public void setAppName(@Nullable String appName) {
		this.appName = appName;
	}

	@Nullable
	public String getAppVersion() {
		return this.appVersion;
	}

	public void setAppVersion(@Nullable String appVersion) {
		this.appVersion = appVersion;
	}

	@Nullable
	public String getClientDeviceOperatingSystemName() {
		return this.clientDeviceOperatingSystemName;
	}

	public void setClientDeviceOperatingSystemName(@Nullable String clientDeviceOperatingSystemName) {
		this.clientDeviceOperatingSystemName = clientDeviceOperatingSystemName;
	}

	@Nullable
	public String getClientDeviceOperatingSystemVersion() {
		return this.clientDeviceOperatingSystemVersion;
	}

	public void setClientDeviceOperatingSystemVersion(@Nullable String clientDeviceOperatingSystemVersion) {
		this.clientDeviceOperatingSystemVersion = clientDeviceOperatingSystemVersion;
	}

	@Nullable
	public List<Locale> getClientDeviceSupportedLocales() {
		return this.clientDeviceSupportedLocales;
	}

	public void setClientDeviceSupportedLocales(@Nullable List<Locale> clientDeviceSupportedLocales) {
		this.clientDeviceSupportedLocales = clientDeviceSupportedLocales;
	}

	@Nullable
	public Locale getClientDeviceLocale() {
		return this.clientDeviceLocale;
	}

	public void setClientDeviceLocale(@Nullable Locale clientDeviceLocale) {
		this.clientDeviceLocale = clientDeviceLocale;
	}

	@Nullable
	public ZoneId getClientDeviceTimeZone() {
		return this.clientDeviceTimeZone;
	}

	public void setClientDeviceTimeZone(@Nullable ZoneId clientDeviceTimeZone) {
		this.clientDeviceTimeZone = clientDeviceTimeZone;
	}

	@Nullable
	public String getIpAddress() {
		return this.ipAddress;
	}

	public void setIpAddress(@Nullable String ipAddress) {
		this.ipAddress = ipAddress;
	}

	@Nullable
	public String getUserAgent() {
		return this.userAgent;
	}

	public void setUserAgent(@Nullable String userAgent) {
		this.userAgent = userAgent;
	}

	@Nullable
	public String getUserAgentDeviceFamily() {
		return this.userAgentDeviceFamily;
	}

	public void setUserAgentDeviceFamily(@Nullable String userAgentDeviceFamily) {
		this.userAgentDeviceFamily = userAgentDeviceFamily;
	}

	@Nullable
	public String getUserAgentBrowserFamily() {
		return this.userAgentBrowserFamily;
	}

	public void setUserAgentBrowserFamily(@Nullable String userAgentBrowserFamily) {
		this.userAgentBrowserFamily = userAgentBrowserFamily;
	}

	@Nullable
	public String getUserAgentBrowserVersion() {
		return this.userAgentBrowserVersion;
	}

	public void setUserAgentBrowserVersion(@Nullable String userAgentBrowserVersion) {
		this.userAgentBrowserVersion = userAgentBrowserVersion;
	}

	@Nullable
	public String getUserAgentOperatingSystemName() {
		return this.userAgentOperatingSystemName;
	}

	public void setUserAgentOperatingSystemName(@Nullable String userAgentOperatingSystemName) {
		this.userAgentOperatingSystemName = userAgentOperatingSystemName;
	}

	@Nullable
	public String getUserAgentOperatingSystemVersion() {
		return this.userAgentOperatingSystemVersion;
	}

	public void setUserAgentOperatingSystemVersion(@Nullable String userAgentOperatingSystemVersion) {
		this.userAgentOperatingSystemVersion = userAgentOperatingSystemVersion;
	}

	@Nullable
	public Integer getScreenColorDepth() {
		return this.screenColorDepth;
	}

	public void setScreenColorDepth(@Nullable Integer screenColorDepth) {
		this.screenColorDepth = screenColorDepth;
	}

	@Nullable
	public Integer getScreenPixelDepth() {
		return this.screenPixelDepth;
	}

	public void setScreenPixelDepth(@Nullable Integer screenPixelDepth) {
		this.screenPixelDepth = screenPixelDepth;
	}

	@Nullable
	public Double getScreenWidth() {
		return this.screenWidth;
	}

	public void setScreenWidth(@Nullable Double screenWidth) {
		this.screenWidth = screenWidth;
	}

	@Nullable
	public Double getScreenHeight() {
		return this.screenHeight;
	}

	public void setScreenHeight(@Nullable Double screenHeight) {
		this.screenHeight = screenHeight;
	}

	@Nullable
	public String getScreenOrientation() {
		return this.screenOrientation;
	}

	public void setScreenOrientation(@Nullable String screenOrientation) {
		this.screenOrientation = screenOrientation;
	}

	@Nullable
	public Double getWindowDevicePixelRatio() {
		return this.windowDevicePixelRatio;
	}

	public void setWindowDevicePixelRatio(@Nullable Double windowDevicePixelRatio) {
		this.windowDevicePixelRatio = windowDevicePixelRatio;
	}

	@Nullable
	public Double getWindowWidth() {
		return this.windowWidth;
	}

	public void setWindowWidth(@Nullable Double windowWidth) {
		this.windowWidth = windowWidth;
	}

	@Nullable
	public Double getWindowHeight() {
		return this.windowHeight;
	}

	public void setWindowHeight(@Nullable Double windowHeight) {
		this.windowHeight = windowHeight;
	}

	@Nullable
	public Integer getNavigatorMaxTouchPoints() {
		return this.navigatorMaxTouchPoints;
	}

	public void setNavigatorMaxTouchPoints(@Nullable Integer navigatorMaxTouchPoints) {
		this.navigatorMaxTouchPoints = navigatorMaxTouchPoints;
	}

	@Nullable
	public String getDocumentVisibilityState() {
		return this.documentVisibilityState;
	}

	public void setDocumentVisibilityState(@Nullable String documentVisibilityState) {
		this.documentVisibilityState = documentVisibilityState;
	}
}
