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

package com.cobaltplatform.api.model.api.request;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.Map;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class CreateFileUploadRequest {
	@Nullable
	private UUID accountId;
	@Nullable
	private String storageKeyPrefix;
	@Nullable
	private String filename;
	@Nullable
	private String contentType;
	@Nullable
	private Boolean publicRead;
	@Nullable
	private Map<String, String> metadata;

	@Nullable
	public UUID getAccountId() {
		return this.accountId;
	}

	public void setAccountId(@Nullable UUID accountId) {
		this.accountId = accountId;
	}

	@Nullable
	public String getStorageKeyPrefix() {
		return this.storageKeyPrefix;
	}

	public void setStorageKeyPrefix(@Nullable String storageKeyPrefix) {
		this.storageKeyPrefix = storageKeyPrefix;
	}

	@Nullable
	public String getFilename() {
		return this.filename;
	}

	public void setFilename(@Nullable String filename) {
		this.filename = filename;
	}

	@Nullable
	public String getContentType() {
		return this.contentType;
	}

	public void setContentType(@Nullable String contentType) {
		this.contentType = contentType;
	}

	@Nullable
	public Boolean getPublicRead() {
		return this.publicRead;
	}

	public void setPublicRead(@Nullable Boolean publicRead) {
		this.publicRead = publicRead;
	}

	@Nullable
	public Map<String, String> getMetadata() {
		return this.metadata;
	}

	public void setMetadata(@Nullable Map<String, String> metadata) {
		this.metadata = metadata;
	}
}
