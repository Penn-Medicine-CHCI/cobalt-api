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

package com.cobaltplatform.api.util;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public final class IdUtility {
	@Nonnull
	private static final SecureRandom SECURE_RANDOM;
	@Nonnull
	private static final Base64.Encoder BASE_64_ENCODER;

	private IdUtility() {
	}

	static {
		// This could be made more performant (less contention) if bound to a threadlocal in CurrentContext
		SECURE_RANDOM = new SecureRandom();
		BASE_64_ENCODER = Base64.getUrlEncoder().withoutPadding();
	}

	// This is equivalent to a UUID in terms of bytes of randomness.
	// It is a shorter representation, useful for URLs and the like
	@Nonnull
	public static String generateId() {
		return generateId(16);
	}

	// Per https://neilmadden.blog/2018/08/30/moving-away-from-uuids/
	@Nonnull
	public static String generateId(@Nonnull Integer bytesOfRandomness) {
		byte[] buffer = new byte[bytesOfRandomness];
		getSecureRandom().nextBytes(buffer);
		return getBase64Encoder().encodeToString(buffer);
	}

	@Nonnull
	private static SecureRandom getSecureRandom() {
		return SECURE_RANDOM;
	}

	@Nonnull
	private static Base64.Encoder getBase64Encoder() {
		return BASE_64_ENCODER;
	}
}
