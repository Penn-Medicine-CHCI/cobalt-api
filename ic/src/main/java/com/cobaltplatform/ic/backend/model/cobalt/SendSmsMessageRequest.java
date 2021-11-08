package com.cobaltplatform.ic.backend.model.cobalt;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Immutable
public class SendSmsMessageRequest {
	@Nonnull
	private final String toNumber;
	@Nonnull
	private final String body;

	public SendSmsMessageRequest(@Nonnull String toNumber,
															 @Nonnull String body) {
		requireNonNull(toNumber);
		requireNonNull(body);

		this.toNumber = toNumber;
		this.body = body;
	}

	@Nonnull
	public String getToNumber() {
		return toNumber;
	}

	@Nonnull
	public String getBody() {
		return body;
	}
}
