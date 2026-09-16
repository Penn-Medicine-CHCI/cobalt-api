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

package com.cobaltplatform.api.util;

import com.cobaltplatform.api.UnitTest;
import com.cobaltplatform.api.cache.Cache;
import com.cobaltplatform.api.cache.CaffeineCache;
import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.model.api.response.EncounterApiResponse;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.service.Encounter;
import com.lokalized.Strings;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;

import static org.junit.Assert.assertEquals;

@Category(UnitTest.class)
public class FormatterTests {
	@Test
	public void apiDateDescriptionsUseCanonicalFormat() {
		ZoneId timeZone = ZoneId.of("America/New_York");
		Formatter formatter = formatter(Locale.FRANCE, timeZone);

		assertEquals("Thu, Sep 10, 2026", formatter.formatDateDescription(LocalDate.of(2026, 9, 10)));
		assertEquals("Thu, Sep 10, 2026 7:00 pm",
				formatter.formatDateTimeDescription(LocalDateTime.of(2026, 9, 10, 19, 0)));
		assertEquals("Thu, Sep 10, 2026 7:00 pm",
				formatter.formatTimestampDescription(Instant.parse("2026-09-10T23:00:00Z")));
		assertEquals("Fri, Sep 11, 2026 8:00 am",
				formatter.formatTimestampDescription(Instant.parse("2026-09-10T23:00:00Z"), ZoneId.of("Asia/Tokyo")));
	}

	@Test
	public void apiResponseReturnsCanonicalDateDescriptions() {
		Encounter encounter = new Encounter();
		encounter.setCsn("test-csn");
		encounter.setStatus("finished");
		encounter.setPeriodStart(LocalDateTime.of(2026, 9, 10, 19, 0));
		encounter.setPeriodEnd(LocalDateTime.of(2026, 9, 10, 20, 30));

		EncounterApiResponse response = new EncounterApiResponse(formatter(Locale.US, ZoneId.of("America/New_York")),
				strings(), encounter);

		assertEquals("Thu, Sep 10, 2026 7:00 pm", response.getPeriodStartDescription());
		assertEquals("Thu, Sep 10, 2026 8:30 pm", response.getPeriodEndDescription());
	}

	private Formatter formatter(Locale locale, ZoneId timeZone) {
		CurrentContext currentContext = new CurrentContext.Builder(InstitutionId.COBALT, locale, timeZone).build();
		Cache cache = new CaffeineCache(10);
		return new Formatter(cache, () -> currentContext, () -> cache, strings());
	}

	private Strings strings() {
		return (Strings) Proxy.newProxyInstance(Strings.class.getClassLoader(), new Class[]{Strings.class},
				(proxy, method, arguments) -> arguments == null || arguments.length == 0 ? null : arguments[0]);
	}
}
