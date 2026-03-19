/*
 * Copyright 2026 Cobalt Innovations, Inc.
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

package com.cobaltplatform.api.model.db;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import static java.lang.String.format;

/**
 * @author Cobalt Innovations, Inc.
 */
@NotThreadSafe
public class IpGeolocationStatus {
	@Nullable
	private IpGeolocationStatusId ipGeolocationStatusId;
	@Nullable
	private String description;

	public enum IpGeolocationStatusId {
		PENDING,
		IN_PROGRESS,
		SUCCEEDED,
		FAILED,
		SKIPPED_INVALID,
		SKIPPED_PRIVATE
	}

	@Override
	public String toString() {
		return format("%s{ipGeolocationStatusId=%s, description=%s}", getClass().getSimpleName(), getIpGeolocationStatusId(), getDescription());
	}

	@Nullable
	public IpGeolocationStatusId getIpGeolocationStatusId() {
		return this.ipGeolocationStatusId;
	}

	public void setIpGeolocationStatusId(@Nullable IpGeolocationStatusId ipGeolocationStatusId) {
		this.ipGeolocationStatusId = ipGeolocationStatusId;
	}

	@Nullable
	public String getDescription() {
		return this.description;
	}

	public void setDescription(@Nullable String description) {
		this.description = description;
	}
}
