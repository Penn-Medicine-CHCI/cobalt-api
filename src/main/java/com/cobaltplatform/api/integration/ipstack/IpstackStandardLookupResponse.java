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

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * See documentation at https://ipstack.com/documentation.
 *
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class IpstackStandardLookupResponse {
	@Nullable
	private String rawJson;

	// TODO: other fields

	// {
	//  "ip": "134.201.250.155",
	//  "type": "ipv4",
	//  "continent_code": "NA",
	//  "continent_name": "North America",
	//  "country_code": "US",
	//  "country_name": "United States",
	//  "region_code": "CA",
	//  "region_name": "California",
	//  "city": "Los Angeles",
	//  "zip": "90013",
	//  "latitude": 34.0655,
	//  "longitude": -118.2405,
	//  "msa": "31100",
	//  "dma": "803",
	//  "radius": null,
	//  "ip_routing_type": null,
	//  "connection_type": null,
	//  "location": {
	//    "geoname_id": 5368361,
	//    "capital": "Washington D.C.",
	//    "languages": [
	//        {
	//          "code": "en",
	//          "name": "English",
	//          "native": "English"
	//        }
	//    ],
	//    "country_flag": "https://assets.ipstack.com/images/assets/flags_svg/us.svg",
	//    "country_flag_emoji": "🇺🇸",
	//    "country_flag_emoji_unicode": "U+1F1FA U+1F1F8",
	//    "calling_code": "1",
	//    "is_eu": false
	//  },
	//  "time_zone": {
	//    "id": "America/Los_Angeles",
	//    "current_time": "2018-03-29T07:35:08-07:00",
	//    "gmt_offset": -25200,
	//    "code": "PDT",
	//    "is_daylight_saving": true
	//  },
	//  "currency": {
	//    "code": "USD",
	//    "name": "US Dollar",
	//    "plural": "US dollars",
	//    "symbol": "$",
	//    "symbol_native": "$"
	//  },
	//  "connection": {
	//    "asn": 25876,
	//    "isp": "Los Angeles Department of Water & Power",
	//    "sld": "ladwp",
	//    "tld": "com",
	//    "carrier": "los angeles department of water & power",
	//    "home": null,
	//    "organization_type": null,
	//    "isic_code": null,
	//    "naics_code": null
	//  }
	// }


	@Nullable
	public String getRawJson() {
		return this.rawJson;
	}

	public void setRawJson(@Nullable String rawJson) {
		this.rawJson = rawJson;
	}
}
