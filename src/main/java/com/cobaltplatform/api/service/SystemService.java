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

package com.cobaltplatform.api.service;

import com.cobaltplatform.api.Configuration;
import com.cobaltplatform.api.integration.acuity.AcuitySyncManager;
import com.cobaltplatform.api.integration.common.ProviderAvailabilitySyncManager;
import com.cobaltplatform.api.integration.enterprise.EnterprisePlugin;
import com.cobaltplatform.api.integration.enterprise.EnterprisePluginProvider;
import com.cobaltplatform.api.integration.epic.EpicFhirSyncManager;
import com.cobaltplatform.api.integration.epic.EpicSyncManager;
import com.cobaltplatform.api.integration.microsoft.MicrosoftClient;
import com.cobaltplatform.api.integration.microsoft.model.OnlineMeeting;
import com.cobaltplatform.api.integration.microsoft.request.OnlineMeetingCreateRequest;
import com.cobaltplatform.api.messaging.email.EmailMessage;
import com.cobaltplatform.api.messaging.email.EmailMessageTemplate;
import com.cobaltplatform.api.model.api.request.CreateFileUploadRequest;
import com.cobaltplatform.api.model.api.request.CreateMarketingSiteOutreachRequest;
import com.cobaltplatform.api.model.api.request.CreateMicrosoftTeamsMeetingRequest;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.BetaFeature;
import com.cobaltplatform.api.model.db.EncryptionKeypair;
import com.cobaltplatform.api.model.db.FileUpload;
import com.cobaltplatform.api.model.db.FileUploadType.FileUploadTypeId;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.MicrosoftTeamsMeeting;
import com.cobaltplatform.api.model.db.PrivateKeyFormat.PrivateKeyFormatId;
import com.cobaltplatform.api.model.db.Provider;
import com.cobaltplatform.api.model.db.PublicKeyFormat.PublicKeyFormatId;
import com.cobaltplatform.api.model.db.SchedulingSystem.SchedulingSystemId;
import com.cobaltplatform.api.model.service.AdvisoryLock;
import com.cobaltplatform.api.model.service.FileUploadResult;
import com.cobaltplatform.api.model.service.PresignedUpload;
import com.cobaltplatform.api.util.CryptoUtility;
import com.cobaltplatform.api.util.UploadManager;
import com.cobaltplatform.api.util.ValidationException;
import com.cobaltplatform.api.util.ValidationException.FieldError;
import com.cobaltplatform.api.util.ValidationUtility;
import com.cobaltplatform.api.util.db.DatabaseProvider;
import com.lokalized.Strings;
import com.pyranid.Database;
import com.soklet.web.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.KeyPair;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@Singleton
@ThreadSafe
public class SystemService {
	@Nonnull
	private final DatabaseProvider databaseProvider;
	@Nonnull
	private final javax.inject.Provider<ProviderService> providerServiceProvider;
	@Nonnull
	private final javax.inject.Provider<MessageService> messageServiceProvider;
	@Nonnull
	private final javax.inject.Provider<AccountService> accountServiceProvider;
	@Nonnull
	private final EpicSyncManager epicSyncManager;
	@Nonnull
	private final EpicFhirSyncManager epicFhirSyncManager;
	@Nonnull
	private final AcuitySyncManager acuitySyncManager;
	@Nonnull
	private final UploadManager uploadManager;
	@Nonnull
	private final EnterprisePluginProvider enterprisePluginProvider;
	@Nonnull
	private final Configuration configuration;
	@Nonnull
	private final Strings strings;
	@Nonnull
	private final Logger logger;

	@Inject
	public SystemService(@Nonnull DatabaseProvider databaseProvider,
											 @Nonnull javax.inject.Provider<ProviderService> providerServiceProvider,
											 @Nonnull javax.inject.Provider<MessageService> messageServiceProvider,
											 @Nonnull javax.inject.Provider<AccountService> accountServiceProvider,
											 @Nonnull EpicSyncManager epicSyncManager,
											 @Nonnull EpicFhirSyncManager epicFhirSyncManager,
											 @Nonnull AcuitySyncManager acuitySyncManager,
											 @Nonnull UploadManager uploadManager,
											 @Nonnull EnterprisePluginProvider enterprisePluginProvider,
											 @Nonnull Configuration configuration,
											 @Nonnull Strings strings) {
		requireNonNull(databaseProvider);
		requireNonNull(providerServiceProvider);
		requireNonNull(messageServiceProvider);
		requireNonNull(accountServiceProvider);
		requireNonNull(epicSyncManager);
		requireNonNull(epicFhirSyncManager);
		requireNonNull(acuitySyncManager);
		requireNonNull(uploadManager);
		requireNonNull(enterprisePluginProvider);
		requireNonNull(configuration);
		requireNonNull(strings);

		this.databaseProvider = databaseProvider;
		this.providerServiceProvider = providerServiceProvider;
		this.messageServiceProvider = messageServiceProvider;
		this.accountServiceProvider = accountServiceProvider;
		this.epicSyncManager = epicSyncManager;
		this.epicFhirSyncManager = epicFhirSyncManager;
		this.acuitySyncManager = acuitySyncManager;
		this.uploadManager = uploadManager;
		this.enterprisePluginProvider = enterprisePluginProvider;
		this.configuration = configuration;
		this.strings = strings;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	public List<BetaFeature> findBetaFeatures() {
		return getDatabase().queryForList("SELECT * FROM beta_feature ORDER BY description", BetaFeature.class);
	}

	@Nonnull
	public Boolean performAdvisoryLockOperationIfAvailable(@Nonnull AdvisoryLock advisoryLock,
																												 @Nonnull Runnable runnable) {
		requireNonNull(advisoryLock);
		requireNonNull(runnable);

		getLogger().trace("Attempting to acquire advisory lock {} (key {})",
				advisoryLock.name(), advisoryLock.getKey());

		Boolean lockAcquired = getDatabase().queryForObject("SELECT pg_try_advisory_lock(?)",
				Boolean.class, advisoryLock.getKey()).get();

		if (!lockAcquired) {
			getLogger().trace("Advisory lock {} (key {}) has already been acquired, not performing operation.",
					advisoryLock.name(), advisoryLock.getKey());
			return false;
		}

		try {
			runnable.run();
		} finally {
			getLogger().trace("Releasing advisory lock {} (key {})...", advisoryLock.name(), advisoryLock.getKey());
			getDatabase().queryForObject("SELECT pg_advisory_unlock(?)", Boolean.class, advisoryLock.getKey());
			getLogger().trace("Advisory lock {} (key {}) has been released.", advisoryLock.name(), advisoryLock.getKey());
		}

		return true;
	}

	public void syncPastProviderAvailability(@Nonnull InstitutionId institutionId) {
		requireNonNull(institutionId);
		syncPastProviderAvailability(institutionId, null);
	}

	public void syncPastProviderAvailability(@Nonnull InstitutionId institutionId,
																					 @Nullable LocalDate startingAtDate) {
		requireNonNull(institutionId);

		List<Provider> providers = getProviderService().findProvidersByInstitutionId(institutionId).stream()
				.filter(provider -> provider.getSchedulingSystemId() == SchedulingSystemId.EPIC || provider.getSchedulingSystemId() == SchedulingSystemId.ACUITY)
				.sorted(Comparator.comparing(Provider::getName))
				.collect(Collectors.toList());

		int i = 1;

		getLogger().info("*** STARTING PAST PROVIDER AVAILABILITY SYNC FOR {} ***", institutionId.name());

		for (Provider provider : providers) {
			List<ProviderSyncRecord> providerSyncRecords = new ArrayList<>(providers.size() * 365 * 2);

			// Sync date starts on the date at which the provider record was created and goes until yesterday (inclusive).
			// If a starting-at date was passed in, use that instead
			LocalDate syncDate = startingAtDate != null ? startingAtDate : LocalDate.ofInstant(provider.getCreated(), provider.getTimeZone());
			LocalDate today = LocalDate.now(provider.getTimeZone());

			ProviderAvailabilitySyncManager providerAvailabilitySyncManager = null;

			if (provider.getSchedulingSystemId() == SchedulingSystemId.EPIC)
				providerAvailabilitySyncManager = getEpicSyncManager();
			else if (provider.getSchedulingSystemId() == SchedulingSystemId.EPIC_FHIR)
				providerAvailabilitySyncManager = getEpicFhirSyncManager();
			else if (provider.getSchedulingSystemId() == SchedulingSystemId.ACUITY)
				providerAvailabilitySyncManager = getAcuitySyncManager();

			while (syncDate.isBefore(today)) {
				getLogger().info("Syncing provider {} ({}/{}) with {} on {}...", provider.getName(), i, providers.size(), provider.getSchedulingSystemId().name(), syncDate);

				try {
					providerAvailabilitySyncManager.syncProviderAvailability(provider.getProviderId(), syncDate, true);
					providerSyncRecords.add(new ProviderSyncRecord(provider, syncDate));
				} catch (Exception e) {
					getLogger().warn(format("Error performing old availabilty sync for %s on %s using %s...", provider.getName(), syncDate, provider.getSchedulingSystemId().name()), e);
					providerSyncRecords.add(new ProviderSyncRecord(provider, syncDate, e));
				}

				syncDate = syncDate.plusDays(1);
			}

			for (ProviderSyncRecord providerSyncRecord : providerSyncRecords) {
				getDatabase().transaction(() -> {
					getDatabase().execute(
							"INSERT INTO provider_old_availability_sync_log(provider_id, date, success, sync_timestamp) VALUES (?,?,?,?)",
							providerSyncRecord.getProvider().getProviderId(), providerSyncRecord.getDate(), providerSyncRecord.getSuccess(), providerSyncRecord.getCreated());
				});
			}

			++i;
		}

		getLogger().info("*** ENDING PAST PROVIDER AVAILABILITY SYNC FOR {} ***", institutionId.name());
	}

	public void syncProviderAvailability(@Nonnull UUID providerId,
																			 @Nullable LocalDate startingAtDate,
																			 @Nullable LocalDate endingAtDate) {
		requireNonNull(providerId);

		Provider provider = getProviderService().findProviderById(providerId).orElse(null);

		if (provider == null)
			throw new NotFoundException();

		if (startingAtDate == null)
			startingAtDate = LocalDate.ofInstant(provider.getCreated(), provider.getTimeZone());

		if (endingAtDate == null)
			endingAtDate = LocalDate.ofInstant(Instant.now(), provider.getTimeZone());

		if (startingAtDate.isAfter(endingAtDate))
			throw new IllegalStateException(format("Starting date %s is after ending date %s", startingAtDate, endingAtDate));

		getLogger().info("Syncing provider availability for {} for {} - {}", provider.getName(), startingAtDate, endingAtDate);

		LocalDate syncDate = startingAtDate;

		ProviderAvailabilitySyncManager providerAvailabilitySyncManager = null;

		if (provider.getSchedulingSystemId() == SchedulingSystemId.EPIC)
			providerAvailabilitySyncManager = getEpicSyncManager();
		else if (provider.getSchedulingSystemId() == SchedulingSystemId.EPIC_FHIR)
			providerAvailabilitySyncManager = getEpicFhirSyncManager();
		else if (provider.getSchedulingSystemId() == SchedulingSystemId.ACUITY)
			providerAvailabilitySyncManager = getAcuitySyncManager();

		if (providerAvailabilitySyncManager == null) {
			getLogger().info("Provider {} uses {} for calendaring, no need to sync.", provider.getName(), provider.getSchedulingSystemId().name());
			return;
		}

		boolean success = false;

		while (!syncDate.isAfter(endingAtDate)) {
			getLogger().info("Syncing provider {} on {}...", provider.getName(), syncDate);

			try {
				providerAvailabilitySyncManager.syncProviderAvailability(providerId, syncDate, true);
				success = true;
			} catch (Exception e) {
				getLogger().warn(format("Unable to sync provider %s on %s", provider.getName(), syncDate), e);
			} finally {
				LocalDate pinnedSyncDate = syncDate;
				boolean pinnedSuccess = success;

				getDatabase().transaction(() -> {
					getDatabase().execute(
							"INSERT INTO provider_old_availability_sync_log(provider_id, date, success, sync_timestamp) VALUES (?,?,?,?)",
							providerId, pinnedSyncDate, pinnedSuccess, Instant.now());
				});
			}

			getLogger().info("Syncing provider {} on {} finished.", provider.getName(), syncDate);

			syncDate = syncDate.plusDays(1);
		}

		getLogger().info("Finished syncing provider availability for {} for {} - {}", provider.getName(), startingAtDate, endingAtDate);
	}

	public void createMarketingSiteOutreach(@Nonnull CreateMarketingSiteOutreachRequest request) {
		requireNonNull(request);

		String firstName = trimToNull(request.getFirstName());
		String lastName = trimToNull(request.getLastName());
		String emailAddress = trimToNull(request.getEmailAddress());
		String jobTitle = trimToNull(request.getJobTitle());
		String message = trimToNull(request.getMessage());
		ValidationException validationException = new ValidationException();

		if (firstName == null)
			validationException.add(new FieldError("firstName", strings.get("First name is required.")));

		if (lastName == null)
			validationException.add(new FieldError("lastName", strings.get("Last name is required.")));

		if (emailAddress == null)
			validationException.add(new FieldError("emailAddress", strings.get("Email address is required.")));
		else if (!ValidationUtility.isValidEmailAddress(emailAddress))
			validationException.add(new FieldError("emailAddress", strings.get("Email address is invalid.")));

		if (jobTitle == null)
			validationException.add(new FieldError("jobTitle", strings.get("Job title is required.")));

		if (validationException.hasErrors())
			throw validationException;

		// It's OK to hardcode institution, email, and locale because this is a specially targeted email
		EmailMessage emailMessage = new EmailMessage.Builder(InstitutionId.COBALT, EmailMessageTemplate.MARKETING_SITE_OUTREACH, Locale.US)
				.toAddresses(List.of("hello@cobaltinnovations.org"))
				.messageContext(new HashMap<String, Object>() {{
					put("firstName", firstName);
					put("lastName", lastName);
					put("emailAddress", emailAddress);
					put("jobTitle", jobTitle);
					put("message", message == null ? getStrings().get("(no message)") : message);
				}})
				.build();

		getMessageService().enqueueMessage(emailMessage);
	}

	@Nonnull
	public UUID createEncryptionKeypair() {
		return createEncryptionKeypair("RSA", 4096);
	}

	@Nonnull
	public UUID createEncryptionKeypair(@Nonnull String algorithm,
																			@Nonnull Integer keySize) {
		requireNonNull(algorithm);
		requireNonNull(keySize);

		// Create the keypair and turn it into String representations for persistence
		KeyPair keyPair = CryptoUtility.generateKeyPair(algorithm, keySize);

		Base64.Encoder encoder = Base64.getEncoder();
		String publicKeyAsString = encoder.encodeToString(keyPair.getPublic().getEncoded());
		String privateKeyAsString = encoder.encodeToString(keyPair.getPrivate().getEncoded());
		PublicKeyFormatId publicKeyFormatId = PublicKeyFormatId.fromPublicKey(keyPair.getPublic()).orElseThrow();
		PrivateKeyFormatId privateKeyFormatId = PrivateKeyFormatId.fromPrivateKey(keyPair.getPrivate()).orElseThrow();

		UUID encryptionKeypairId = UUID.randomUUID();

		getDatabase().execute("""
				INSERT INTO encryption_keypair (
				  encryption_keypair_id,
				  public_key,
				  private_key,
				  public_key_format_id,
				  private_key_format_id,
				  key_size
				) VALUES (?,?,?,?,?,?)
				""", encryptionKeypairId, publicKeyAsString, privateKeyAsString, publicKeyFormatId, privateKeyFormatId, keySize);

		return encryptionKeypairId;
	}

	@Nonnull
	public Optional<EncryptionKeypair> findEncryptionKeypairById(@Nullable UUID encryptionKeypairId) {
		if (encryptionKeypairId == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				SELECT *
				FROM encryption_keypair
				WHERE encryption_keypair_id=?
				""", EncryptionKeypair.class, encryptionKeypairId);
	}

	@Nonnull
	public FileUploadResult createFileUpload(@Nonnull CreateFileUploadRequest request) {
		requireNonNull(request);

		UUID accountId = request.getAccountId();
		FileUploadTypeId fileUploadTypeId = request.getFileUploadTypeId();
		String storageKeyPrefix = trimToNull(request.getStorageKeyPrefix());
		String filename = trimToNull(request.getFilename());
		String contentType = trimToNull(request.getContentType());
		Boolean publicRead = request.getPublicRead() == null ? false : request.getPublicRead();
		Map<String, String> metadata = request.getMetadata() == null ? Map.of() : request.getMetadata();
		UUID fileUploadId = UUID.randomUUID();
		Account account = null;
		Number filesize = request.getFilesize();

		ValidationException validationException = new ValidationException();

		if (accountId == null) {
			validationException.add(new FieldError("accountId", getStrings().get("Account ID is required.")));
		} else {
			account = getAccountService().findAccountById(accountId).orElse(null);

			if (account == null)
				validationException.add(new FieldError("accountId", getStrings().get("Account ID is invalid.")));
		}

		if (fileUploadTypeId == null)
			validationException.add(new FieldError("fileUploadTypeId", getStrings().get("File Upload Type ID is required.")));

		if (storageKeyPrefix == null) {
			validationException.add(new FieldError("storageKeyPrefix", getStrings().get("Storage key prefix is required.")));
		} else if (storageKeyPrefix.startsWith("/") || storageKeyPrefix.endsWith("/")) {
			validationException.add(new FieldError("storageKeyPrefix", getStrings().get("Storage key prefix cannot begin or end with a '/' character.")));
		} else if (Pattern.compile("//+").matcher(storageKeyPrefix).find() /* don't care about precompiling pattern here b/c it's short and not a hot codepath */) {
			validationException.add(new FieldError("storageKeyPrefix", getStrings().get("Storage key prefix cannot have multiple '/' characters in a row.")));
		} else {
			boolean valid = true;

			// Only allow letters and numbers and hyphens and forward slashes
			for (int i = 0; i < storageKeyPrefix.length(); ++i) {
				char character = storageKeyPrefix.charAt(i);

				if (!(Character.isLetterOrDigit(character) || character == '-' || character == '/')) {
					valid = false;
					break;
				}
			}

			if (!valid)
				validationException.add(new FieldError("storageKeyPrefix", getStrings().get("Storage key prefix can only contain letters, numbers, forward slashes, and hyphens.")));
		}

		if (filename == null)
			validationException.add(new FieldError("filename", getStrings().get("Filename is required.")));

		if (contentType == null)
			validationException.add(new FieldError("contentType", getStrings().get("Content type is required.")));

		if (validationException.hasErrors())
			throw validationException;

		String storageKey = format("file-uploads/%s/%s/%s", storageKeyPrefix, fileUploadId, filename);

		// Some institutions might have a special prefix.  If so, include it.
		EnterprisePlugin enterprisePlugin = getEnterprisePluginProvider().enterprisePluginForInstitutionId(account.getInstitutionId());
		String fileUploadStorageKeyPrefix = enterprisePlugin.fileUploadStorageKeyPrefix().orElse(null);

		if (fileUploadStorageKeyPrefix != null)
			storageKey = format("%s/%s", fileUploadStorageKeyPrefix, storageKey);

		contentType = contentType.toLowerCase(Locale.ENGLISH);

		PresignedUpload presignedUpload = getUploadManager().createPresignedUpload(storageKey, contentType, publicRead, metadata);

		getDatabase().execute("""
				INSERT INTO file_upload (
				  file_upload_id,
				  file_upload_type_id,
				  account_id,
				  url,
				  storage_key,
				  filename,
				  content_type,
				  filesize
				) VALUES (?,?,?,?,?,?,?,?)
				""", fileUploadId, fileUploadTypeId, accountId, presignedUpload.getAccessUrl(), storageKey, filename, contentType, filesize);

		return new FileUploadResult(fileUploadId, presignedUpload);
	}

	@Nonnull
	public Optional<FileUpload> findFileUploadById(@Nullable UUID fileUploadId) {
		if (fileUploadId == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				SELECT *
				FROM file_upload
				WHERE file_upload_id=?
				""", FileUpload.class, fileUploadId);
	}

	public void downloadFileUpload(@Nonnull UUID fileUploadId,
																 @Nonnull BufferedOutputStream bufferedOutputStream) {
		requireNonNull(fileUploadId);
		requireNonNull(bufferedOutputStream);

		FileUpload fileUpload = findFileUploadById(fileUploadId).orElse(null);

		if (fileUpload == null)
			throw new ValidationException(getStrings().get("File Upload ID is invalid."));

		getUploadManager().downloadFileLocatedByStorageKey(fileUpload.getStorageKey(), bufferedOutputStream);
	}

	// Convenience method for the above if we need to download the file in-memory
	@Nonnull
	public byte[] downloadFileUploadToByteArray(@Nonnull UUID fileUploadId) {
		try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
				 BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(byteArrayOutputStream)) {
			downloadFileUpload(fileUploadId, bufferedOutputStream);
			bufferedOutputStream.flush();
			return byteArrayOutputStream.toByteArray();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Nonnull
	public UUID createMicrosoftTeamsMeeting(@Nonnull CreateMicrosoftTeamsMeetingRequest request) {
		requireNonNull(request);

		InstitutionId institutionId = request.getInstitutionId();
		UUID createdByAccountId = request.getCreatedByAccountId();
		OnlineMeetingCreateRequest onlineMeetingCreateRequest = request.getOnlineMeetingCreateRequest();
		UUID microsoftTeamsMeetingId = UUID.randomUUID();
		LocalDateTime startDateTime = null;
		LocalDateTime endDateTime = null;
		ZoneId timeZone = null;
		ValidationException validationException = new ValidationException();

		if (institutionId == null)
			validationException.add(new FieldError("institutionId", getStrings().get("Institution ID is required.")));

		if (createdByAccountId == null)
			validationException.add(new FieldError("createdByAccountId", getStrings().get("Created-by Account ID is required.")));

		if (onlineMeetingCreateRequest == null) {
			validationException.add(new FieldError("onlineMeetingCreateRequest", getStrings().get("Teams meeting information is required.")));
		} else {
			ZonedDateTime zonedStartDateTime = onlineMeetingCreateRequest.getStartDateTime();
			ZonedDateTime zonedEndDateTime = onlineMeetingCreateRequest.getEndDateTime();

			if (zonedStartDateTime == null) {
				validationException.add(new FieldError("onlineMeetingCreateRequest.zonedStartDateTime", getStrings().get("Start date and time is required.")));
			} else {
				startDateTime = zonedStartDateTime.toLocalDateTime();
				timeZone = zonedStartDateTime.getZone();
			}

			if (zonedEndDateTime == null) {
				validationException.add(new FieldError("onlineMeetingCreateRequest.zonedEndDateTime", getStrings().get("End date and time is required.")));
			} else {
				endDateTime = zonedEndDateTime.toLocalDateTime();

				if (!Objects.equals(zonedStartDateTime.getZone(), zonedEndDateTime.getZone()))
					validationException.add(new FieldError("onlineMeetingCreateRequest", getStrings().get("Start and end timezones don't match.")));
			}
		}

		if (validationException.hasErrors())
			throw validationException;

		EnterprisePlugin enterprisePlugin = getEnterprisePluginProvider().enterprisePluginForInstitutionId(institutionId);
		MicrosoftClient microsoftClient = enterprisePlugin.microsoftTeamsClientForDaemon().get();
		OnlineMeeting onlineMeeting = microsoftClient.createOnlineMeeting(onlineMeetingCreateRequest);

		getDatabase().execute("""
						INSERT INTO microsoft_teams_meeting (
							microsoft_teams_meeting_id,
							institution_id,
							created_by_account_id,
							online_meeting_id,
							join_url,
							start_date_time,
							end_date_time,
							time_zone,
							api_response
						) VALUES (?,?,?,?,?,?,?,?,CAST(? AS JSONB))
						""", microsoftTeamsMeetingId, institutionId, createdByAccountId, onlineMeeting.getId(), onlineMeeting.getJoinUrl(),
				startDateTime, endDateTime, timeZone, onlineMeeting.getRawJson());

		return microsoftTeamsMeetingId;
	}

	@Nonnull
	public Optional<MicrosoftTeamsMeeting> findMicrosoftTeamsMeetingById(@Nullable UUID microsoftTeamsMeetingId) {
		if (microsoftTeamsMeetingId == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				SELECT *
				FROM microsoft_teams_meeting
				WHERE microsoft_teams_meeting_id=?
				""", MicrosoftTeamsMeeting.class, microsoftTeamsMeetingId);
	}

	@ThreadSafe
	public static class ProviderSyncRecord {
		@Nonnull
		private Provider provider;
		@Nonnull
		private LocalDate date;
		@Nonnull
		private Boolean success;
		@Nullable
		private Exception exception;
		@Nullable
		private Instant created;

		public ProviderSyncRecord(@Nonnull Provider provider,
															@Nonnull LocalDate date) {
			this(provider, date, null);
		}

		public ProviderSyncRecord(@Nonnull Provider provider,
															@Nonnull LocalDate date,
															@Nullable Exception exception) {
			requireNonNull(provider);
			requireNonNull(date);

			this.provider = provider;
			this.date = date;
			this.success = exception == null;
			this.exception = exception;
			this.created = Instant.now();
		}

		@Override
		public String toString() {
			return getSuccess() ? format("%s %s: SUCCESS", getProvider().getName(), getDate()) :
					format("%s %s: ERROR! Reason: %s", getProvider().getName(), getDate(), getException().get().getMessage());
		}

		@Nonnull
		public Provider getProvider() {
			return this.provider;
		}

		@Nonnull
		public LocalDate getDate() {
			return this.date;
		}

		@Nonnull
		public Boolean getSuccess() {
			return this.success;
		}

		@Nonnull
		public Optional<Exception> getException() {
			return Optional.ofNullable(this.exception);
		}

		@Nullable
		public Instant getCreated() {
			return this.created;
		}
	}

	@Nonnull
	protected Database getDatabase() {
		return this.databaseProvider.get();
	}

	@Nonnull
	protected ProviderService getProviderService() {
		return this.providerServiceProvider.get();
	}

	@Nonnull
	protected MessageService getMessageService() {
		return this.messageServiceProvider.get();
	}

	@Nonnull
	protected AccountService getAccountService() {
		return this.accountServiceProvider.get();
	}

	@Nonnull
	protected EpicSyncManager getEpicSyncManager() {
		return this.epicSyncManager;
	}

	@Nonnull
	protected EpicFhirSyncManager getEpicFhirSyncManager() {
		return this.epicFhirSyncManager;
	}

	@Nonnull
	protected AcuitySyncManager getAcuitySyncManager() {
		return this.acuitySyncManager;
	}

	@Nonnull
	protected UploadManager getUploadManager() {
		return this.uploadManager;
	}

	@Nonnull
	protected EnterprisePluginProvider getEnterprisePluginProvider() {
		return this.enterprisePluginProvider;
	}

	@Nonnull
	protected Configuration getConfiguration() {
		return this.configuration;
	}

	@Nonnull
	protected Strings getStrings() {
		return this.strings;
	}

	@Nonnull
	protected Logger getLogger() {
		return this.logger;
	}
}
