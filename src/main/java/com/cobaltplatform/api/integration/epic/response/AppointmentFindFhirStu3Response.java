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

import com.cobaltplatform.api.integration.epic.code.AppointmentParticipantStatusCode;
import com.cobaltplatform.api.integration.epic.code.AppointmentStatusCode;
import com.cobaltplatform.api.integration.epic.code.SlotStatusCode;
import com.cobaltplatform.api.integration.epic.shared.Link;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class AppointmentFindFhirStu3Response {
	@Nonnull
	private static final Gson GSON;

	static {
		GSON = new GsonBuilder().disableHtmlEscaping().create();
	}

	@Nonnull
	public String serialize() {
		return GSON.toJson(this);
	}

	@Nonnull
	public static AppointmentFindFhirStu3Response deserialize(@Nonnull String serialized) {
		requireNonNull(serialized);
		return GSON.fromJson(serialized, AppointmentFindFhirStu3Response.class);
	}

	@Nullable
	private String rawJson;

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
		private Resource resource;
		@Nullable
		private Search search;
		@Nullable
		private List<Link> link;

		@NotThreadSafe
		public static class Resource {
			@Nullable
			private String id;
			@Nullable
			private String resourceType;
			@Nullable
			private AppointmentStatusCode status;
			@Nullable
			private Instant start;
			@Nullable
			private Instant end;
			@Nullable
			private Integer minutesDuration;
			@Nullable
			private List<Contained> contained;
			@Nullable
			private List<ServiceType> serviceType;
			@Nullable
			private List<Slot> slot;
			@Nullable
			private List<Participant> participant;

			@NotThreadSafe
			public static class Contained {
				@Nullable
				private String id;
				@Nullable
				private String resourceType;
				@Nullable
				private Schedule schedule;
				@Nullable
				private SlotStatusCode status;
				@Nullable
				private Instant start;
				@Nullable
				private Instant end;

				@NotThreadSafe
				public static class Schedule {
					@Nullable
					private String reference;

					@Nullable
					public String getReference() {
						return this.reference;
					}

					public void setReference(@Nullable String reference) {
						this.reference = reference;
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
				public Schedule getSchedule() {
					return this.schedule;
				}

				public void setSchedule(@Nullable Schedule schedule) {
					this.schedule = schedule;
				}

				@Nullable
				public SlotStatusCode getStatus() {
					return this.status;
				}

				public void setStatus(@Nullable SlotStatusCode status) {
					this.status = status;
				}

				@Nullable
				public Instant getStart() {
					return this.start;
				}

				public void setStart(@Nullable Instant start) {
					this.start = start;
				}

				@Nullable
				public Instant getEnd() {
					return this.end;
				}

				public void setEnd(@Nullable Instant end) {
					this.end = end;
				}
			}

			@NotThreadSafe
			public static class ServiceType {
				@Nullable
				private List<Coding> coding;

				@NotThreadSafe
				public static class Coding {
					@Nullable
					private String system;
					@Nullable
					private String code;
					@Nullable
					private String display;

					@Nullable
					public String getSystem() {
						return this.system;
					}

					public void setSystem(@Nullable String system) {
						this.system = system;
					}

					@Nullable
					public String getCode() {
						return this.code;
					}

					public void setCode(@Nullable String code) {
						this.code = code;
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
				public List<Coding> getCoding() {
					return this.coding;
				}

				public void setCoding(@Nullable List<Coding> coding) {
					this.coding = coding;
				}
			}

			@NotThreadSafe
			public static class Slot {
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

			@NotThreadSafe
			public static class Participant {
				@Nullable
				private Actor actor;
				@Nullable
				private AppointmentParticipantStatusCode status;

				@NotThreadSafe
				public static class Actor {
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
				public Actor getActor() {
					return this.actor;
				}

				public void setActor(@Nullable Actor actor) {
					this.actor = actor;
				}

				@Nullable
				public AppointmentParticipantStatusCode getStatus() {
					return this.status;
				}

				public void setStatus(@Nullable AppointmentParticipantStatusCode status) {
					this.status = status;
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
			public AppointmentStatusCode getStatus() {
				return this.status;
			}

			public void setStatus(@Nullable AppointmentStatusCode status) {
				this.status = status;
			}

			@Nullable
			public Instant getStart() {
				return this.start;
			}

			public void setStart(@Nullable Instant start) {
				this.start = start;
			}

			@Nullable
			public Instant getEnd() {
				return this.end;
			}

			public void setEnd(@Nullable Instant end) {
				this.end = end;
			}

			@Nullable
			public Integer getMinutesDuration() {
				return this.minutesDuration;
			}

			public void setMinutesDuration(@Nullable Integer minutesDuration) {
				this.minutesDuration = minutesDuration;
			}

			@Nullable
			public List<Contained> getContained() {
				return this.contained;
			}

			public void setContained(@Nullable List<Contained> contained) {
				this.contained = contained;
			}

			@Nullable
			public List<ServiceType> getServiceType() {
				return this.serviceType;
			}

			public void setServiceType(@Nullable List<ServiceType> serviceType) {
				this.serviceType = serviceType;
			}

			@Nullable
			public List<Slot> getSlot() {
				return this.slot;
			}

			public void setSlot(@Nullable List<Slot> slot) {
				this.slot = slot;
			}

			@Nullable
			public List<Participant> getParticipant() {
				return this.participant;
			}

			public void setParticipant(@Nullable List<Participant> participant) {
				this.participant = participant;
			}
		}

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
		public Resource getResource() {
			return this.resource;
		}

		public void setResource(@Nullable Resource resource) {
			this.resource = resource;
		}

		@Nullable
		public Search getSearch() {
			return this.search;
		}

		public void setSearch(@Nullable Search search) {
			this.search = search;
		}

		@Nullable
		public List<Link> getLink() {
			return this.link;
		}

		public void setLink(@Nullable List<Link> link) {
			this.link = link;
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
