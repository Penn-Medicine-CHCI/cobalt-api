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

package com.cobaltplatform.api.integration.beiwe;

import com.cobaltplatform.api.model.db.AccountStudy;
import com.cobaltplatform.api.model.db.EncryptionKeypair;
import com.google.gson.Gson;
import com.pyranid.Database;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.soklet.util.LoggingUtils.initializeLogback;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class BeiweDataDownloader {
	@BeforeClass
	public static void initialize() {
		initializeLogback(Paths.get("config/local/logback.xml"));
	}

	@Test
	public void downloadBeiweData() {
		Logger logger = LoggerFactory.getLogger(getClass());
		Path configFile = Path.of("resources/test/beiwe-data-downloader-config.json");
		BeiweDataDownloaderConfig beiweDataDownloaderConfig = loadBeiweDataDownloaderConfigFromFile(configFile);

		performOperationWithDatabase(beiweDataDownloaderConfig, (database) -> {
			UUID studyId = database.queryForObject("""
					SELECT study_id
					FROM study
					WHERE url_name=?
					""", UUID.class, beiweDataDownloaderConfig.getStudyUrlName()).get();

			logger.debug("Processing study ID {} (URL name {})...", studyId, beiweDataDownloaderConfig.getStudyUrlName());

			List<AccountStudy> accountStudies = database.queryForList("""
					SELECT *
					FROM account_study
					WHERE study_id=?
					""", AccountStudy.class, studyId);

			logger.debug("There are {} accounts for this study.", accountStudies.size());

			List<EncryptionKeypair> encryptionKeypairs = database.queryForList("""
					SELECT ek.*
					FROM encryption_keypair ek, account_study acs
					WHERE ek.encryption_keypair_id=acs.encryption_keypair_id
					AND acs.study_id=?
					""", EncryptionKeypair.class, studyId);

			Map<UUID, EncryptionKeypair> encryptionKeypairsById = encryptionKeypairs.stream()
					.collect(Collectors.toMap(EncryptionKeypair::getEncryptionKeypairId, Function.identity()));


		});
	}

	@Nonnull
	protected BeiweDataDownloaderConfig loadBeiweDataDownloaderConfigFromFile(@Nonnull Path configFile) {
		requireNonNull(configFile);

		if (!Files.exists(configFile))
			throw new IllegalArgumentException(format("File at %s does not exist", configFile.toAbsolutePath()));

		try {
			String beiweDataDownloaderConfigJson = Files.readString(configFile, StandardCharsets.UTF_8);
			return new Gson().fromJson(beiweDataDownloaderConfigJson, BeiweDataDownloaderConfig.class);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	protected void performOperationWithDatabase(@Nonnull BeiweDataDownloaderConfig beiweDataDownloaderConfig,
																							@Nonnull Consumer<Database> databaseConsumer) {
		requireNonNull(beiweDataDownloaderConfig);
		requireNonNull(databaseConsumer);

		try (HikariDataSource dataSource = new HikariDataSource(new HikariConfig() {
			{
				setJdbcUrl(beiweDataDownloaderConfig.getJdbcUrl());
				setUsername(beiweDataDownloaderConfig.getJdbcUsername());
				setPassword(beiweDataDownloaderConfig.getJdbcPassword());
				setMaximumPoolSize(1);
			}
		})) {
			Database database = Database.forDataSource(dataSource)
					.timeZone(ZoneId.of("UTC"))
					.build();

			databaseConsumer.accept(database);
		}
	}

	@NotThreadSafe
	protected static class BeiweDataDownloaderConfig {
		@Nullable
		private String studyUrlName;
		@Nullable
		private String jdbcUrl;
		@Nullable
		private String jdbcUsername;
		@Nullable
		private String jdbcPassword;

		@Nullable
		public String getStudyUrlName() {
			return this.studyUrlName;
		}

		public void setStudyUrlName(@Nullable String studyUrlName) {
			this.studyUrlName = studyUrlName;
		}

		@Nullable
		public String getJdbcUrl() {
			return this.jdbcUrl;
		}

		public void setJdbcUrl(@Nullable String jdbcUrl) {
			this.jdbcUrl = jdbcUrl;
		}

		@Nullable
		public String getJdbcUsername() {
			return this.jdbcUsername;
		}

		public void setJdbcUsername(@Nullable String jdbcUsername) {
			this.jdbcUsername = jdbcUsername;
		}

		@Nullable
		public String getJdbcPassword() {
			return this.jdbcPassword;
		}

		public void setJdbcPassword(@Nullable String jdbcPassword) {
			this.jdbcPassword = jdbcPassword;
		}
	}
}
