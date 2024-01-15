package com.cobaltplatform.api.service;

import com.cobaltplatform.api.Configuration;
import com.cobaltplatform.api.util.db.DatabaseProvider;
import com.lokalized.Strings;
import com.pyranid.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Instant;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify LLC.
 */
@Singleton
public class FingerprintService {
	@Nonnull
	private final DatabaseProvider databaseProvider;
	@Nonnull
	private final Configuration configuration;
	@Nonnull
	private final Strings strings;
	@Nonnull
	private final Logger logger;


	@Inject
	public FingerprintService(@Nonnull DatabaseProvider databaseProvider,
														@Nonnull Configuration configuration,
														@Nonnull Strings strings) {
		requireNonNull(databaseProvider);
		requireNonNull(configuration);
		requireNonNull(strings);

		this.databaseProvider = databaseProvider;
		this.configuration = configuration;
		this.strings = strings;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	public void storeFingerprintForAccount(@Nonnull UUID accountId, @Nonnull String fingerprintId) {
		getDatabase().execute("INSERT INTO account_fingerprint (account_id, fingerprint_id) VALUES (?,?) " +
				"ON CONFLICT ON CONSTRAINT account_fingerprint_key DO UPDATE SET last_updated=?", accountId, fingerprintId, Instant.now());
	}

	@Nonnull
	protected Database getDatabase() {
		return this.databaseProvider.get();
	}

	@Nonnull
	protected Configuration getConfiguration() {
		return configuration;
	}

	@Nonnull
	protected Strings getStrings() {
		return strings;
	}

	@Nonnull
	protected Logger getLogger() {
		return logger;
	}
}
