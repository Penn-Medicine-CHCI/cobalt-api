package com.cobaltplatform.ic.backend.service;

import com.cobaltplatform.ic.backend.exception.CobaltException;
import com.cobaltplatform.ic.backend.model.cobalt.CobaltClaims;
import com.cobaltplatform.ic.backend.model.cobalt.FindAccountResponse;
import com.cobaltplatform.ic.backend.model.db.DCobaltAccount;
import com.cobaltplatform.ic.backend.model.db.query.QDCobaltAccount;
import com.cobaltplatform.ic.model.CobaltAccount;
import io.ebean.DB;
import io.ebean.SqlUpdate;
import io.ebean.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Optional;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class CobaltAccountService {
	@Nonnull
	private static final CobaltAccountService SHARED_INSTANCE;

	@Nonnull
	private final CobaltService cobaltService;
	@Nonnull
	private final Logger logger;

	static {
		SHARED_INSTANCE = new CobaltAccountService();
	}

	public CobaltAccountService() {
		this(CobaltService.getSharedInstance());
	}

	public CobaltAccountService(@Nonnull CobaltService cobaltService) {
		requireNonNull(cobaltService);

		this.cobaltService = cobaltService;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	public Optional<DCobaltAccount> findCobaltAccountById(UUID accountId) {
		if (accountId == null)
			return Optional.empty();

		return new QDCobaltAccount().accountId.eq(accountId).findOneOrEmpty();
	}

	@Nonnull
	public Optional<DCobaltAccount> findCobaltAccountByProviderId(@Nullable UUID providerId) {
		if (providerId == null)
			return Optional.empty();

		return new QDCobaltAccount().providerId.eq(providerId).findOneOrEmpty();
	}

	@Transactional
	public Optional<DCobaltAccount> findOrCreateCobaltAccountForClaims(@Nullable CobaltClaims cobaltClaims) {
		if (cobaltClaims == null)
			return Optional.empty();

		DCobaltAccount existingCobaltAccount = findCobaltAccountById(cobaltClaims.getAccountId()).orElse(null);

		if (existingCobaltAccount != null)
			return Optional.of(existingCobaltAccount);

		CobaltAccount cobaltAccount;

		try {
			FindAccountResponse findAccountResponse = getCobaltService().findAccount(cobaltClaims.getAccountId()).orElse(null);

			if (findAccountResponse == null || findAccountResponse.getAccount() == null)
				return Optional.empty();

			cobaltAccount = findAccountResponse.getAccount();
		} catch (CobaltException e) {
			getLogger().error(String.format("Unable to fetch account ID %s from Cobalt", cobaltClaims.getAccountId()), e);
			throw new RuntimeException(e);
		}

		getLogger().info("No account was found for Cobalt account ID {} and role {}, so creating one in IC...",
				cobaltClaims.getAccountId(), cobaltClaims.getIcRole().name());

		// Use raw SQL since we have an upsert
		String sql = "INSERT INTO cobalt_account (account_id, provider_id, role_id, first_name, last_name, display_name, email_address, created_dt, updated_dt) "
				+ "VALUES (:accountId, :providerId, :roleId, :firstName, :lastName, :displayName, :emailAddress, now(), now()) ON CONFLICT DO NOTHING";

		SqlUpdate sqlUpdate = DB.sqlUpdate(sql)
				.setParameter("accountId", cobaltAccount.getAccountId())
				.setParameter("providerId", cobaltAccount.getProviderId())
				.setParameter("roleId", cobaltAccount.getRoleId())
				.setParameter("firstName", cobaltAccount.getFirstName())
				.setParameter("lastName", cobaltAccount.getLastName())
				.setParameter("displayName", cobaltAccount.getDisplayName())
				.setParameter("emailAddress", cobaltAccount.getEmailAddress());

		sqlUpdate.execute();

		return findCobaltAccountById(cobaltClaims.getAccountId());
	}

	@Nonnull
	public static CobaltAccountService getSharedInstance() {
		return SHARED_INSTANCE;
	}

	@Nonnull
	protected CobaltService getCobaltService() {
		return cobaltService;
	}

	@Nonnull
	protected Logger getLogger() {
		return logger;
	}
}