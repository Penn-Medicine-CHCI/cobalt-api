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

import com.cobaltplatform.api.http.HttpClient;
import com.cobaltplatform.api.http.HttpRequest;
import com.cobaltplatform.api.http.HttpResponse;
import org.junit.Assert;
import org.junit.Test;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.nio.charset.StandardCharsets;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class IpstackClientTests {
	@Test
	public void testStandardLookupParsesSuccessfulResponse() {
		IpstackClient ipstackClient = new TestIpstackClient("""
				{
				  "ip": "134.201.250.155",
				  "hostname": "134.201.250.155",
				  "type": "ipv4",
				  "continent_code": "NA",
				  "continent_name": "North America",
				  "country_code": "US",
				  "country_name": "United States",
				  "region_code": "CA",
				  "region_name": "California",
				  "city": "Los Angeles",
				  "zip": "90013",
				  "latitude": 34.0655,
				  "longitude": -118.2405,
				  "location": {
				    "geoname_id": 5368361,
				    "capital": "Washington D.C.",
				    "languages": [{"code": "en", "name": "English", "native": "English"}],
				    "country_flag": "https://assets.ipstack.com/images/assets/flags_svg/us.svg",
				    "country_flag_emoji": "",
				    "country_flag_emoji_unicode": "U+1F1FA U+1F1F8",
				    "calling_code": "1",
				    "is_eu": false
				  },
				  "time_zone": {
				    "id": "America/Los_Angeles",
				    "current_time": "2024-06-14T01:45:35-07:00",
				    "gmt_offset": -25200,
				    "code": "PDT",
				    "is_daylight_saving": true
				  },
				  "currency": {
				    "code": "USD",
				    "name": "US Dollar",
				    "plural": "US dollars",
				    "symbol": "$",
				    "symbol_native": "$"
				  },
				  "connection": {
				    "asn": 25876,
				    "isp": "Los Angeles Department of Water & Power",
				    "sld": "ladwp",
				    "tld": "com",
				    "carrier": "los angeles department of water & power",
				    "home": null,
				    "organization_type": null,
				    "isic_code": null,
				    "naics_code": null
				  },
				  "security": {
				    "is_proxy": false,
				    "proxy_type": null,
				    "is_crawler": false,
				    "crawler_name": null,
				    "crawler_type": null,
				    "is_tor": false,
				    "threat_level": "low",
				    "threat_types": null,
				    "proxy_last_detected": null,
				    "proxy_level": null,
				    "vpn_service": null,
				    "anonymizer_status": null,
				    "hosting_facility": false
				  }
				}
				""");

		IpstackStandardLookupResponse response = ipstackClient.performStandardLookup(
				IpstackStandardLookupRequest.withIpAddress("134.201.250.155")
						.hostname(true)
						.security(true)
						.language("en")
						.build());

		Assert.assertNotNull(response);
		Assert.assertFalse(response.hasError());
		Assert.assertEquals("US", response.getCountryCode());
		Assert.assertEquals("Los Angeles", response.getCity());
		Assert.assertEquals("America/Los_Angeles", response.getTimeZone().getId());
		Assert.assertEquals(Long.valueOf(25876), response.getConnection().getAsn());
		Assert.assertEquals("low", response.getSecurity().getThreatLevel());
		Assert.assertNotNull(response.getRawJson());
	}

	@Test
	public void testStandardLookupParsesLogicalErrorResponse() {
		IpstackClient ipstackClient = new TestIpstackClient("""
				{
				  "success": false,
				  "error": {
				    "code": 104,
				    "type": "monthly_limit_reached",
				    "info": "Your monthly API request volume has been reached. Please upgrade your plan."
				  }
				}
				""");

		IpstackStandardLookupResponse response = ipstackClient.performStandardLookup(
				IpstackStandardLookupRequest.withIpAddress("134.201.250.155").build());

		Assert.assertTrue(response.hasError());
		Assert.assertEquals(Integer.valueOf(104), response.getError().getCode());
		Assert.assertEquals("monthly_limit_reached", response.getError().getType());
		Assert.assertEquals("Your monthly API request volume has been reached. Please upgrade your plan.", response.getError().getMessage());
	}

	@ThreadSafe
	protected static class TestIpstackClient extends DefaultIpstackClient {
		@Nonnull
		private final HttpClient httpClient;

		public TestIpstackClient(@Nonnull String responseBody) {
			super("test-access-key");
			this.httpClient = new StaticHttpClient(responseBody);
		}

		@Nonnull
		@Override
		protected HttpClient getHttpClient() {
			return this.httpClient;
		}
	}

	@ThreadSafe
	protected static class StaticHttpClient implements HttpClient {
		@Nonnull
		private final String responseBody;

		public StaticHttpClient(@Nonnull String responseBody) {
			this.responseBody = responseBody;
		}

		@Nonnull
		@Override
		public HttpResponse execute(@Nonnull HttpRequest httpRequest,
																com.cobaltplatform.api.http.HttpRequestOption... httpRequestOptions) {
			return new HttpResponse(200, this.responseBody.getBytes(StandardCharsets.UTF_8));
		}
	}
}
