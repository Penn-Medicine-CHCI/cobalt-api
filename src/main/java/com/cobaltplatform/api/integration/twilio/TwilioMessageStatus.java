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
 * See https://www.twilio.com/docs/sms/api/message-resource#message-status-values
 *
 * @author Transmogrify, LLC.
 */
public enum TwilioMessageStatus {
	// Twilio has received your API request to send a message with a Messaging Service and a From number is being dynamically selected. This will be the initial status when sending with a Messaging Service and the From parameter.
	ACCEPTED("accepted"),
	// The message is scheduled to be sent. This will be the initial status when scheduling a message with a Messaging Service
	SCHEDULED("scheduled"),
	// The API request to send a message was successful and the message is queued to be sent out. This will be the initial status when you are not using a Messaging Service
	QUEUED("queued"),
	// Twilio is in the process of dispatching your message to the nearest upstream carrier in the network.
	SENDING("sending"),
	// The nearest upstream carrier accepted the message.
	SENT("sent"),
	// The inbound message has been received by Twilio and is currently being processed.
	RECEIVING("receiving"),
	// On inbound messages only. The inbound message was received by one of your Twilio numbers.
	RECEIVED("received"),
	// Twilio has received confirmation of message delivery from the upstream carrier, and, where available, the destination handset.
	DELIVERED("delivered"),
	// 	Twilio has received a delivery receipt indicating that the message was not delivered. This can happen for many reasons including carrier content filtering and the availability of the destination handset.
	UNDELIVERED("undelivered"),
	// The message could not be sent. This can happen for various reasons including queue overflows, account suspensions and media errors (in the case of MMS). Twilio does not charge you for failed messages.
	FAILED("failed"),
	// On WhatsApp messages only. The message has been delivered and opened by the recipient in the conversation. The recipient must have enabled read receipts.
	READ("read"),
	// The message has been canceled. This status is only accessible when using a Messaging Service
	CANCELED("canceled");

	@Nonnull
	private final String name;

	private TwilioMessageStatus(@Nonnull String name) {
		requireNonNull(name);
		this.name = name;
	}

	@Nonnull
	private static final Map<String, TwilioMessageStatus> TWILIO_MESSAGE_STATUSES_BY_NAME;

	static {
		TWILIO_MESSAGE_STATUSES_BY_NAME = new HashMap<>(TwilioMessageStatus.values().length);

		for (TwilioMessageStatus twilioMessageStatus : TwilioMessageStatus.values())
			TWILIO_MESSAGE_STATUSES_BY_NAME.put(twilioMessageStatus.getName(), twilioMessageStatus);
	}

	@Nonnull
	public static Optional<TwilioMessageStatus> fromName(@Nonnull String name) {
		requireNonNull(name);
		return Optional.ofNullable(TWILIO_MESSAGE_STATUSES_BY_NAME.get(name));
	}

	@Nonnull
	public String getName() {
		return this.name;
	}
}
