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

package com.cobaltplatform.api.integration.acuity;

import com.cobaltplatform.api.integration.acuity.model.AcuityError;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Optional;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class AcuitySchedulingException extends RuntimeException {
	@Nullable
	private final AcuityError acuityError;

	public AcuitySchedulingException(@Nullable String message) {
		super(message);
		this.acuityError = null;
	}

	public AcuitySchedulingException(@Nullable String message,
																	 @Nullable AcuityError acuityError) {
		super(message);
		this.acuityError = acuityError;
	}

	public AcuitySchedulingException(@Nullable Throwable cause) {
		super(cause);
		this.acuityError = null;
	}

	public AcuitySchedulingException(@Nullable String message,
																	 @Nullable Throwable cause) {
		super(message, cause);
		this.acuityError = null;
	}

	@Nonnull
	public Optional<AcuityError> getAcuityError() {
		return Optional.ofNullable(acuityError);
	}
}
