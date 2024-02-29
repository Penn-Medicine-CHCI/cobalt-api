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

import com.cobaltplatform.api.integration.enterprise.EnterprisePlugin;
import com.cobaltplatform.api.integration.enterprise.EnterprisePluginProvider;
import com.cobaltplatform.api.messaging.MessageSender;
import com.cobaltplatform.api.messaging.push.PushMessage;
import com.cobaltplatform.api.messaging.push.PushMessageTemplate;
import com.cobaltplatform.api.model.api.request.CreateClientDeviceActivityRequest;
import com.cobaltplatform.api.model.api.request.TestClientDevicePushMessageRequest;
import com.cobaltplatform.api.model.api.request.UpsertClientDevicePushTokenRequest;
import com.cobaltplatform.api.model.api.request.UpsertClientDeviceRequest;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.ClientDevice;
import com.cobaltplatform.api.model.db.ClientDeviceActivity;
import com.cobaltplatform.api.model.db.ClientDeviceActivityType.ClientDeviceActivityTypeId;
import com.cobaltplatform.api.model.db.ClientDevicePushToken;
import com.cobaltplatform.api.model.db.ClientDevicePushTokenType.ClientDevicePushTokenTypeId;
import com.cobaltplatform.api.model.db.ClientDeviceType.ClientDeviceTypeId;
import com.cobaltplatform.api.util.ValidationException;
import com.cobaltplatform.api.util.ValidationException.FieldError;
import com.cobaltplatform.api.util.db.DatabaseProvider;
import com.lokalized.Strings;
import com.pyranid.Database;
import com.pyranid.DatabaseException;
import com.pyranid.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.sql.Savepoint;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@Singleton
@ThreadSafe
public class ClientDeviceService {
	@Nonnull
	private final Provider<MessageService> messageServiceProvider;
	@Nonnull
	private final Provider<AccountService> accountServiceProvider;
	@Nonnull
	private final EnterprisePluginProvider enterprisePluginProvider;
	@Nonnull
	private final DatabaseProvider databaseProvider;
	@Nonnull
	private final Strings strings;
	@Nonnull
	private final Logger logger;

	@Inject
	public ClientDeviceService(@Nonnull Provider<MessageService> messageServiceProvider,
														 @Nonnull Provider<AccountService> accountServiceProvider,
														 @Nonnull EnterprisePluginProvider enterprisePluginProvider,
														 @Nonnull DatabaseProvider databaseProvider,
														 @Nonnull Strings strings) {
		requireNonNull(messageServiceProvider);
		requireNonNull(accountServiceProvider);
		requireNonNull(enterprisePluginProvider);
		requireNonNull(databaseProvider);
		requireNonNull(strings);

		this.messageServiceProvider = messageServiceProvider;
		this.accountServiceProvider = accountServiceProvider;
		this.enterprisePluginProvider = enterprisePluginProvider;
		this.databaseProvider = databaseProvider;
		this.strings = strings;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	public Optional<ClientDevice> findClientDeviceById(@Nullable UUID clientDeviceId) {
		if (clientDeviceId == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				SELECT *
				FROM client_device
				WHERE client_device_id=?
				""", ClientDevice.class, clientDeviceId);
	}

	@Nonnull
	public Optional<ClientDevice> findClientDeviceByFingerprint(@Nullable String fingerprint) {
		fingerprint = trimToNull(fingerprint);

		if (fingerprint == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				SELECT *
				FROM client_device
				WHERE fingerprint=?
				""", ClientDevice.class, fingerprint);
	}

	@Nonnull
	public Optional<ClientDevicePushToken> findClientDevicePushTokenById(@Nullable UUID clientDevicePushTokenId) {
		if (clientDevicePushTokenId == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				SELECT *
				FROM client_device_push_token
				WHERE client_device_push_token_id=?
				""", ClientDevicePushToken.class, clientDevicePushTokenId);
	}

	@Nonnull
	public Optional<ClientDevicePushToken> findClientDevicePushTokenByClientDeviceAndPushToken(@Nullable UUID clientDeviceId,
																																														 @Nullable ClientDevicePushTokenTypeId clientDevicePushTokenTypeId,
																																														 @Nullable String pushToken) {
		pushToken = trimToNull(pushToken);

		if (clientDeviceId == null || clientDevicePushTokenTypeId == null || pushToken == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				SELECT *
				FROM client_device_push_token
				WHERE client_device_id=?
				AND client_device_push_token_type_id=?
				AND push_token=?
				""", ClientDevicePushToken.class, clientDeviceId, clientDevicePushTokenTypeId, pushToken);
	}

	@Nonnull
	public Boolean isAccountAssociatedWithClientDeviceId(@Nullable UUID accountId,
																											 @Nullable UUID clientDeviceId) {
		if (accountId == null || clientDeviceId == null)
			return false;

		return getDatabase().queryForObject("""
				    SELECT COUNT(*) > 0
				    FROM account_client_device
				    WHERE account_id=?
				    AND client_device_id=?
				""", Boolean.class, accountId, clientDeviceId).get();
	}

	@Nonnull
	public UUID upsertClientDevice(@Nonnull UpsertClientDeviceRequest request) {
		requireNonNull(request);

		UUID accountId = request.getAccountId();
		ClientDeviceTypeId clientDeviceTypeId = request.getClientDeviceTypeId();
		String fingerprint = trimToNull(request.getFingerprint());
		String model = trimToNull(request.getModel());
		String brand = trimToNull(request.getBrand());
		String operatingSystemName = trimToNull(request.getOperatingSystemName());
		String operatingSystemVersion = trimToNull(request.getOperatingSystemVersion());

		ValidationException validationException = new ValidationException();

		if (clientDeviceTypeId == null)
			validationException.add(new FieldError("clientDeviceTypeId", getStrings().get("Client Device Type ID is required.")));

		if (fingerprint == null)
			validationException.add(new FieldError("fingerprint", getStrings().get("Fingerprint is required.")));

		if (validationException.hasErrors())
			throw validationException;

		UUID clientDeviceId = getDatabase().executeReturning("""					
				INSERT INTO client_device (
				  client_device_type_id,
				  fingerprint,
				  model,
				  brand,
				  operating_system_name,
				  operating_system_version
				)
				VALUES (?,?,?,?,?,?)
				ON CONFLICT ON CONSTRAINT client_device_unique_idx
				DO UPDATE SET
				  operating_system_name=EXCLUDED.operating_system_name,
				  operating_system_version=EXCLUDED.operating_system_version
				RETURNING client_device_id
				""", UUID.class, clientDeviceTypeId, fingerprint, model, brand, operatingSystemName, operatingSystemVersion).get();

		if (accountId != null) {
			Transaction transaction = getDatabase().currentTransaction().get();
			Savepoint savepoint = transaction.createSavepoint();

			try {
				getDatabase().execute("""
						    INSERT INTO account_client_device (
						      client_device_id,
						      account_id
						    ) VALUES (?,?)
						""", clientDeviceId, accountId);
			} catch (DatabaseException e) {
				if ("account_client_device_unique_idx".equals(e.constraint().orElse(null))) {
					getLogger().trace("Client device already associated with account, don't need to re-associate.");
					transaction.rollback(savepoint);
				} else {
					throw e;
				}
			}
		}

		return clientDeviceId;
	}

	@Nonnull
	public UUID upsertClientDevicePushToken(@Nonnull UpsertClientDevicePushTokenRequest request) {
		requireNonNull(request);

		UUID clientDeviceId = request.getClientDeviceId();
		String fingerprint = trimToNull(request.getFingerprint());
		String pushToken = trimToNull(request.getPushToken());
		ClientDevicePushTokenTypeId clientDevicePushTokenTypeId = request.getClientDevicePushTokenTypeId();
		ClientDevice clientDevice = null;

		ValidationException validationException = new ValidationException();

		if (clientDeviceId != null) {
			clientDevice = findClientDeviceById(clientDeviceId).orElse(null);

			if (clientDevice == null)
				validationException.add(new FieldError("clientDeviceId", getStrings().get("Client Device ID is invalid.")));
		} else if (fingerprint != null) {
			clientDevice = findClientDeviceByFingerprint(fingerprint).orElse(null);

			if (clientDevice == null)
				validationException.add(new FieldError("fingerprint", getStrings().get("Client Device Fingerprint is invalid.")));
		} else {
			validationException.add(getStrings().get("Either a Client Device ID or Client Device Fingerprint is required."));
		}

		if (pushToken == null)
			validationException.add(new FieldError("pushToken", getStrings().get("Push Token is required.")));

		if (clientDevicePushTokenTypeId == null)
			validationException.add(new FieldError("clientDevicePushTokenTypeId", getStrings().get("Client Device Push Token Type ID is required.")));

		if (validationException.hasErrors())
			throw validationException;

		// Make our life simpler and ensure "clientDeviceId" is always set correctly
		clientDeviceId = clientDevice.getClientDeviceId();

		UUID clientDevicePushTokenId = getDatabase().executeReturning("""					
				INSERT INTO client_device_push_token (
				  client_device_id,
				  client_device_push_token_type_id,
				  push_token
				)
				VALUES (?,?,?)
				ON CONFLICT ON CONSTRAINT client_device_push_token_unique_idx
				DO NOTHING
				RETURNING client_device_push_token_id
				""", UUID.class, clientDeviceId, clientDevicePushTokenTypeId, pushToken).orElse(null);

		// Special scenario: ON CONFLICT DO NOTHING in Postgres will return NULL if the row already exists!
		// This is in contrast to ON CONFLICT DO UPDATE.
		// So, we need to pull the ID ourselves here as a workaround.
		if (clientDevicePushTokenId == null)
			clientDevicePushTokenId = findClientDevicePushTokenByClientDeviceAndPushToken(clientDeviceId, clientDevicePushTokenTypeId, pushToken)
					.get()
					.getClientDevicePushTokenId();

		return clientDevicePushTokenId;
	}

	@Nonnull
	public Optional<ClientDeviceActivity> findClientDeviceActivityById(@Nullable UUID clientDeviceActivityId) {
		if (clientDeviceActivityId == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				SELECT *
				FROM client_device_activity
				WHERE client_device_activity_id=?
				""", ClientDeviceActivity.class, clientDeviceActivityId);
	}

	@Nonnull
	public UUID createClientDeviceActivity(@Nonnull CreateClientDeviceActivityRequest request) {
		requireNonNull(request);

		UUID clientDeviceId = request.getClientDeviceId();
		String fingerprint = trimToNull(request.getFingerprint());
		ClientDeviceActivityTypeId clientDeviceActivityTypeId = request.getClientDeviceActivityTypeId();
		UUID accountId = request.getAccountId();
		UUID clientDeviceActivityId = UUID.randomUUID();
		ClientDevice clientDevice = null;
		ValidationException validationException = new ValidationException();

		if (clientDeviceId != null) {
			clientDevice = findClientDeviceById(clientDeviceId).orElse(null);

			if (clientDevice == null)
				validationException.add(new FieldError("clientDeviceId", getStrings().get("Client Device ID is invalid.")));
		} else if (fingerprint != null) {
			clientDevice = findClientDeviceByFingerprint(fingerprint).orElse(null);

			if (clientDevice == null)
				validationException.add(new FieldError("fingerprint", getStrings().get("Client Device Fingerprint is invalid.")));
		} else {
			validationException.add(getStrings().get("Either a Client Device ID or Client Device Fingerprint is required."));
		}

		if (clientDeviceActivityTypeId == null)
			validationException.add(new FieldError("clientDeviceActivityTypeId", getStrings().get("Client Device Activity Type ID is required.")));

		if (validationException.hasErrors())
			throw validationException;

		// Make our life simpler and ensure "clientDeviceId" is always set correctly
		clientDeviceId = clientDevice.getClientDeviceId();

		getDatabase().execute("""					
				INSERT INTO client_device_activity (
				  client_device_activity_id,
				  client_device_id,
				  client_device_activity_type_id,
				  account_id
				)
				VALUES (?,?,?,?)
				""", clientDeviceActivityId, clientDeviceId, clientDeviceActivityTypeId, accountId);

		return clientDeviceActivityId;
	}

	@Nonnull
	public String testClientDevicePushMessage(@Nonnull TestClientDevicePushMessageRequest request) {
		requireNonNull(request);

		UUID accountId = request.getAccountId();
		ClientDevicePushTokenTypeId clientDevicePushTokenTypeId = request.getClientDevicePushTokenTypeId();
		String pushToken = trimToNull(request.getPushToken());
		PushMessageTemplate pushMessageTemplate = request.getPushMessageTemplate();
		Map<String, Object> messageContext = request.getMessageContext();
		Map<String, String> metadata = request.getMetadata();
		Account account = null;

		ValidationException validationException = new ValidationException();

		if (accountId == null) {
			validationException.add(new FieldError("accountId", getStrings().get("Account ID is required.")));
		} else {
			account = getAccountService().findAccountById(accountId).orElse(null);

			if (account == null)
				validationException.add(new FieldError("accountId", getStrings().get("Account ID is invalid.")));
		}

		if (clientDevicePushTokenTypeId == null)
			validationException.add(new FieldError("clientDevicePushTokenTypeId", getStrings().get("Client Device Push Token Type ID is required.")));

		if (pushToken == null)
			validationException.add(new FieldError("pushToken", getStrings().get("Push token is required.")));

		if (pushMessageTemplate == null)
			validationException.add(new FieldError("pushMessageTemplate", getStrings().get("Push Message Template is required.")));

		if (messageContext == null)
			messageContext = Map.of();

		if (metadata == null)
			metadata = Map.of();

		if (validationException.hasErrors())
			throw validationException;

		PushMessage pushMessage = new PushMessage.Builder(account.getInstitutionId(), pushMessageTemplate, clientDevicePushTokenTypeId, pushToken, account.getLocale())
				.messageContext(messageContext)
				.metadata(metadata)
				.build();

		// Other parts of the application should enqueue the message via MessageService.
		// Here, we use the "raw" sender with no enqueuing so exceptions are immediately bubbled out to the caller,
		// which is helpful for testing.
		boolean useRawPushMessageSender = true;

		if (useRawPushMessageSender) {
			EnterprisePlugin enterprisePlugin = getEnterprisePluginProvider().enterprisePluginForInstitutionId(account.getInstitutionId());
			MessageSender<PushMessage> pushMessageSender = enterprisePlugin.pushMessageSenderForPushTokenTypeId(clientDevicePushTokenTypeId);
			return pushMessageSender.sendMessage(pushMessage);
		} else {
			getMessageService().enqueueMessage(pushMessage);
			return String.valueOf(pushMessage.getMessageId());
		}
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
	protected EnterprisePluginProvider getEnterprisePluginProvider() {
		return this.enterprisePluginProvider;
	}

	@Nonnull
	protected Database getDatabase() {
		return this.databaseProvider.get();
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