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

import com.google.gson.annotations.SerializedName;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.Map;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class Event extends Way2HealthEntity {
	@Nullable
	private Long id;
	@Nullable
	private String name;
	@Nullable
	private String type;
	@Nullable
	private Source source;

	@NotThreadSafe
	public static class Source extends Way2HealthEntity {
		@Nullable
		private Long id;
		@Nullable
		private String name;
		@Nullable
		@SerializedName("class")
		private String sourceClass;
		@Nullable
		private String type;
		@Nullable
		private Map<String, Object> params;

		@Nullable
		public Long getId() {
			return id;
		}

		public void setId(@Nullable Long id) {
			this.id = id;
		}

		@Nullable
		public String getName() {
			return name;
		}

		public void setName(@Nullable String name) {
			this.name = name;
		}

		@Nullable
		public String getSourceClass() {
			return sourceClass;
		}

		public void setSourceClass(@Nullable String sourceClass) {
			this.sourceClass = sourceClass;
		}

		@Nullable
		public String getType() {
			return type;
		}

		public void setType(@Nullable String type) {
			this.type = type;
		}

		@Nullable
		public Map<String, Object> getParams() {
			return params;
		}

		public void setParams(@Nullable Map<String, Object> params) {
			this.params = params;
		}
	}

	@Nullable
	public Long getId() {
		return id;
	}

	public void setId(@Nullable Long id) {
		this.id = id;
	}

	@Nullable
	public String getName() {
		return name;
	}

	public void setName(@Nullable String name) {
		this.name = name;
	}

	@Nullable
	public String getType() {
		return type;
	}

	public void setType(@Nullable String type) {
		this.type = type;
	}

	@Nullable
	public Source getSource() {
		return source;
	}

	public void setSource(@Nullable Source source) {
		this.source = source;
	}
}
