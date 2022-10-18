package com.cobaltplatform.api.integration.epic;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public class EpicConfiguration {
	@Nonnull
	private final String clientId;
	@Nonnull
	private final String baseUrl;
	@Nonnull
	private final Boolean permitUnsafeCerts;
	@Nullable
	private final String userId;
	@Nullable
	private final String userIdType;
	@Nullable
	private final String username;
	@Nullable
	private final String password;

	protected EpicConfiguration(@Nonnull EpicConfiguration.Builder builder) {
		requireNonNull(builder);

		this.clientId = builder.clientId;
		this.baseUrl = builder.baseUrl;
		this.permitUnsafeCerts = builder.permitUnsafeCerts == null ? false : builder.permitUnsafeCerts;
		this.userId = builder.userId;
		this.userIdType = builder.userIdType;
		this.username = builder.username;
		this.password = builder.password;
	}

	@Nonnull
	public String getClientId() {
		return this.clientId;
	}

	@Nonnull
	public String getBaseUrl() {
		return this.baseUrl;
	}

	@Nonnull
	public Boolean getPermitUnsafeCerts() {
		return this.permitUnsafeCerts;
	}

	@Nonnull
	public Optional<String> getUserId() {
		return Optional.ofNullable(this.userId);
	}

	@Nonnull
	public Optional<String> getUserIdType() {
		return Optional.ofNullable(this.userIdType);
	}

	@Nonnull
	public Optional<String> getUsername() {
		return Optional.ofNullable(this.username);
	}

	@Nonnull
	public Optional<String> getPassword() {
		return Optional.ofNullable(this.password);
	}

	@NotThreadSafe
	public static class Builder {
		@Nonnull
		private final String clientId;
		@Nonnull
		private final String baseUrl;
		@Nullable
		private Boolean permitUnsafeCerts;
		@Nullable
		private String userId;
		@Nullable
		private String userIdType;
		@Nullable
		private String username;
		@Nullable
		private String password;

		@Nonnull
		public Builder(@Nonnull String clientId,
									 @Nonnull String baseUrl) {
			requireNonNull(clientId);
			requireNonNull(baseUrl);

			this.clientId = clientId;
			this.baseUrl = baseUrl;
		}

		@Nonnull
		public Builder permitUnsafeCerts(@Nullable Boolean permitUnsafeCerts) {
			this.permitUnsafeCerts = permitUnsafeCerts;
			return this;
		}

		@Nonnull
		public Builder userId(@Nullable String userId) {
			this.userId = userId;
			return this;
		}

		@Nonnull
		public Builder userIdType(@Nullable String userIdType) {
			this.userIdType = userIdType;
			return this;
		}

		@Nonnull
		public Builder username(@Nullable String username) {
			this.username = username;
			return this;
		}

		@Nonnull
		public Builder password(@Nullable String password) {
			this.password = password;
			return this;
		}

		@Nonnull
		public EpicConfiguration build() {
			return new EpicConfiguration(this);
		}
	}
}
