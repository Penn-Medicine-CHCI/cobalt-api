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

package com.cobaltplatform.api.integration.way2health.model.response;

import com.cobaltplatform.api.integration.way2health.Way2HealthGsonSupport;
import com.cobaltplatform.api.integration.way2health.model.entity.Way2HealthEntity;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.List;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class ObjectResponse<T extends Way2HealthEntity> {
	@Nullable
	private T data;
	@Nullable
	private List<Error> errors;
	@Nullable
	private String rawResponseBody;

	@Override
	public String toString() {
		return Way2HealthGsonSupport.sharedGson().toJson(this);
	}

	@Nullable
	public T getData() {
		return data;
	}

	public void setData(@Nullable T data) {
		this.data = data;
	}

	@Nullable
	public List<Error> getErrors() {
		return errors;
	}

	public void setErrors(@Nullable List<Error> errors) {
		this.errors = errors;
	}

	@Nullable
	public String getRawResponseBody() {
		return rawResponseBody;
	}

	public void setRawResponseBody(@Nullable String rawResponseBody) {
		this.rawResponseBody = rawResponseBody;
	}
}
