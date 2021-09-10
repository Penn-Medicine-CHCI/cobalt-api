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

package com.cobaltplatform.api.integration.gcal;

import com.cobaltplatform.api.util.WebUtility;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class GoogleCalendarUrlGenerator {
	@Nonnull
	private final DateTimeFormatter dateTimeFormatter;

	public GoogleCalendarUrlGenerator() {
		this.dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmSS").withZone(ZoneId.of("GMT"));
	}

	public String generateNewEventUrl(@Nonnull String title,
																		@Nullable String description,
																		@Nonnull LocalDateTime startDateTime,
																		@Nonnull LocalDateTime endDateTime,
																		@Nonnull ZoneId timeZone,
																		@Nonnull String location) {
		requireNonNull(title);
		requireNonNull(startDateTime);
		requireNonNull(endDateTime);
		requireNonNull(timeZone);
		requireNonNull(location);

		description = trimToNull(description);

		// See https://github.com/InteractionDesignFoundation/add-event-to-calendar-docs/blob/master/services/google.md
		// See https://stackoverflow.com/questions/22757908/what-parameters-are-required-to-create-an-add-to-google-calendar-link
		StringBuilder url = new StringBuilder("https://www.google.com/calendar/render?action=TEMPLATE");

		url.append("&text=");
		url.append(WebUtility.urlEncode(title));

		if (description != null) {
			url.append("&details=");
			url.append(WebUtility.urlEncode(description));
		}

		// YYYYMMDDTHHmmSS/YYYYMMDDTHHmmSS
		// dates=20201231T193000/20201231T223000
		url.append("&dates=");
		url.append(WebUtility.urlEncode(getDateTimeFormatter().format(startDateTime.atZone(ZoneId.of("GMT")))));
		url.append(WebUtility.urlEncode("/"));
		url.append(WebUtility.urlEncode(getDateTimeFormatter().format(endDateTime.atZone(ZoneId.of("GMT")))));

		url.append("&location=");
		url.append(WebUtility.urlEncode(location));

		url.append("&ctz=");
		url.append(WebUtility.urlEncode(timeZone.getId()));

		return url.toString();
	}

	@Nonnull
	protected DateTimeFormatter getDateTimeFormatter() {
		return dateTimeFormatter;
	}
}
