package com.cobaltplatform.api.model.service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Locale;
import java.util.Optional;

import static com.cobaltplatform.api.util.ValidationUtility.isValidEmailAddress;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class IcTestPatientEmailAddress {
	@Nonnull
	private final String firstName;
	@Nonnull
	private final String lastName;
	@Nonnull
	private final String uid;
	@Nonnull
	private final String mrn;
	@Nonnull
	private final String emailAddress;

	public IcTestPatientEmailAddress(@Nonnull String firstName,
																	 @Nonnull String lastName,
																	 @Nonnull String uid,
																	 @Nonnull String mrn) {
		firstName = trimToNull(firstName);
		lastName = trimToNull(lastName);
		uid = trimToNull(uid);
		mrn = trimToNull(mrn);

		requireNonNull(firstName);
		requireNonNull(lastName);
		requireNonNull(uid);
		requireNonNull(mrn);

		this.firstName = firstName;
		this.lastName = lastName;
		this.uid = uid;
		this.mrn = mrn;

		// e.g. maisie.solomon.80991084.46562620@test.cobaltintegratedcare.com
		this.emailAddress = format("%s.%s.%s.%s@test.cobaltintegratedcare.com",
				firstName, lastName, uid, mrn
		).toLowerCase(Locale.US);
	}

	@Nonnull
	public static Optional<IcTestPatientEmailAddress> fromEmailAddress(@Nullable String emailAddress) {
		emailAddress = trimToNull(emailAddress);

		if (emailAddress == null)
			return Optional.empty();

		if (!isValidEmailAddress(emailAddress))
			return Optional.empty();

		emailAddress = emailAddress.toLowerCase(Locale.US);

		// e.g. maisie.solomon.80991084.46562620@test.cobaltintegratedcare.com
		if (!emailAddress.endsWith("@test.cobaltintegratedcare.com"))
			return Optional.empty();

		emailAddress = emailAddress.replace("@test.cobaltintegratedcare.com", "");
		String[] emailAddressComponents = emailAddress.split("\\.");

		if (emailAddressComponents.length != 4)
			return Optional.empty();

		String firstName = emailAddressComponents[0];
		String lastName = emailAddressComponents[1];
		String uid = emailAddressComponents[2];
		String mrn = emailAddressComponents[3];

		return Optional.of(new IcTestPatientEmailAddress(firstName, lastName, uid, mrn));
	}

	@Nonnull
	public static Boolean isTestEmailAddress(@Nullable String emailAddress) {
		return fromEmailAddress(emailAddress).isPresent();
	}

	@Nonnull
	public String getFirstName() {
		return this.firstName;
	}

	@Nonnull
	public String getLastName() {
		return this.lastName;
	}

	@Nonnull
	public String getUid() {
		return this.uid;
	}

	@Nonnull
	public String getMrn() {
		return this.mrn;
	}

	@Nonnull
	public String getEmailAddress() {
		return this.emailAddress;
	}
}
