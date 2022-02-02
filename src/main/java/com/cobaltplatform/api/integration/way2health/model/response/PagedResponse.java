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
public class PagedResponse<T extends Way2HealthEntity> {
	@Nullable
	private List<T> data;
	@Nullable
	private Meta meta;
	@Nullable
	private String rawResponseBody;

	@Override
	public String toString() {
		return Way2HealthGsonSupport.sharedGson().toJson(this);
	}

	@NotThreadSafe
	public static class Meta extends Way2HealthEntity {
		@Nullable
		private Pagination pagination;
		@Nullable
		private List<String> ignoredFilters;
		@Nullable
		private List<String> skippedFilters;

		@Override
		public String toString() {
			return Way2HealthGsonSupport.sharedGson().toJson(this);
		}

		@NotThreadSafe
		public static class Pagination extends Way2HealthEntity {
			@Nullable
			private Integer total;
			@Nullable
			private Integer count;
			@Nullable
			private Integer perPage;
			@Nullable
			private Integer currentPage;
			@Nullable
			private Integer totalPages;
			@Nullable
			private Links links; // Way2Health returns this as either an object or a list; we parse it specially

			@Override
			public String toString() {
				return Way2HealthGsonSupport.sharedGson().toJson(this);
			}

			@NotThreadSafe
			public static class Links extends Way2HealthEntity {
				@Nullable
				private String previous;
				@Nullable
				private String next;
				@Nullable
				private String baseRequest;

				@Override
				public String toString() {
					return Way2HealthGsonSupport.sharedGson().toJson(this);
				}

				@Nullable
				public String getPrevious() {
					return previous;
				}

				public void setPrevious(@Nullable String previous) {
					this.previous = previous;
				}

				@Nullable
				public String getNext() {
					return next;
				}

				public void setNext(@Nullable String next) {
					this.next = next;
				}

				@Nullable
				public String getBaseRequest() {
					return baseRequest;
				}

				public void setBaseRequest(@Nullable String baseRequest) {
					this.baseRequest = baseRequest;
				}
			}

			@Nullable
			public Integer getTotal() {
				return total;
			}

			public void setTotal(@Nullable Integer total) {
				this.total = total;
			}

			@Nullable
			public Integer getCount() {
				return count;
			}

			public void setCount(@Nullable Integer count) {
				this.count = count;
			}

			@Nullable
			public Integer getPerPage() {
				return perPage;
			}

			public void setPerPage(@Nullable Integer perPage) {
				this.perPage = perPage;
			}

			@Nullable
			public Integer getCurrentPage() {
				return currentPage;
			}

			public void setCurrentPage(@Nullable Integer currentPage) {
				this.currentPage = currentPage;
			}

			@Nullable
			public Integer getTotalPages() {
				return totalPages;
			}

			public void setTotalPages(@Nullable Integer totalPages) {
				this.totalPages = totalPages;
			}

			@Nullable
			public Links getLinks() {
				return links;
			}

			public void setLinks(@Nullable Links links) {
				this.links = links;
			}
		}

		@Nullable
		public Pagination getPagination() {
			return pagination;
		}

		public void setPagination(@Nullable Pagination pagination) {
			this.pagination = pagination;
		}

		@Nullable
		public List<String> getIgnoredFilters() {
			return ignoredFilters;
		}

		public void setIgnoredFilters(@Nullable List<String> ignoredFilters) {
			this.ignoredFilters = ignoredFilters;
		}

		@Nullable
		public List<String> getSkippedFilters() {
			return skippedFilters;
		}

		public void setSkippedFilters(@Nullable List<String> skippedFilters) {
			this.skippedFilters = skippedFilters;
		}
	}

	@Nullable
	public List<T> getData() {
		return data;
	}

	public void setData(@Nullable List<T> data) {
		this.data = data;
	}

	@Nullable
	public Meta getMeta() {
		return meta;
	}

	public void setMeta(@Nullable Meta meta) {
		this.meta = meta;
	}

	@Nullable
	public String getRawResponseBody() {
		return rawResponseBody;
	}

	public void setRawResponseBody(@Nullable String rawResponseBody) {
		this.rawResponseBody = rawResponseBody;
	}
}
