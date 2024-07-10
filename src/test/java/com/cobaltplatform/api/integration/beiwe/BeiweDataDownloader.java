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
import com.cobaltplatform.api.model.db.FileUpload;
import com.cobaltplatform.api.model.db.ScreeningSession;
import com.cobaltplatform.api.util.CryptoUtility;
import com.google.gson.Gson;
import com.pyranid.Database;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.cobaltplatform.api.util.DatabaseUtility.sqlInListPlaceholders;
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

		boolean downloadAssessments = true;
		boolean downloadActive = true;
		boolean downloadPassive = true;

		performOperationWithDatabase(beiweDataDownloaderConfig, (database) -> {
			UUID studyId = database.queryForObject("""
					SELECT study_id
					FROM study
					WHERE url_name=?
					""", UUID.class, beiweDataDownloaderConfig.getStudyUrlName()).get();

			logger.debug("Processing study ID {} (URL name {})...", studyId, beiweDataDownloaderConfig.getStudyUrlName());

			List<AccountStudy> accountStudies;

			if (beiweDataDownloaderConfig.getRestrictToUsernames().size() > 0) {
				logger.info("Restricting data pull to usernames: {}", beiweDataDownloaderConfig.getRestrictToUsernames());

				List<Object> parameters = new ArrayList<>();
				parameters.add(studyId);
				parameters.addAll(beiweDataDownloaderConfig.getRestrictToUsernames());

				accountStudies = database.queryForList(format("""
						SELECT acs.*
						FROM v_account_study acs, account a
						WHERE acs.study_id=?
						AND acs.account_id=a.account_id
						AND a.username IN %s
						ORDER BY a.username
						""", sqlInListPlaceholders(beiweDataDownloaderConfig.getRestrictToUsernames())), AccountStudy.class, parameters.toArray(new Object[]{}));
			} else {
				accountStudies = database.queryForList("""
						SELECT *
						FROM v_account_study
						WHERE study_id=?
						ORDER BY account_id
						""", AccountStudy.class, studyId);
			}

			logger.debug("There are {} accounts for this study.", accountStudies.size());

			List<EncryptionKeypair> encryptionKeypairs = database.queryForList("""
					SELECT ek.*
					FROM encryption_keypair ek, v_account_study acs
					WHERE ek.encryption_keypair_id=acs.encryption_keypair_id
					AND acs.study_id=?
					""", EncryptionKeypair.class, studyId);

			Map<UUID, EncryptionKeypair> encryptionKeypairsById = new HashMap<>(encryptionKeypairs.size());
			Map<UUID, PrivateKey> privateKeysByEncryptionKeypairId = new HashMap<>(encryptionKeypairs.size());

			for (EncryptionKeypair encryptionKeypair : encryptionKeypairs) {
				encryptionKeypairsById.put(encryptionKeypair.getEncryptionKeypairId(), encryptionKeypair);
				privateKeysByEncryptionKeypairId.put(encryptionKeypair.getEncryptionKeypairId(), CryptoUtility.toPrivateKey(encryptionKeypair.getPrivateKeyAsString()));
			}

			// This is using credentials for beiwe-data-downloader
			AwsBasicCredentials awsBasicCredentials = AwsBasicCredentials.create(
					beiweDataDownloaderConfig.getAmazonS3AccessKey(),
					beiweDataDownloaderConfig.getAmazonS3SecretKey()
			);

			S3Client s3Client = S3Client.builder().credentialsProvider(StaticCredentialsProvider.create(awsBasicCredentials))
					.region(Region.of(beiweDataDownloaderConfig.getAmazonS3Region()))
					.build();

			BeiweCryptoManager beiweCryptoManager = new BeiweCryptoManager();
			Map<String, BeiweDownloadResult> beiweDownloadResultsByUsername = new HashMap<>(accountStudies.size());

			int i = 0;

			for (AccountStudy accountStudy : accountStudies) {
				String username = database.queryForObject("SELECT username FROM account WHERE account_id=?", String.class, accountStudy.getAccountId()).get();

				BeiweDownloadResult beiweDownloadResult = new BeiweDownloadResult();
				beiweDownloadResultsByUsername.put(username, beiweDownloadResult);

				logger.debug("Processing username {} ({} of {})...", username, i + 1, accountStudies.size());

				// TODO: correct thing is to look at account_check_in_action_file_upload and study_file_upload, but those are missing test data prior to Feb 28 2024.
				List<FileUpload> fileUploads = database.queryForList("""
						SELECT *
						FROM file_upload
						WHERE account_id=?
						AND (storage_key LIKE 'file-uploads/account-check-in-actions/%' OR storage_key LIKE 'file-uploads/studies/%')
							""", FileUpload.class, accountStudy.getAccountId());

				beiweDownloadResult.totalFiles = fileUploads.size();

				Map<UUID, FileUpload> accountCheckInActionFileUploadsById = fileUploads.stream()
						.filter(fileUpload -> fileUpload.getStorageKey().startsWith("file-uploads/account-check-in-actions/"))
						.collect(Collectors.toMap(FileUpload::getFileUploadId, Function.identity()));

				Map<UUID, FileUpload> studyFileUploadsById = fileUploads.stream()
						.filter(fileUpload -> fileUpload.getStorageKey().startsWith("file-uploads/studies/"))
						.collect(Collectors.toMap(FileUpload::getFileUploadId, Function.identity()));

				List<ScreeningSession> screeningSessions = database.queryForList("""
						select ss.*
						from screening_session ss, account_check_in_action acia, account_check_in aci
						where ss.account_check_in_action_id=acia.account_check_in_action_id
						and acia.account_check_in_id=aci.account_check_in_id
						and aci.account_study_id=?
						order by ss.created
						""", ScreeningSession.class, accountStudy.getAccountStudyId());

				logger.debug("Username {} has {} screening session[s], {} check-in upload[s] and {} passive data upload[s]", username, screeningSessions.size(), accountCheckInActionFileUploadsById.size(), studyFileUploadsById.size());

				if (screeningSessions.size() == 0 && fileUploads.size() == 0) {
					logger.debug("No data to process for {}, moving on to next...", username);
					++i;
					continue;
				}

				DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss", Locale.US)
						.withZone(accountStudy.getTimeZone());

				Path usernameDirectory = Path.of(beiweDataDownloaderConfig.getDataDownloadDirectory(), username);

				try {
					if (!Files.isDirectory(usernameDirectory))
						Files.createDirectory(usernameDirectory);
				} catch (IOException e) {
					throw new UncheckedIOException(format("Cannot create username directory %s", usernameDirectory.toAbsolutePath()), e);
				}

				Path accountCheckInActionsDirectory = null;

				if (accountCheckInActionFileUploadsById.size() > 0) {
					accountCheckInActionsDirectory = usernameDirectory.resolve("active");

					try {
						if (!Files.isDirectory(accountCheckInActionsDirectory))
							Files.createDirectory(accountCheckInActionsDirectory);
					} catch (IOException e) {
						throw new UncheckedIOException(format("Cannot create account check-in actions directory %s", accountCheckInActionsDirectory.toAbsolutePath()), e);
					}
				}

				Path studiesDirectory = null;

				if (studyFileUploadsById.size() > 0) {
					studiesDirectory = usernameDirectory.resolve("passive");

					try {
						if (!Files.isDirectory(studiesDirectory))
							Files.createDirectory(studiesDirectory);
					} catch (IOException e) {
						throw new UncheckedIOException(format("Cannot create studies directory %s", studiesDirectory.toAbsolutePath()), e);
					}
				}

				Path assessmentsDirectory = null;

				if (screeningSessions.size() > 0) {
					assessmentsDirectory = usernameDirectory.resolve("assessments");

					try {
						if (!Files.isDirectory(assessmentsDirectory))
							Files.createDirectory(assessmentsDirectory);
					} catch (IOException e) {
						throw new UncheckedIOException(format("Cannot create assessments directory %s", assessmentsDirectory.toAbsolutePath()), e);
					}
				}

				if (downloadAssessments && screeningSessions.size() > 0) {
					for (ScreeningSession screeningSession : screeningSessions) {
						List<StudyScreeningAnswerRow> studyScreeningAnswerRows = database.queryForList("""
								select
									s.name,
									sq.question_text as question,
									sao.answer_option_text as answer,
									sa.created as answer_created_at
								from
									v_screening_answer sa,
									screening_answer_option sao,
									v_screening_session_answered_screening_question ssasq,
									v_screening_session_screening sss,
									screening_question sq,
									screening_version sv,
									screening s
								where
									sa.screening_session_answered_screening_question_id=ssasq.screening_session_answered_screening_question_id
									and sa.screening_answer_option_id=sao.screening_answer_option_id
									and ssasq.screening_session_screening_id=sss.screening_session_screening_id
									and ssasq.screening_question_id=sq.screening_question_id
									and sss.screening_version_id=sv.screening_version_id
									and sv.screening_id=s.screening_id
									and sss.screening_session_id=?
								order by sa.created
								""", StudyScreeningAnswerRow.class, screeningSession.getScreeningSessionId());

						Path csvFile = assessmentsDirectory.resolve(format("%s.csv", dateTimeFormatter.format(screeningSession.getCreated())));

						List<String> headerColumns = List.of(
								"Assessment",
								"Question",
								"Answer",
								"Answered At"
						);

						try (FileWriter fileWriter = new FileWriter(csvFile.toFile(), StandardCharsets.UTF_8);
								 CSVPrinter csvPrinter = new CSVPrinter(fileWriter, CSVFormat.DEFAULT.withHeader(headerColumns.toArray(new String[0])))) {

							for (StudyScreeningAnswerRow studyScreeningAnswerRow : studyScreeningAnswerRows) {
								List<String> recordElements = new ArrayList<>();
								recordElements.add(studyScreeningAnswerRow.getName());
								recordElements.add(studyScreeningAnswerRow.getQuestion());
								recordElements.add(studyScreeningAnswerRow.getAnswer());
								recordElements.add(dateTimeFormatter.format(studyScreeningAnswerRow.getAnswerCreatedAt()));

								csvPrinter.printRecord(recordElements.toArray(new Object[0]));
							}
						} catch (IOException e) {
							throw new UncheckedIOException(e);
						}
					}
				}

				// Ignore active and/or passive files if configured to do so
				List<FileUpload> fileUploadsToProcess = fileUploads.stream()
						.filter((fileUpload -> {
							boolean isActiveFileUpload = accountCheckInActionFileUploadsById.containsKey(fileUpload.getFileUploadId());
							boolean isPassiveFileUpload = studyFileUploadsById.containsKey(fileUpload.getFileUploadId());

							if (isActiveFileUpload && !downloadActive)
								return false;

							if (isPassiveFileUpload && !downloadPassive)
								return false;

							return true;
						}))
						.collect(Collectors.toList());

				ExecutorService executorService = Executors.newFixedThreadPool(6);
				List<CompletableFuture<Void>> completableFutures = new ArrayList<>(fileUploadsToProcess.size());
				AtomicInteger fileUploadProcessedCount = new AtomicInteger(0);

				for (FileUpload fileUpload : fileUploadsToProcess) {
					int currentIndex = fileUploadProcessedCount.incrementAndGet();

					logger.debug("Processing file upload {} of {} for {}...", currentIndex, fileUploadsToProcess.size(), username);

					boolean isActiveFileUpload = accountCheckInActionFileUploadsById.containsKey(fileUpload.getFileUploadId());
					boolean isPassiveFileUpload = studyFileUploadsById.containsKey(fileUpload.getFileUploadId());

					// Enable access in closure below
					Path pinnedAccountCheckInActionsDirectory = accountCheckInActionsDirectory;
					Path pinnedStudiesDirectory = studiesDirectory;

					completableFutures.add(CompletableFuture.supplyAsync(() -> {
						logger.debug("Fetching file from {}...", fileUpload.getStorageKey());

						GetObjectRequest objectRequest = GetObjectRequest.builder()
								.bucket(beiweDataDownloaderConfig.getAmazonS3BucketName())
								.key(fileUpload.getStorageKey())
								.build();

						try (ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(objectRequest)) {
							Path storageDirectory;

							if (isActiveFileUpload)
								storageDirectory = pinnedAccountCheckInActionsDirectory;
							else if (isPassiveFileUpload)
								storageDirectory = pinnedStudiesDirectory;
							else
								throw new IllegalStateException("Not sure what this upload is...");

							// Prepend timestamp to filename for easier sorting and in the event that devices choose non-unique filename
							Path encryptedFile = storageDirectory.resolve(format("%s-%s", dateTimeFormatter.format(fileUpload.getCreated()), fileUpload.getFilename()));

							try (OutputStream outputStream = new FileOutputStream(encryptedFile.toFile())) {
								IOUtils.copy(s3Object, outputStream);
							}

							logger.debug("File successfully fetched from {}.", fileUpload.getStorageKey());

							// Decrypt passive data
							if (studyFileUploadsById.containsKey(fileUpload.getFileUploadId())) {
								PrivateKey privateKey = privateKeysByEncryptionKeypairId.get(accountStudy.getEncryptionKeypairId());

								if (!fileUpload.getFilename().endsWith(".csv"))
									throw new IllegalStateException("Unexpected filename: does not end in .csv");

								String decryptedFilename = encryptedFile.getFileName().toString().replace(".csv", ".DECRYPTED.csv");
								Path decryptedFile = storageDirectory.resolve(decryptedFilename);

								try {
									beiweCryptoManager.decryptBeiweTextFile(encryptedFile, decryptedFile, privateKey);
									logger.debug("File successfully decrypted.");
									// Successful decryption: overwrite encrypted file
									decryptedFile.toFile().renameTo(encryptedFile.toFile());
								} catch (Exception e) {
									logger.error("An error occurred during decryption", e);
									beiweDownloadResult.decryptionFailures++;

									boolean deleteDecryptionFailures = true;

									if (deleteDecryptionFailures) {
										// Normal path for unsuccessful decryption: delete the junk encrypted file so it's not included in report data
										Files.deleteIfExists(encryptedFile);
										Files.deleteIfExists(decryptedFile);
									} else {
										// Alternative path for unsuccessful decryption: store off a .DECRYPTION_FAILED.csv version
										String decryptionFailedFilename = decryptedFile.toAbsolutePath().toString().replace(".DECRYPTED.csv", ".DECRYPTION-FAILED.csv");
										decryptedFile.toFile().renameTo(new File(decryptionFailedFilename));
									}
								}
							}
						} catch (NoSuchKeyException e) {
							logger.debug("File at {} does not exist.", fileUpload.getStorageKey());
							beiweDownloadResult.missingFiles++;
						} catch (IOException e) {
							logger.error("An error occurred during download", e);
							beiweDownloadResult.downloadErrors++;
						}

						logger.debug("Finished processing file upload {} of {} for username {}.", currentIndex, fileUploadsToProcess.size(), username);

						// Sleep a little so we don't overwhelm S3
						try {
							Thread.sleep(500L);
						} catch (InterruptedException ignored) {
							// Nothing to do
						}

						// Don't need to return a value for this future
						return null;
					}, executorService));
				}

				CompletableFuture<Void> combinedFuture = CompletableFuture.allOf(completableFutures.toArray(new CompletableFuture[0]));

				logger.debug("Waiting for all futures to complete for username {}...", username);

				try {
					combinedFuture.get(180, TimeUnit.MINUTES);
				} catch (ExecutionException e) {
					throw new RuntimeException("File download job failed", e);
				} catch (TimeoutException e) {
					throw new RuntimeException("File download job timed out", e);
				} catch (InterruptedException e) {
					throw new RuntimeException("File download job was interrupted", e);
				}

				logger.debug("All futures completed for username {}.", username);

				++i;
			}

			List<String> results = new ArrayList<>();

			for (Map.Entry<String, BeiweDownloadResult> entry : beiweDownloadResultsByUsername.entrySet()) {
				String username = entry.getKey();
				BeiweDownloadResult beiweDownloadResult = entry.getValue();

				List<String> resultComponents = new ArrayList<>();
				resultComponents.add(format("%s: %d files", username, beiweDownloadResult.totalFiles));

				if (beiweDownloadResult.missingFiles > 0)
					resultComponents.add(format("%d missing files", beiweDownloadResult.missingFiles));

				if (beiweDownloadResult.downloadErrors > 0)
					resultComponents.add(format("%d download errors", beiweDownloadResult.downloadErrors));

				if (beiweDownloadResult.decryptionFailures > 0)
					resultComponents.add(format("%d decryption failures", beiweDownloadResult.decryptionFailures));

				results.add(resultComponents.stream().collect(Collectors.joining(", ")));
			}

			logger.info("*** RESULTS ***\n{}", results.stream().collect(Collectors.joining("\n")));
		});
	}

	@NotThreadSafe
	public static class StudyScreeningAnswerRow {
		String name;
		String question;
		String answer;
		Instant answerCreatedAt;

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getQuestion() {
			return this.question;
		}

		public void setQuestion(String question) {
			this.question = question;
		}

		public String getAnswer() {
			return this.answer;
		}

		public void setAnswer(String answer) {
			this.answer = answer;
		}

		public Instant getAnswerCreatedAt() {
			return this.answerCreatedAt;
		}

		public void setAnswerCreatedAt(Instant answerCreatedAt) {
			this.answerCreatedAt = answerCreatedAt;
		}
	}

	@Nonnull
	protected BeiweDataDownloaderConfig loadBeiweDataDownloaderConfigFromFile(@Nonnull Path configFile) {
		requireNonNull(configFile);

		if (!Files.exists(configFile))
			throw new IllegalArgumentException(format("File at %s does not exist", configFile.toAbsolutePath()));

		try {
			String beiweDataDownloaderConfigJson = Files.readString(configFile, StandardCharsets.UTF_8);
			BeiweDataDownloaderConfig beiweDataDownloaderConfig = new Gson().fromJson(beiweDataDownloaderConfigJson, BeiweDataDownloaderConfig.class);

			if (!Files.isDirectory(Path.of(beiweDataDownloaderConfig.getDataDownloadDirectory())))
				throw new IllegalArgumentException(format("Download directory at %s does not exist, please create it.", beiweDataDownloaderConfig.getDataDownloadDirectory()));

			if (beiweDataDownloaderConfig.getRestrictToUsernames() == null)
				beiweDataDownloaderConfig.setRestrictToUsernames(List.of());

			return beiweDataDownloaderConfig;
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
	protected static class BeiweDownloadResult {
		int totalFiles;
		int missingFiles;
		int downloadErrors;
		int decryptionFailures;
	}

	@NotThreadSafe
	protected static class BeiweDataDownloaderConfig {
		@Nullable
		private String studyUrlName;
		@Nullable
		private List<String> restrictToUsernames;
		@Nullable
		private String dataDownloadDirectory;
		@Nullable
		private String amazonS3AccessKey;
		@Nullable
		private String amazonS3SecretKey;
		@Nullable
		private String amazonS3Region;
		@Nullable
		private String amazonS3BucketName;
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
		public List<String> getRestrictToUsernames() {
			return this.restrictToUsernames;
		}

		public void setRestrictToUsernames(@Nullable List<String> restrictToUsernames) {
			this.restrictToUsernames = restrictToUsernames;
		}

		@Nullable
		public String getDataDownloadDirectory() {
			return this.dataDownloadDirectory;
		}

		public void setDataDownloadDirectory(@Nullable String dataDownloadDirectory) {
			this.dataDownloadDirectory = dataDownloadDirectory;
		}

		@Nullable
		public String getAmazonS3AccessKey() {
			return this.amazonS3AccessKey;
		}

		public void setAmazonS3AccessKey(@Nullable String amazonS3AccessKey) {
			this.amazonS3AccessKey = amazonS3AccessKey;
		}

		@Nullable
		public String getAmazonS3SecretKey() {
			return this.amazonS3SecretKey;
		}

		public void setAmazonS3SecretKey(@Nullable String amazonS3SecretKey) {
			this.amazonS3SecretKey = amazonS3SecretKey;
		}

		@Nullable
		public String getAmazonS3Region() {
			return this.amazonS3Region;
		}

		public void setAmazonS3Region(@Nullable String amazonS3Region) {
			this.amazonS3Region = amazonS3Region;
		}

		@Nullable
		public String getAmazonS3BucketName() {
			return this.amazonS3BucketName;
		}

		public void setAmazonS3BucketName(@Nullable String amazonS3BucketName) {
			this.amazonS3BucketName = amazonS3BucketName;
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
