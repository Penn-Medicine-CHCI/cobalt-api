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

package com.cobaltplatform.api.integration.twilio;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * See https://www.twilio.com/docs/voice/twiml#callstatus-values
 *
 * @author Transmogrify, LLC.
 */
public enum TwilioCallStatus {
	// The call is ready and waiting in line before going out.
	QUEUED("queued"),
	// The call is currently ringing.
	RINGING("ringing"),
	// The call was answered and is actively in progress.
	IN_PROGRESS("in-progress"),
	// The call was answered and has ended normally.
	COMPLETED("completed"),
	// The caller received a busy signal.
	BUSY("busy"),
	// The call could not be completed as dialed, most likely because the phone number was non-existent.
	FAILED("failed"),
	// The call ended without being answered.
	NO_ANSWER("no-answer"),
	// The call was canceled via the REST API while queued or ringing.
	CANCELED("canceled");

	@Nonnull
	private final String name;

	private TwilioCallStatus(@Nonnull String name) {
		requireNonNull(name);
		this.name = name;
	}

	@Nonnull
	private static final Map<String, TwilioCallStatus> TWILIO_CALL_STATUSES_BY_NAME;

	static {
		TWILIO_CALL_STATUSES_BY_NAME = new HashMap<>(TwilioCallStatus.values().length);

		for (TwilioCallStatus twilioCallStatus : TwilioCallStatus.values())
			TWILIO_CALL_STATUSES_BY_NAME.put(twilioCallStatus.getName(), twilioCallStatus);
	}

	@Nonnull
	public static Optional<TwilioCallStatus> fromName(@Nonnull String name) {
		requireNonNull(name);
		return Optional.ofNullable(TWILIO_CALL_STATUSES_BY_NAME.get(name));
	}

	@Nonnull
	public String getName() {
		return this.name;
	}
}
