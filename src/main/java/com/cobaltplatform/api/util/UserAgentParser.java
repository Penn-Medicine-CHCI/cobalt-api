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

import ua_parser.Client;
import ua_parser.Parser;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class UserAgentParser {
	@Nonnull
	private final Parser parser;
	@Nonnull
	private final Set<String> searchBotUserAgentIds;

	public UserAgentParser() {
		try {
			this.parser = new Parser();
		} catch (IOException e) {
			throw new UncheckedIOException("Unable to initialize ua_parser library", e);
		}

		this.searchBotUserAgentIds = unmodifiableSet(provideSearchBotUserAgentIds());
	}

	@Nonnull
	public UserAgent parse(@Nonnull HttpServletRequest httpServletRequest) {
		requireNonNull(httpServletRequest);
		return parse(httpServletRequest.getHeader("User-Agent"));
	}

	@Nonnull
	public UserAgent parse(@Nullable String userAgent) {
		userAgent = trimToNull(userAgent);

		if (userAgent == null)
			return new UserAgent.Builder(UserAgentType.UNKNOWN).build();

		// "By hand" search engine bot detection
		Optional<UserAgent> searchBotUserAgent = parseForSearchBot(userAgent);

		if (searchBotUserAgent.isPresent())
			return searchBotUserAgent.get();

		// Looks like it wasn't anything we recognize - assume it's a browser and let the library parse it
		Client client = parser.parse(userAgent);

		UserAgentType userAgentType = UserAgentType.WEB_BROWSER;

		if (userAgent.contains("Googlebot")
				|| userAgent.contains("Bingbot")
				|| userAgent.contains("Yahoo! Slurp")
				|| userAgent.contains("DuckDuckBot")
				|| userAgent.contains("Baiduspider")
				|| userAgent.contains("YandexBot")
				|| userAgent.contains("ia_archiver")
				|| userAgent.contains("facebot")
				|| userAgent.contains("facebookexternalhit"))
			userAgentType = UserAgentType.SEARCH_BOT;

		return new UserAgent.Builder(userAgentType)
				.userAgent(userAgent)
				.browserFamily("Other".equals(client.userAgent.family) ? null : client.userAgent.family)
				.browserMajorVersion(client.userAgent.major)
				.browserMinorVersion(client.userAgent.minor)
				.browserPatchVersion(client.userAgent.patch)
				.operatingSystemName("Other".equals(client.os.family) ? null : client.os.family)
				.operatingSystemMajorVersion(client.os.major)
				.operatingSystemMinorVersion(client.os.minor)
				.operatingSystemPatchVersion(client.os.patch)
				.operatingSystemPatchMinorVersion(client.os.patchMinor)
				.deviceFamily("Other".equals(client.device.family) ? null : client.device.family)
				.build();
	}

	protected Optional<UserAgent> parseForSearchBot(@Nonnull String userAgent) {
		requireNonNull(userAgent);

		for (String searchBotUserAgentId : getSearchBotUserAgentIds()) {
			if (userAgent.contains(searchBotUserAgentId))
				return Optional.of(new UserAgent.Builder(UserAgentType.SEARCH_BOT)
						.userAgent(userAgent)
						.deviceFamily(searchBotUserAgentId)
						.build());
		}

		return Optional.empty();
	}

	@Nonnull
	protected Set<String> getSearchBotUserAgentIds() {
		return this.searchBotUserAgentIds;
	}

	@Nonnull
	protected Set<String> provideSearchBotUserAgentIds() {
		return new LinkedHashSet<String>() {{
			add("Googlebot");
			add("Bingbot");
			add("Yahoo! Slurp");
			add("DuckDuckBot");
			add("Baiduspider");
			add("YandexBot");
			add("ia_archiver");
			add("facebot");
			add("facebookexternalhit");
		}};
	}
}