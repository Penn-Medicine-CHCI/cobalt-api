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

package com.cobaltplatform.api.messaging.email;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Immutable
public class EmailAttachment {
	@Nonnull
	private final String filename;
	@Nonnull
	private final String contentType;
	@Nonnull
	private final byte[] data;

	public EmailAttachment(@Nonnull String filename, @Nonnull String contentType, @Nonnull byte[] data) {
		requireNonNull(filename);
		requireNonNull(contentType);
		requireNonNull(data);

		this.filename = filename;
		this.contentType = contentType;
		this.data = data;
	}

	@Override
	public String toString() {
		return format("%s{filename=%s, contentType=%s, data=%d bytes}", getClass().getSimpleName(), getFilename(),
				getContentType(), getData().length);
	}

	@Nonnull
	public String getFilename() {
		return this.filename;
	}

	@Nonnull
	public String getContentType() {
		return this.contentType;
	}

	@Nonnull
	public byte[] getData() {
		return this.data;
	}
}
