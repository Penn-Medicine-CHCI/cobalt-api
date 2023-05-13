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

package com.cobaltplatform.api.integration.amazon;

import javax.annotation.Nonnull;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
public enum AmazonSnsMessageType {
	NOTIFICATION,
	SUBSCRIPTION_CONFIRMATION,
	UNSUBSCRIBE_CONFIRMATION;

	@Nonnull
	public static Optional<AmazonSnsMessageType> fromType(@Nonnull String type) {
		requireNonNull(type);

		if ("Notification".equals(type))
			return Optional.of(NOTIFICATION);
		if ("SubscriptionConfirmation".equals(type))
			return Optional.of(SUBSCRIPTION_CONFIRMATION);
		if ("UnsubscribeConfirmation".equals(type))
			return Optional.of(UNSUBSCRIBE_CONFIRMATION);

		return Optional.empty();
	}
}
