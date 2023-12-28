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

import com.cobaltplatform.api.model.api.request.UpsertClientDevicePushTokenRequest;
import com.cobaltplatform.api.model.api.request.UpsertClientDeviceRequest;
import com.cobaltplatform.api.model.db.ClientDevice;
import com.cobaltplatform.api.model.db.ClientDevicePushToken;
import com.cobaltplatform.api.model.db.ClientDevicePushTokenType.ClientDevicePushTokenTypeId;
import com.cobaltplatform.api.model.db.ClientDeviceType.ClientDeviceTypeId;
import com.cobaltplatform.api.util.ValidationException;
import com.cobaltplatform.api.util.ValidationException.FieldError;
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
	private final Database database;
	@Nonnull
	private final Strings strings;
	@Nonnull
	private final Logger logger;

	@Inject
	public ClientDeviceService(@Nonnull Database database,
														 @Nonnull Strings strings) {
		requireNonNull(database);
		requireNonNull(strings);

		this.database = database;
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
		String modelName = trimToNull(request.getModelName());
		String operatingSystemName = trimToNull(request.getOperatingSystemName());
		String operatingSystemVersion = trimToNull(request.getOperatingSystemVersion());

		ValidationException validationException = new ValidationException();

		if (accountId == null)
			validationException.add(new FieldError("accountId", getStrings().get("Account ID is required.")));

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
				  model_name,
				  operating_system_name,
				  operating_system_version
				)
				VALUES (?,?,?,?,?)
				ON CONFLICT ON CONSTRAINT client_device_unique_idx
				DO UPDATE SET
				  operating_system_name=EXCLUDED.operating_system_name,
				  operating_system_version=EXCLUDED.operating_system_version
				RETURNING client_device_id
				""", UUID.class, clientDeviceTypeId, fingerprint, modelName, operatingSystemName, operatingSystemVersion).get();

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
				getLogger().debug("Client device already associated with account, don't need to re-associate.");
				transaction.rollback(savepoint);
			} else {
				throw e;
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
		UUID accountId = request.getAccountId();
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

		if (accountId == null)
			validationException.add(new FieldError("accountId", getStrings().get("Account ID is required.")));

		if (validationException.hasErrors())
			throw validationException;

		// Make our life simpler and ensure "clientDeviceId" is always set correctly
		clientDeviceId = clientDevice.getClientDeviceId();

		// Don't let people "guess" fingerprints or IDs and associate push tokens with others' devices
		if (!isAccountAssociatedWithClientDeviceId(accountId, clientDeviceId))
			throw new ValidationException(getStrings().get("Cannot persist push token because Account ID {{accountId}} is not associated with Client Device ID {{clientDeviceId}}.", Map.of(
					"accountId", accountId,
					"clientDeviceId", clientDeviceId
			)));

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
	protected Database getDatabase() {
		return this.database;
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