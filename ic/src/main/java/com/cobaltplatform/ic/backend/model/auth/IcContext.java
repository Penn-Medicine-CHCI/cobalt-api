package com.cobaltplatform.ic.backend.model.auth;

import com.cobaltplatform.ic.model.IcRole;
import com.cobaltplatform.ic.backend.model.db.DCobaltAccount;
import com.cobaltplatform.ic.backend.model.db.DPatient;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class IcContext {
	@Nonnull
	private static final ThreadLocal<IcContext> IC_CONTEXT_HOLDER;

	@Nullable
	private DPatient patient;
	@Nullable
	private DCobaltAccount cobaltAccount;

	static {
		IC_CONTEXT_HOLDER = new ThreadLocal<>();
	}

	public IcContext() {
		this(null, null);
	}

	public IcContext(@Nullable DCobaltAccount cobaltAccount) {
		this(null, cobaltAccount);
	}

	public IcContext(@Nullable DPatient patient) {
		this(patient, null);
	}

	public IcContext(@Nullable DPatient patient,
									 @Nullable DCobaltAccount cobaltAccount) {
		this.patient = patient;
		this.cobaltAccount = cobaltAccount;
	}

	/**
	 * Should always be matched with {@link #clear()}.
	 *
	 * @param icContext the context to set (not null)
	 */
	public static void set(@Nonnull IcContext icContext) {
		requireNonNull(icContext);
		getIcContextHolder().set(icContext);
	}

	/**
	 * Should always be matched with {@link #set(IcContext)}.
	 */
	public static void clear() {
		getIcContextHolder().remove();
	}

	/**
	 * Convenience shorthand
	 */
	public boolean hasRole(@Nonnull IcRole icRole) {
		requireNonNull(icRole);
		return hasRole(Set.of(icRole));
	}

	/**
	 * Convenience shorthand
	 */
	public boolean hasRole(@Nonnull Set<IcRole> icRoles) {
		requireNonNull(icRoles);

		DCobaltAccount cobaltAccount = getCobaltAccount().orElse(null);

		for(IcRole icRole : icRoles) {
			if (icRole == IcRole.ANYONE)
				return true;

			if(icRole == IcRole.PATIENT && getPatient().isPresent())
				return true;

			// MHIC or other future roles for non-patients
			if (cobaltAccount != null && icRole.name().equals(cobaltAccount.getRoleId()))
				return true;
		}

		return false;
	}

	/**
	 * Convenience shorthand for threadlocal version
	 */
	public static boolean currentContextHasRole(@Nonnull IcRole icRole) {
		requireNonNull(icRole);
		return currentContextHasRole(Set.of(icRole));
	}

	/**
	 * Convenience shorthand for threadlocal version
	 */
	public static boolean currentContextHasRole(@Nonnull Set<IcRole> icRoles) {
		requireNonNull(icRoles);

		IcContext icContext = IcContext.getCurrentContext().orElse(null);
		return icContext == null ? false : icContext.hasRole(icRoles);
	}

	/**
	 * Convenience shorthand
	 */
	public boolean hasPatientId(@Nonnull UUID patientId) {
		requireNonNull(patientId);

		DPatient patient = getPatient().orElse(null);

		if(patient == null)
			return false;

		return patient.getId().equals(patientId);
	}

	/**
	 * Convenience shorthand for threadlocal version
	 */
	public static boolean currentContextHasPatientId(@Nonnull UUID patientId) {
		requireNonNull(patientId);

		IcContext icContext = IcContext.getCurrentContext().orElse(null);
		return icContext == null ? false : icContext.hasPatientId(patientId);
	}

	/**
	 * Convenience shorthand for threadlocal version
	 */
	public static Optional<DPatient> currentContextPatient() {
		IcContext icContext = IcContext.getCurrentContext().orElse(null);
		return icContext == null ? Optional.empty() : icContext.getPatient();
	}

	/**
	 * Convenience shorthand for threadlocal version
	 */
	public static Optional<DCobaltAccount> currentContextCobaltAccount() {
		IcContext icContext = IcContext.getCurrentContext().orElse(null);
		return icContext == null ? Optional.empty() : icContext.getCobaltAccount();
	}

	@Nonnull
	public static Optional<IcContext> getCurrentContext() {
		return Optional.ofNullable(getIcContextHolder().get());
	}

	@Nonnull
	private static ThreadLocal<IcContext> getIcContextHolder() {
		return IC_CONTEXT_HOLDER;
	}

	public void setPatient(@Nullable DPatient patient) {
		this.patient = patient;
	}

	@Nonnull
	public Optional<DPatient> getPatient() {
		return Optional.ofNullable(patient);
	}

	public void setCobaltAccount(@Nullable DCobaltAccount cobaltAccount) {
		this.cobaltAccount = cobaltAccount;
	}

	@Nonnull
	public Optional<DCobaltAccount> getCobaltAccount() {
		return Optional.ofNullable(cobaltAccount);
	}
}
