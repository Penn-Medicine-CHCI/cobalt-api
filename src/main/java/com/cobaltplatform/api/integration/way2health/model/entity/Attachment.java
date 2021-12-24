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

package com.cobaltplatform.api.integration.way2health.model.entity;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.math.BigInteger;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class Attachment extends Way2HealthEntity {
	@Nullable
	private BigInteger attachmentId;
	@Nullable
	private String objectType;
	@Nullable
	private Details details;

	@NotThreadSafe
	public static class Details extends Way2HealthEntity {
		@Nullable
		private String message;

		@Nullable
		public String getMessage() {
			return message;
		}

		public void setMessage(@Nullable String message) {
			this.message = message;
		}
	}

	@Nullable
	public BigInteger getAttachmentId() {
		return attachmentId;
	}

	public void setAttachmentId(@Nullable BigInteger attachmentId) {
		this.attachmentId = attachmentId;
	}

	@Nullable
	public String getObjectType() {
		return objectType;
	}

	public void setObjectType(@Nullable String objectType) {
		this.objectType = objectType;
	}

	@Nullable
	public Details getDetails() {
		return details;
	}

	public void setDetails(@Nullable Details details) {
		this.details = details;
	}
}
