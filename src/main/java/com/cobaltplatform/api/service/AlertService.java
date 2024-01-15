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

import com.cobaltplatform.api.model.api.request.CreateAlertDismissalRequest;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.Alert;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.util.ValidationException;
import com.cobaltplatform.api.util.ValidationException.FieldError;
import com.cobaltplatform.api.util.db.DatabaseProvider;
import com.lokalized.Strings;
import com.pyranid.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Singleton
@ThreadSafe
public class AlertService {
	@Nonnull
	private final Provider<AccountService> accountServiceProvider;
	@Nonnull
	private final Provider<InstitutionService> institutionServiceProvider;
	@Nonnull
	private final DatabaseProvider databaseProvider;
	@Nonnull
	private final Strings strings;
	@Nonnull
	private final Logger logger;

	@Inject
	public AlertService(@Nonnull Provider<AccountService> accountServiceProvider,
											@Nonnull Provider<InstitutionService> institutionServiceProvider,
											@Nonnull DatabaseProvider databaseProvider,
											@Nonnull Strings strings) {
		requireNonNull(accountServiceProvider);
		requireNonNull(institutionServiceProvider);
		requireNonNull(databaseProvider);
		requireNonNull(strings);

		this.accountServiceProvider = accountServiceProvider;
		this.institutionServiceProvider = institutionServiceProvider;
		this.databaseProvider = databaseProvider;
		this.strings = strings;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	public Optional<Alert> findAlertById(@Nullable UUID alertId) {
		if (alertId == null)
			return Optional.empty();

		return getDatabase().queryForObject("SELECT * FROM alert WHERE alert_id=?", Alert.class, alertId);
	}

	@Nonnull
	public List<Alert> findAlertsByInstitutionId(@Nullable InstitutionId institutionId) {
		if (institutionId == null)
			return List.of();

		return getDatabase().queryForList("""
				SELECT a.*
				FROM alert a, institution_alert ai, alert_type at
				WHERE a.alert_id=ai.alert_id
				AND ai.active=TRUE
				AND a.alert_type_id=at.alert_type_id
				AND ai.institution_id=?
				ORDER BY at.severity DESC, a.title, a.message
				""", Alert.class, institutionId);
	}

	@Nonnull
	public List<Alert> findUndismissedInstitutionAlertsByAccountId(@Nullable UUID accountId) {
		if (accountId == null)
			return List.of();

		return getDatabase().queryForList("""
				SELECT a.*
				FROM alert a, institution_alert ai, account ac, alert_type at
				WHERE a.alert_id=ai.alert_id
				AND ai.active=TRUE
				AND ai.institution_id=ac.institution_id				
				AND ac.account_id=?
				AND a.alert_type_id=at.alert_type_id
				AND a.alert_id NOT IN (SELECT ad.alert_id FROM alert_dismissal ad WHERE ad.account_id=ac.account_id)
				ORDER BY at.severity DESC, a.title, a.message
				""", Alert.class, accountId);
	}

	@Nonnull
	public UUID createAlertDismissal(@Nonnull CreateAlertDismissalRequest request) {
		requireNonNull(request);

		UUID accountId = request.getAccountId();
		UUID alertId = request.getAlertId();
		UUID alertDismissalId = UUID.randomUUID();
		ValidationException validationException = new ValidationException();

		if (accountId == null) {
			validationException.add(new FieldError("accountId", getStrings().get("Account ID is required.")));
		} else {
			Account account = getAccountService().findAccountById(accountId).orElse(null);

			if (account == null)
				validationException.add(new FieldError("accountId", getStrings().get("Account ID is invalid.")));
		}

		if (alertId == null) {
			validationException.add(new FieldError("alertId", getStrings().get("Alert ID is required.")));
		} else {
			Alert alert = findAlertById(alertId).orElse(null);

			if (alert == null)
				validationException.add(new FieldError("alertId", getStrings().get("Alert ID is invalid.")));
		}

		if (validationException.hasErrors())
			throw validationException;

		getDatabase().execute("""
				INSERT INTO alert_dismissal (
				alert_dismissal_id,
				account_id,
				alert_id
				) VALUES (?,?,?)
				""", alertDismissalId, accountId, alertId);

		return alertDismissalId;
	}

	@Nonnull
	protected AccountService getAccountService() {
		return this.accountServiceProvider.get();
	}

	@Nonnull
	protected InstitutionService getInstitutionService() {
		return this.institutionServiceProvider.get();
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
