package com.cobaltplatform.ic.backend.model.cobalt;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Immutable
public class CreateOrderReportPatientRequest {
	@Nonnull
	private final String uid;
	@Nullable
	private final String firstName;
	@Nullable
	private final String lastName;

	public CreateOrderReportPatientRequest(@Nonnull String uid,
																				 @Nullable String firstName,
																				 @Nullable String lastName) {
		requireNonNull(uid);

		this.uid = uid;
		this.firstName = firstName;
		this.lastName = lastName;
	}

	@Nonnull
	public String getUid() {
		return uid;
	}

	@Nonnull
	public Optional<String> getFirstName() {
		return Optional.ofNullable(firstName);
	}

	@Nonnull
	public Optional<String> getLastName() {
		return Optional.ofNullable(lastName);
	}
}