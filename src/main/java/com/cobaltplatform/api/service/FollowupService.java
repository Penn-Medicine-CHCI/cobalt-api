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
import com.cobaltplatform.api.model.api.request.CreateFollowupRequest;
import com.cobaltplatform.api.model.api.request.FindFollowupsRequest;
import com.cobaltplatform.api.model.db.Followup;
import com.cobaltplatform.api.model.db.Provider;
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
import javax.inject.Singleton;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.cobaltplatform.api.util.DatabaseUtility.sqlVaragsParameters;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@Singleton
@ThreadSafe
public class FollowupService {
	@Nonnull
	private final javax.inject.Provider<AccountService> accountServiceProvider;
	@Nonnull
	private final javax.inject.Provider<ProviderService> providerServiceProvider;
	@Nonnull
	private final DatabaseProvider databaseProvider;
	@Nonnull
	private final Configuration configuration;
	@Nonnull
	private final Strings strings;
	@Nonnull
	private final Logger logger;

	@Inject
	public FollowupService(@Nonnull javax.inject.Provider<AccountService> accountServiceProvider,
												 @Nonnull javax.inject.Provider<ProviderService> providerServiceProvider,
												 @Nonnull DatabaseProvider databaseProvider,
												 @Nonnull Configuration configuration,
												 @Nonnull Strings strings) {
		requireNonNull(accountServiceProvider);
		requireNonNull(providerServiceProvider);
		requireNonNull(databaseProvider);
		requireNonNull(configuration);
		requireNonNull(strings);

		this.accountServiceProvider = accountServiceProvider;
		this.providerServiceProvider = providerServiceProvider;
		this.databaseProvider = databaseProvider;
		this.configuration = configuration;
		this.strings = strings;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	public Optional<Followup> findFollowupById(@Nullable UUID followupId) {
		if (followupId == null)
			return Optional.empty();

		return getDatabase().queryForObject("SELECT * FROM followup WHERE followup_id=? AND canceled=FALSE",
				Followup.class, followupId);
	}

	@Nonnull
	public UUID createFollowup(@Nonnull CreateFollowupRequest request) {
		requireNonNull(request);

		UUID providerId = request.getProviderId();
		UUID accountId = request.getAccountId();
		UUID createdByAccountId = request.getCreatedByAccountId();
		LocalDate followupDate = request.getFollowupDate();
		UUID appointmentReasonId = request.getAppointmentReasonId();
		String comment = trimToNull(request.getComment());

		ValidationException validationException = new ValidationException();

		if (providerId == null) {
			validationException.add(new FieldError("providerId", getStrings().get("Provider ID is required.")));
		} else {
			Provider provider = getProviderService().findProviderById(providerId).orElse(null);

			if (provider == null)
				validationException.add(new FieldError("providerId", getStrings().get("Provider ID is invalid.")));
		}

		if (accountId == null)
			validationException.add(new FieldError("accountId", getStrings().get("Account ID is required.")));

		if (createdByAccountId == null)
			validationException.add(new FieldError("createdByAccountId", getStrings().get("Created By Account ID is required.")));

		if (followupDate == null)
			validationException.add(new FieldError("followupDate", getStrings().get("Date is required.")));

		if (followupDate == null)
			validationException.add(new FieldError("appointmentReasonId", getStrings().get("Reason is required.")));

		if (validationException.hasErrors())
			throw validationException;

		UUID followupId = UUID.randomUUID();

		getDatabase().execute("INSERT INTO followup(followup_id, provider_id, account_id, created_by_account_id, appointment_reason_id, " +
						"followup_date, comment) VALUES (?,?,?,?,?,?,?)",
				followupId, providerId, accountId, createdByAccountId, appointmentReasonId, followupDate, comment);

		return followupId;
	}

	@Nonnull
	public List<Followup> findFollowupsByProviderId(@Nullable UUID providerId,
																									@Nullable LocalDate startDate,
																									@Nullable LocalDate endDate) {
		if (providerId == null)
			return Collections.emptyList();

		StringBuilder sql = new StringBuilder("SELECT * FROM followup WHERE canceled=FALSE ");
		List<Object> parameters = new ArrayList<>();

		sql.append("AND provider_id = ? ");
		parameters.add(providerId);

		if (startDate != null) {
			sql.append("AND followup_date >= ? ");
			parameters.add(startDate);
		}

		if (endDate != null) {
			sql.append("AND followup_date <= ? ");
			parameters.add(endDate);
		}

		sql.append("ORDER BY followup_date, account_id");

		return getDatabase().queryForList(sql.toString(), Followup.class, sqlVaragsParameters(parameters));
	}

	@Nonnull
	public List<Followup> findFollowups(@Nonnull FindFollowupsRequest request) {
		requireNonNull(request);

		UUID accountId = request.getAccountId();
		FindFollowupsRequest.FilterBy filterBy = request.getFilterBy() == null ? FindFollowupsRequest.FilterBy.ALL : request.getFilterBy();
		ZoneId timeZone = request.getTimeZone();

		if (accountId == null)
			return Collections.emptyList();

		if (timeZone == null && filterBy == FindFollowupsRequest.FilterBy.UPCOMING)
			throw new ValidationException(getStrings().get("You must provide a time zone if you are filtering on {{filterBy}} followups.", new HashMap<String, Object>() {{
				put("filterBy", FindFollowupsRequest.FilterBy.UPCOMING.name());
			}}));

		StringBuilder sql = new StringBuilder("SELECT * FROM followup WHERE canceled=FALSE ");
		List<Object> parameters = new ArrayList<>();

		sql.append("AND account_id = ? ");
		parameters.add(accountId);

		if (filterBy == FindFollowupsRequest.FilterBy.UPCOMING) {
			LocalDate now = LocalDate.now(timeZone);

			sql.append("AND followup_date >= ? ");
			parameters.add(now);
		}

		sql.append("ORDER BY followup_date, account_id");

		return getDatabase().queryForList(sql.toString(), Followup.class, sqlVaragsParameters(parameters));
	}

	@Nonnull
	public Boolean cancelFollowup(@Nullable UUID followupId) {
		if (followupId == null)
			return false;

		return getDatabase().execute("UPDATE followup SET canceled=TRUE, canceled_at=NOW() WHERE followup_id=?", followupId) > 0;
	}

	@Nonnull
	protected AccountService getAccountService() {
		return accountServiceProvider.get();
	}

	@Nonnull
	protected ProviderService getProviderService() {
		return providerServiceProvider.get();
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
