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

package com.cobaltplatform.api.integration.microsoft;

import com.cobaltplatform.api.integration.microsoft.model.OnlineMeeting;
import com.cobaltplatform.api.integration.microsoft.model.Subscription;
import com.cobaltplatform.api.integration.microsoft.model.User;
import com.cobaltplatform.api.integration.microsoft.request.OnlineMeetingCreateRequest;
import com.cobaltplatform.api.integration.microsoft.request.SubscriptionCreateRequest;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Optional;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public interface MicrosoftClient {
	/**
	 * Requires {@code User.ReadBasic.All} for Daemon apps.
	 */
	@Nonnull
	Optional<User> getUser(@Nullable String id) throws MicrosoftException;

	/**
	 * List of legal subscription resource paths is available here:
	 * https://learn.microsoft.com/en-us/graph/api/resources/webhooks?view=graph-rest-1.0
	 * <p>
	 * TODO: incorporate `clientState` generator - signed JWT timeboxed to maximum subscription refresh window?
	 */
	@Nonnull
	Subscription createSubscription(@Nonnull SubscriptionCreateRequest request) throws MicrosoftException;

	@Nonnull
	void deleteSubscription(@Nullable String id) throws MicrosoftException;

	// TODO: implement PATCH for subscriptions to support renewing (for example)
	// PATCH https://graph.microsoft.com/v1.0/subscriptions/{id}
	// Content-Type: application/json
	//
	// {
	//  "expirationDateTime": "2022-10-22T11:00:00.0000000Z"
	// }

	@Nonnull
	OnlineMeeting createOnlineMeeting(@Nonnull OnlineMeetingCreateRequest request) throws MicrosoftException;
}