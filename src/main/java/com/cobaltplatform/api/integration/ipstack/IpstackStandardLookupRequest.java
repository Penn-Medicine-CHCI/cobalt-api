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

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * Normalized/simplified representation of https://support.google.com/analytics/answer/7029846?hl=en.
 *
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class IpstackStandardLookupRequest {
	@Nonnull
	private final String ipAddress;
	@Nonnull
	private final Set<String> fields;
	@Nonnull
	private final Boolean hostname;
	@Nonnull
	private final Boolean security;
	@Nullable
	private final String language;

	@Nonnull
	public static Builder withIpAddress(@Nonnull String ipAddress) {
		requireNonNull(ipAddress);
		return new Builder(ipAddress);
	}

	protected IpstackStandardLookupRequest(@Nonnull Builder builder) {
		requireNonNull(builder);

		this.ipAddress = builder.ipAddress;
		this.fields = builder.fields == null ? Set.of() : Collections.unmodifiableSet(new HashSet<>(builder.fields));
		this.hostname = builder.hostname == null ? false : builder.hostname;
		this.security = builder.security == null ? false : builder.security;
		this.language = builder.language;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}

	@NotThreadSafe
	public static class Builder {
		@Nonnull
		private final String ipAddress;
		@Nullable
		private Set<String> fields;
		@Nullable
		private Boolean hostname;
		@Nullable
		private Boolean security;
		@Nullable
		private String language;

		Builder(@Nonnull String ipAddress) {
			requireNonNull(ipAddress);
			this.ipAddress = ipAddress;
		}

		@Nonnull
		public Builder fields(@Nullable Set<String> fields) {
			this.fields = fields;
			return this;
		}

		@Nonnull
		public Builder hostname(@Nullable Boolean hostname) {
			this.hostname = hostname;
			return this;
		}

		@Nonnull
		public Builder security(@Nullable Boolean security) {
			this.security = security;
			return this;
		}

		@Nonnull
		public Builder language(@Nullable String language) {
			this.language = language;
			return this;
		}

		@Nonnull
		public IpstackStandardLookupRequest build() {
			return new IpstackStandardLookupRequest(this);
		}
	}

	@Nonnull
	public String getIpAddress() {
		return this.ipAddress;
	}

	@Nonnull
	public Set<String> getFields() {
		return this.fields;
	}

	@Nonnull
	public Boolean getHostname() {
		return this.hostname;
	}

	@Nonnull
	public Boolean getSecurity() {
		return this.security;
	}

	@Nonnull
	public Optional<String> getLanguage() {
		return Optional.ofNullable(this.language);
	}
}