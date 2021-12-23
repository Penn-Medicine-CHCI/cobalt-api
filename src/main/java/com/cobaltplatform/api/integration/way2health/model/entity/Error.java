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
import java.util.List;
import java.util.Map;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class Error extends Way2HealthEntity {
	@Nullable
	private Integer code;
	@Nullable
	private Integer resourceId;
	@Nullable
	private Map<String, Object> payload; // Don't have a good example of this yet
	@Nullable
	private List<String> errors;
	@Nullable
	private List<FieldError> fieldErrors;

	@NotThreadSafe
	public static class FieldError {
		@Nullable
		private String field;
		@Nullable
		private String originalValue;
		@Nullable
		private String message;

		@Nullable
		public String getField() {
			return field;
		}

		public void setField(@Nullable String field) {
			this.field = field;
		}

		@Nullable
		public String getOriginalValue() {
			return originalValue;
		}

		public void setOriginalValue(@Nullable String originalValue) {
			this.originalValue = originalValue;
		}

		@Nullable
		public String getMessage() {
			return message;
		}

		public void setMessage(@Nullable String message) {
			this.message = message;
		}
	}

	@Nullable
	public Integer getCode() {
		return code;
	}

	public void setCode(@Nullable Integer code) {
		this.code = code;
	}

	@Nullable
	public Integer getResourceId() {
		return resourceId;
	}

	public void setResourceId(@Nullable Integer resourceId) {
		this.resourceId = resourceId;
	}

	@Nullable
	public Map<String, Object> getPayload() {
		return payload;
	}

	public void setPayload(@Nullable Map<String, Object> payload) {
		this.payload = payload;
	}

	@Nullable
	public List<String> getErrors() {
		return errors;
	}

	public void setErrors(@Nullable List<String> errors) {
		this.errors = errors;
	}

	@Nullable
	public List<FieldError> getFieldErrors() {
		return fieldErrors;
	}

	public void setFieldErrors(@Nullable List<FieldError> fieldErrors) {
		this.fieldErrors = fieldErrors;
	}
}
