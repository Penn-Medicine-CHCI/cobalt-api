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

package com.cobaltplatform.api.integration.epic.response;

import com.cobaltplatform.api.integration.epic.shared.Link;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.List;
import java.util.Map;

/**
 * FHIR Coverage.Search (R4) bundle.
 * <p>
 * TODO: flesh out strongly-typed Coverage resource fields as payload examples are captured.
 *
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class CoverageSearchFhirR4Response {
	@Nullable
	private String rawJson;
	@Nullable
	private String id;
	@Nullable
	private String resourceType;
	@Nullable
	private String type;
	@Nullable
	private Integer total;
	@Nullable
	private List<Link> link;
	@Nullable
	private List<Entry> entry;

	@NotThreadSafe
	public static class Entry {
		@Nullable
		private String fullUrl;
		@Nullable
		private Map<String, Object> resource;
		@Nullable
		private Search search;

		@NotThreadSafe
		public static class Search {
			@Nullable
			private String mode;

			@Nullable
			public String getMode() {
				return this.mode;
			}

			public void setMode(@Nullable String mode) {
				this.mode = mode;
			}
		}

		@Nullable
		public String getFullUrl() {
			return this.fullUrl;
		}

		public void setFullUrl(@Nullable String fullUrl) {
			this.fullUrl = fullUrl;
		}

		@Nullable
		public Map<String, Object> getResource() {
			return this.resource;
		}

		public void setResource(@Nullable Map<String, Object> resource) {
			this.resource = resource;
		}

		@Nullable
		public Search getSearch() {
			return this.search;
		}

		public void setSearch(@Nullable Search search) {
			this.search = search;
		}
	}

	@Nullable
	public String getRawJson() {
		return this.rawJson;
	}

	public void setRawJson(@Nullable String rawJson) {
		this.rawJson = rawJson;
	}

	@Nullable
	public String getId() {
		return this.id;
	}

	public void setId(@Nullable String id) {
		this.id = id;
	}

	@Nullable
	public String getResourceType() {
		return this.resourceType;
	}

	public void setResourceType(@Nullable String resourceType) {
		this.resourceType = resourceType;
	}

	@Nullable
	public String getType() {
		return this.type;
	}

	public void setType(@Nullable String type) {
		this.type = type;
	}

	@Nullable
	public Integer getTotal() {
		return this.total;
	}

	public void setTotal(@Nullable Integer total) {
		this.total = total;
	}

	@Nullable
	public List<Link> getLink() {
		return this.link;
	}

	public void setLink(@Nullable List<Link> link) {
		this.link = link;
	}

	@Nullable
	public List<Entry> getEntry() {
		return this.entry;
	}

	public void setEntry(@Nullable List<Entry> entry) {
		this.entry = entry;
	}
}
