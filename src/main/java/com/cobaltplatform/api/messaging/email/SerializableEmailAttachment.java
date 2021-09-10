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

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class SerializableEmailAttachment {
	@Nullable
	private String filename;
	@Nullable
	private String contentType;
	@Nullable
	private String key; // Pointer to where the file lives (e.g. S3)

	@Nullable
	public String getFilename() {
		return filename;
	}

	public void setFilename(@Nullable String filename) {
		this.filename = filename;
	}

	@Nullable
	public String getContentType() {
		return contentType;
	}

	public void setContentType(@Nullable String contentType) {
		this.contentType = contentType;
	}

	@Nullable
	public String getKey() {
		return key;
	}

	public void setKey(@Nullable String key) {
		this.key = key;
	}
}