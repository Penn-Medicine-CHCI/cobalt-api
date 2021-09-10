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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@Immutable
public class UserAgent {
	@Nonnull
	private final UserAgentType userAgentType;
	@Nullable
	private final String userAgent;
	@Nullable
	private final String browserFamily;
	@Nullable
	private final String browserVersion;
	@Nullable
	private final String browserMajorVersion;
	@Nullable
	private final String browserMinorVersion;
	@Nullable
	private final String browserPatchVersion;
	@Nullable
	private final String operatingSystemName;
	@Nullable
	private final String operatingSystemVersion;
	@Nullable
	private final String operatingSystemMajorVersion;
	@Nullable
	private final String operatingSystemMinorVersion;
	@Nullable
	private final String operatingSystemPatchVersion;
	@Nullable
	private final String operatingSystemPatchMinorVersion;
	@Nonnull
	private final String deviceFamily;

	private UserAgent(@Nonnull UserAgentType userAgentType,
										@Nullable String userAgent,
										@Nullable String browserFamily,
										@Nullable String browserMajorVersion,
										@Nullable String browserMinorVersion,
										@Nullable String browserPatchVersion,
										@Nullable String operatingSystemName,
										@Nullable String operatingSystemMajorVersion,
										@Nullable String operatingSystemMinorVersion,
										@Nullable String operatingSystemPatchVersion,
										@Nullable String operatingSystemPatchMinorVersion,
										@Nullable String deviceFamily) {
		requireNonNull(userAgentType);

		this.userAgentType = userAgentType;
		this.userAgent = trimToNull(userAgent);
		this.browserFamily = trimToNull(browserFamily);
		this.browserMajorVersion = trimToNull(browserMajorVersion);
		this.browserMinorVersion = trimToNull(browserMinorVersion);
		this.browserPatchVersion = trimToNull(browserPatchVersion);
		this.operatingSystemName = trimToNull(operatingSystemName);
		this.operatingSystemMajorVersion = trimToNull(operatingSystemMajorVersion);
		this.operatingSystemMinorVersion = trimToNull(operatingSystemMinorVersion);
		this.operatingSystemPatchVersion = trimToNull(operatingSystemPatchVersion);
		this.operatingSystemPatchMinorVersion = trimToNull(operatingSystemPatchMinorVersion);
		this.deviceFamily = trimToNull(deviceFamily);

		this.browserVersion = extractVersion(this.browserMajorVersion, this.browserMinorVersion, this.browserPatchVersion).orElse(null);
		this.operatingSystemVersion = extractVersion(this.operatingSystemMajorVersion, this.operatingSystemMinorVersion,
				this.operatingSystemPatchVersion, this.operatingSystemPatchMinorVersion).orElse(null);
	}

	@Nonnull
	protected Optional<String> extractVersion(@Nullable String... versionComponents) {
		if (versionComponents == null)
			return Optional.empty();

		// Ensure any version components that are blank are coerced to null
		List<String> versionComponentsAsList = Arrays.stream(versionComponents)
				.map(versionComponent -> trimToNull(versionComponent))
				.collect(toList());

		// Remove trailing null components so they don't appear in the output (e.g. perhaps we don't have a patch version)
		int trailingNullComponentCount = 0;

		for (int i = versionComponentsAsList.size() - 1; i > 0; --i) {
			if (versionComponentsAsList.get(i) == null)
				++trailingNullComponentCount;
			else
				break;
		}

		if (trailingNullComponentCount > 0)
			versionComponentsAsList = versionComponentsAsList.subList(0, versionComponentsAsList.size() - trailingNullComponentCount - 1);

		// Join together remaining version elements using ".", with gaps represented by "?"
		// e.g. ["1", null, "3"] would be "1.?.3"
		// The alternative would be to assume null means "0", which hides the fact that data is missing
		String version = versionComponentsAsList.stream()
				.map(versionComponent -> versionComponent == null ? "?" : versionComponent)
				.collect(joining("."));

		return Optional.ofNullable(trimToNull(version));
	}

	@Override
	@Nonnull
	public String toString() {
		return getDescription();
	}

	/**
	 * Returns a human-readable description of the user agent information.
	 * For example: {@code Browser (Chrome Mobile 52.0.2743, Android 6.0, SM-G920V)}
	 *
	 * @return a human-readable description of the user agent information
	 */
	@Nonnull
	public String getDescription() {
		List<String> components = new ArrayList<>();
		UserAgentType userAgentType = getUserAgentType();
		String userAgentTypeDescription;

		if (userAgentType == UserAgentType.SEARCH_BOT)
			userAgentTypeDescription = "Search Bot";
		else if (userAgentType == UserAgentType.UNKNOWN)
			userAgentTypeDescription = "Unknown";
		else if (userAgentType == UserAgentType.WEB_BROWSER)
			userAgentTypeDescription = "Web Browser";
		else if (userAgentType == UserAgentType.NATIVE_APP)
			userAgentTypeDescription = "Native App";
		else
			throw new IllegalStateException(format("Unexpected value '%s' for %s", userAgentType, UserAgentType.class.getSimpleName()));

		getBrowserFamily().ifPresent((browserFamily) -> {
			StringBuilder browserDescription = new StringBuilder(browserFamily);
			getBrowserVersion().ifPresent((browserVersion) -> browserDescription.append(format(" %s", browserVersion)));
			components.add(browserDescription.toString());
		});

		getOperatingSystemName().ifPresent((operatingSystem) -> {
			StringBuilder operatingSystemDescription = new StringBuilder(operatingSystemName);
			getOperatingSystemVersion().ifPresent((operatingSystemVersion) -> operatingSystemDescription.append(format(" %s", operatingSystemVersion)));
			components.add(operatingSystemDescription.toString());
		});

		getDeviceFamily().ifPresent((deviceFamily) -> components.add(deviceFamily));

		String joinedComponents = String.join(", ", components);

		return joinedComponents.length() > 0 ? format("%s (%s)", userAgentTypeDescription, joinedComponents) : userAgentTypeDescription;
	}

	@Nonnull
	public UserAgentType getUserAgentType() {
		return this.userAgentType;
	}

	@Nonnull
	public Optional<String> getUserAgent() {
		return Optional.ofNullable(this.userAgent);
	}

	@Nonnull
	public Optional<String> getBrowserFamily() {
		return Optional.ofNullable(this.browserFamily);
	}

	@Nonnull
	public Optional<String> getBrowserVersion() {
		return Optional.ofNullable(this.browserVersion);
	}

	@Nonnull
	public Optional<String> getBrowserMajorVersion() {
		return Optional.ofNullable(this.browserMajorVersion);
	}

	@Nonnull
	public Optional<String> getBrowserMinorVersion() {
		return Optional.ofNullable(this.browserMinorVersion);
	}

	@Nonnull
	public Optional<String> getBrowserPatchVersion() {
		return Optional.ofNullable(this.browserPatchVersion);
	}

	@Nonnull
	public Optional<String> getOperatingSystemName() {
		return Optional.ofNullable(this.operatingSystemName);
	}

	@Nonnull
	public Optional<String> getOperatingSystemVersion() {
		return Optional.ofNullable(this.operatingSystemVersion);
	}

	@Nonnull
	public Optional<String> getOperatingSystemMajorVersion() {
		return Optional.ofNullable(this.operatingSystemMajorVersion);
	}

	@Nonnull
	public Optional<String> getOperatingSystemMinorVersion() {
		return Optional.ofNullable(this.operatingSystemMinorVersion);
	}

	@Nonnull
	public Optional<String> getOperatingSystemPatchVersion() {
		return Optional.ofNullable(this.operatingSystemPatchVersion);
	}

	@Nonnull
	public Optional<String> getOperatingSystemPatchMinorVersion() {
		return Optional.ofNullable(this.operatingSystemPatchMinorVersion);
	}

	@Nonnull
	public Optional<String> getDeviceFamily() {
		return Optional.ofNullable(this.deviceFamily);
	}

	@NotThreadSafe
	public static class Builder {
		@Nonnull
		private final UserAgentType userAgentType;
		@Nullable
		private String userAgent;
		@Nullable
		private String browserFamily;
		@Nullable
		private String browserMajorVersion;
		@Nullable
		private String browserMinorVersion;
		@Nullable
		private String browserPatchVersion;
		@Nullable
		private String operatingSystemName;
		@Nullable
		private String operatingSystemMajorVersion;
		@Nullable
		private String operatingSystemMinorVersion;
		@Nullable
		private String operatingSystemPatchVersion;
		@Nullable
		private String operatingSystemPatchMinorVersion;
		@Nonnull
		private String deviceFamily;

		public Builder(@Nonnull UserAgentType userAgentType) {
			requireNonNull(userAgentType);
			this.userAgentType = userAgentType;
		}

		@Nonnull
		public Builder userAgent(@Nullable String userAgent) {
			this.userAgent = userAgent;
			return this;
		}

		@Nonnull
		public Builder browserFamily(@Nullable String browserFamily) {
			this.browserFamily = browserFamily;
			return this;
		}

		@Nonnull
		public Builder browserMajorVersion(@Nullable String browserMajorVersion) {
			this.browserMajorVersion = browserMajorVersion;
			return this;
		}

		@Nonnull
		public Builder browserMinorVersion(@Nullable String browserMinorVersion) {
			this.browserMinorVersion = browserMinorVersion;
			return this;
		}

		@Nonnull
		public Builder browserPatchVersion(@Nullable String browserPatchVersion) {
			this.browserPatchVersion = browserPatchVersion;
			return this;
		}

		@Nonnull
		public Builder operatingSystemName(@Nullable String operatingSystemName) {
			this.operatingSystemName = operatingSystemName;
			return this;
		}

		@Nonnull
		public Builder operatingSystemMajorVersion(@Nullable String operatingSystemMajorVersion) {
			this.operatingSystemMajorVersion = operatingSystemMajorVersion;
			return this;
		}

		@Nonnull
		public Builder operatingSystemMinorVersion(@Nullable String operatingSystemMinorVersion) {
			this.operatingSystemMinorVersion = operatingSystemMinorVersion;
			return this;
		}

		@Nonnull
		public Builder operatingSystemPatchVersion(@Nullable String operatingSystemPatchVersion) {
			this.operatingSystemPatchVersion = operatingSystemPatchVersion;
			return this;
		}

		@Nonnull
		public Builder operatingSystemPatchMinorVersion(@Nullable String operatingSystemPatchMinorVersion) {
			this.operatingSystemPatchMinorVersion = operatingSystemPatchMinorVersion;
			return this;
		}

		@Nonnull
		public Builder deviceFamily(@Nullable String deviceFamily) {
			this.deviceFamily = deviceFamily;
			return this;
		}

		@Nonnull
		public UserAgent build() {
			return new UserAgent(this.userAgentType, this.userAgent, this.browserFamily, this.browserMajorVersion,
					this.browserMinorVersion, this.browserPatchVersion, this.operatingSystemName,
					this.operatingSystemMajorVersion, this.operatingSystemMinorVersion,
					this.operatingSystemPatchVersion, this.operatingSystemPatchMinorVersion,
					this.deviceFamily);
		}
	}
}