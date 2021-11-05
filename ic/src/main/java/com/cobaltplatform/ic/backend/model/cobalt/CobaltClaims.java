package com.cobaltplatform.ic.backend.model.cobalt;

import com.cobaltplatform.ic.model.IcRole;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import java.util.Objects;
import java.util.UUID;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Immutable
public class CobaltClaims {
	@Nonnull
	private final UUID accountId;
	@Nonnull
	private final IcRole icRole;

	public CobaltClaims(@Nonnull UUID accountId,
											@Nonnull IcRole icRole) {
		requireNonNull(accountId);
		requireNonNull(icRole);

		this.accountId = accountId;
		this.icRole = icRole;
	}

	@Override
	public String toString() {
		return format("%s{accountId=%s, picRole=%s}", getClass().getSimpleName(), getAccountId(), getIcRole().getName());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		CobaltClaims that = (CobaltClaims) o;
		return getAccountId().equals(that.getAccountId()) &&
				getIcRole() == that.getIcRole();
	}

	@Override
	public int hashCode() {
		return Objects.hash(getAccountId(), getIcRole());
	}

	@Nonnull
	public UUID getAccountId() {
		return accountId;
	}

	@Nonnull
	public IcRole getIcRole() {
		return icRole;
	}
}
