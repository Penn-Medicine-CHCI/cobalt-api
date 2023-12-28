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

import com.cobaltplatform.api.model.api.request.UpsertClientDeviceRequest;
import com.cobaltplatform.api.model.db.ClientDevice;
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