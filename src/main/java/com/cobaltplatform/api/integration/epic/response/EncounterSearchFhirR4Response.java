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
import com.google.gson.annotations.SerializedName;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.List;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class EncounterSearchFhirR4Response {
	@Nullable
	private String rawJson;

	@Nullable
	private String id;
	@Nullable
	private String resourceType;
	@Nullable
	private Integer total;
	@Nullable
	private List<Link> link;
	@Nullable
	private List<Entry> entry;

	@NotThreadSafe
	public static class Entry {
		@Nullable
		private List<Link> link;
		@Nullable
		private String fullUrl;
		@Nullable
		private Resource resource;

		@NotThreadSafe
		public static class Resource {
			@Nullable
			private String id;
			@Nullable
			private String resourceType;
			@Nullable
			private List<Identifier> identifier;
			@Nullable
			private String status;
			@Nullable
			@SerializedName("class")
			private Coding classValue;
			@Nullable
			private List<Type> type;
			@Nullable
			private Type serviceType;
			@Nullable
			private Subject subject;
			@Nullable
			private Period period;

			@Nullable
			public static class Period {
				@Nullable
				private String start; // might be 2022-04-29T14:50:00Z, might be 2022-04-29
				@Nullable
				private String end; // might be 2022-04-29T14:50:00Z, might be 2022-04-29

				@Nullable
				public String getStart() {
					return this.start;
				}

				public void setStart(@Nullable String start) {
					this.start = start;
				}

				@Nullable
				public String getEnd() {
					return this.end;
				}

				public void setEnd(@Nullable String end) {
					this.end = end;
				}
			}

			@Nullable
			public static class Subject {
				@Nullable
				private String reference;
				@Nullable
				private String display;

				@Nullable
				public String getReference() {
					return this.reference;
				}

				public void setReference(@Nullable String reference) {
					this.reference = reference;
				}

				@Nullable
				public String getDisplay() {
					return this.display;
				}

				public void setDisplay(@Nullable String display) {
					this.display = display;
				}
			}

			@Nullable
			public static class Type {
				@Nullable
				private List<Coding> coding;
				@Nullable
				private String text;

				@Nullable
				public List<Coding> getCoding() {
					return this.coding;
				}

				public void setCoding(@Nullable List<Coding> coding) {
					this.coding = coding;
				}

				@Nullable
				public String getText() {
					return this.text;
				}

				public void setText(@Nullable String text) {
					this.text = text;
				}
			}

			@Nullable
			public static class Coding {
				@Nullable
				private String code;
				@Nullable
				private String system;
				@Nullable
				private String display;

				@Nullable
				public String getCode() {
					return this.code;
				}

				public void setCode(@Nullable String code) {
					this.code = code;
				}

				@Nullable
				public String getSystem() {
					return this.system;
				}

				public void setSystem(@Nullable String system) {
					this.system = system;
				}

				@Nullable
				public String getDisplay() {
					return this.display;
				}

				public void setDisplay(@Nullable String display) {
					this.display = display;
				}
			}

			@NotThreadSafe
			public static class Identifier {
				@Nullable
				private String use;
				@Nullable
				private String system;
				@Nullable
				private String value;

				@Nullable
				public String getUse() {
					return this.use;
				}

				public void setUse(@Nullable String use) {
					this.use = use;
				}

				@Nullable
				public String getSystem() {
					return this.system;
				}

				public void setSystem(@Nullable String system) {
					this.system = system;
				}

				@Nullable
				public String getValue() {
					return this.value;
				}

				public void setValue(@Nullable String value) {
					this.value = value;
				}
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
			public List<Identifier> getIdentifier() {
				return this.identifier;
			}

			public void setIdentifier(@Nullable List<Identifier> identifier) {
				this.identifier = identifier;
			}

			@Nullable
			public String getStatus() {
				return this.status;
			}

			public void setStatus(@Nullable String status) {
				this.status = status;
			}

			@Nullable
			public Coding getClassValue() {
				return this.classValue;
			}

			public void setClassValue(@Nullable Coding classValue) {
				this.classValue = classValue;
			}

			@Nullable
			public List<Type> getType() {
				return this.type;
			}

			public void setType(@Nullable List<Type> type) {
				this.type = type;
			}

			@Nullable
			public Type getServiceType() {
				return this.serviceType;
			}

			public void setServiceType(@Nullable Type serviceType) {
				this.serviceType = serviceType;
			}

			@Nullable
			public Subject getSubject() {
				return this.subject;
			}

			public void setSubject(@Nullable Subject subject) {
				this.subject = subject;
			}

			@Nullable
			public Period getPeriod() {
				return this.period;
			}

			public void setPeriod(@Nullable Period period) {
				this.period = period;
			}
		}

		@Nullable
		public List<Link> getLink() {
			return this.link;
		}

		public void setLink(@Nullable List<Link> link) {
			this.link = link;
		}

		@Nullable
		public String getFullUrl() {
			return this.fullUrl;
		}

		public void setFullUrl(@Nullable String fullUrl) {
			this.fullUrl = fullUrl;
		}

		@Nullable
		public Resource getResource() {
			return this.resource;
		}

		public void setResource(@Nullable Resource resource) {
			this.resource = resource;
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