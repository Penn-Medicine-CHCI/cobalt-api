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

package com.cobaltplatform.api.integration.way2health.model.request;

import com.cobaltplatform.api.integration.way2health.Way2HealthGsonSupport;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.List;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class PatchIncidentsRequest {
	@Nullable
	private String id; // Format is "operator(value|value2)", e.g. "in(1234|1553)". Supported operators: is, in, not
	@Nullable
	private List<PatchOperation> patchOperations;

	@Override
	public String toString() {
		return Way2HealthGsonSupport.sharedGson().toJson(this);
	}

	@NotThreadSafe
	public static class PatchOperation {
		@Nullable
		private String path;
		@Nullable
		private String op;
		@Nullable
		private String value;

		@Override
		public String toString() {
			return Way2HealthGsonSupport.sharedGson().toJson(this);
		}

		@Nullable
		public String getPath() {
			return path;
		}

		public void setPath(@Nullable String path) {
			this.path = path;
		}

		@Nullable
		public String getOp() {
			return op;
		}

		public void setOp(@Nullable String op) {
			this.op = op;
		}

		@Nullable
		public String getValue() {
			return value;
		}

		public void setValue(@Nullable String value) {
			this.value = value;
		}
	}

	@Nullable
	public String getId() {
		return id;
	}

	public void setId(@Nullable String id) {
		this.id = id;
	}

	@Nullable
	public List<PatchOperation> getPatchOperations() {
		return patchOperations;
	}

	public void setPatchOperations(@Nullable List<PatchOperation> patchOperations) {
		this.patchOperations = patchOperations;
	}
}
