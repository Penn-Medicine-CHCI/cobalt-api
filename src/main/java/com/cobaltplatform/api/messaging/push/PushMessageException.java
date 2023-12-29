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

package com.cobaltplatform.api.messaging.push;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class PushMessageException extends RuntimeException {
	@Nonnull
	private final PushMessage pushMessage;

	public PushMessageException(@Nullable String message,
															@Nonnull PushMessage pushMessage) {
		this(message, null, pushMessage);
	}

	public PushMessageException(@Nullable Throwable cause,
															@Nonnull PushMessage pushMessage) {
		this(null, cause, pushMessage);
	}

	public PushMessageException(@Nullable String message,
															@Nullable Throwable cause,
															@Nonnull PushMessage pushMessage) {
		super(message, cause);

		requireNonNull(pushMessage);
		this.pushMessage = pushMessage;
	}

	@Nonnull
	public PushMessage getPushMessage() {
		return this.pushMessage;
	}
}
