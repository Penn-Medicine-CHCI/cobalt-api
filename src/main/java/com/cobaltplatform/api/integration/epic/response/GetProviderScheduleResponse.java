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

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.List;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class GetProviderScheduleResponse {
	@Nullable
	private String rawJson;

	@Nullable
	private String Error;
	@Nullable
	private String ProviderName;
	@Nullable
	private String DepartmentName;
	@Nullable
	private String Date;
	@Nullable
	private String UnavailableDayReason;
	@Nullable
	private String UnavailableDayComment;
	@Nullable
	private List<ScheduleSlot> ScheduleSlots;
	@Nullable
	private List<ProviderMessage> ProviderMessages;
	@Nullable
	private List<ProviderID> ProviderIDs;
	@Nullable
	private List<DepartmentID> DepartmentIDs;

	@NotThreadSafe
	public static class ScheduleSlot {
		@Nullable
		private String StartTime; // " 8:00 AM"
		@Nullable
		private String Length; // "10"
		@Nullable
		private String AvailableOpenings; // "1"
		@Nullable
		private String OriginalOpenings; // "1"
		@Nullable
		private String OverbookOpenings; // "0"
		@Nullable
		private Boolean Public;
		@Nullable
		private String SlotColor; // "00FF00"
		@Nullable
		private String UnavailableTimeReason;
		@Nullable
		private String UnavailableTimeComment;
		@Nullable
		private String HeldTimeReason;
		@Nullable
		private String HeldTimeComment;
		@Nullable
		private Boolean HeldTimeAllDay;

		@Override
		public String toString() {
			return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
		}

		@Nullable
		public String getStartTime() {
			return StartTime;
		}

		public void setStartTime(@Nullable String startTime) {
			StartTime = startTime;
		}

		@Nullable
		public String getLength() {
			return Length;
		}

		public void setLength(@Nullable String length) {
			Length = length;
		}

		@Nullable
		public String getAvailableOpenings() {
			return AvailableOpenings;
		}

		public void setAvailableOpenings(@Nullable String availableOpenings) {
			AvailableOpenings = availableOpenings;
		}

		@Nullable
		public String getOriginalOpenings() {
			return OriginalOpenings;
		}

		public void setOriginalOpenings(@Nullable String originalOpenings) {
			OriginalOpenings = originalOpenings;
		}

		@Nullable
		public String getOverbookOpenings() {
			return OverbookOpenings;
		}

		public void setOverbookOpenings(@Nullable String overbookOpenings) {
			OverbookOpenings = overbookOpenings;
		}

		@Nullable
		public Boolean getPublic() {
			return Public;
		}

		public void setPublic(@Nullable Boolean aPublic) {
			Public = aPublic;
		}

		@Nullable
		public String getSlotColor() {
			return SlotColor;
		}

		public void setSlotColor(@Nullable String slotColor) {
			SlotColor = slotColor;
		}

		@Nullable
		public String getUnavailableTimeReason() {
			return UnavailableTimeReason;
		}

		public void setUnavailableTimeReason(@Nullable String unavailableTimeReason) {
			UnavailableTimeReason = unavailableTimeReason;
		}

		@Nullable
		public String getUnavailableTimeComment() {
			return UnavailableTimeComment;
		}

		public void setUnavailableTimeComment(@Nullable String unavailableTimeComment) {
			UnavailableTimeComment = unavailableTimeComment;
		}

		@Nullable
		public String getHeldTimeReason() {
			return HeldTimeReason;
		}

		public void setHeldTimeReason(@Nullable String heldTimeReason) {
			HeldTimeReason = heldTimeReason;
		}

		@Nullable
		public String getHeldTimeComment() {
			return HeldTimeComment;
		}

		public void setHeldTimeComment(@Nullable String heldTimeComment) {
			HeldTimeComment = heldTimeComment;
		}

		@Nullable
		public Boolean getHeldTimeAllDay() {
			return HeldTimeAllDay;
		}

		public void setHeldTimeAllDay(@Nullable Boolean heldTimeAllDay) {
			HeldTimeAllDay = heldTimeAllDay;
		}
	}

	@NotThreadSafe
	public static class ProviderMessage {
		@Nullable
		private String Time;
		@Nullable
		private String Text;

		@Nullable
		public String getTime() {
			return Time;
		}

		public void setTime(@Nullable String time) {
			Time = time;
		}

		@Nullable
		public String getText() {
			return Text;
		}

		public void setText(@Nullable String text) {
			Text = text;
		}
	}

	@NotThreadSafe
	public static class ProviderID {
		@Nullable
		private String ID;
		@Nullable
		private String Type;

		@Nullable
		public String getID() {
			return ID;
		}

		public void setID(@Nullable String ID) {
			this.ID = ID;
		}

		@Nullable
		public String getType() {
			return Type;
		}

		public void setType(@Nullable String type) {
			Type = type;
		}
	}

	@NotThreadSafe
	public static class DepartmentID {
		@Nullable
		private String ID;
		@Nullable
		private String Type;

		@Nullable
		public String getID() {
			return ID;
		}

		public void setID(@Nullable String ID) {
			this.ID = ID;
		}

		@Nullable
		public String getType() {
			return Type;
		}

		public void setType(@Nullable String type) {
			Type = type;
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
	public String getError() {
		return Error;
	}

	public void setError(@Nullable String error) {
		Error = error;
	}

	@Nullable
	public List<ScheduleSlot> getScheduleSlots() {
		return ScheduleSlots;
	}

	public void setScheduleSlots(@Nullable List<ScheduleSlot> scheduleSlots) {
		ScheduleSlots = scheduleSlots;
	}

	@Nullable
	public List<ProviderMessage> getProviderMessages() {
		return ProviderMessages;
	}

	public void setProviderMessages(@Nullable List<ProviderMessage> providerMessages) {
		ProviderMessages = providerMessages;
	}

	@Nullable
	public List<ProviderID> getProviderIDs() {
		return ProviderIDs;
	}

	public void setProviderIDs(@Nullable List<ProviderID> providerIDs) {
		ProviderIDs = providerIDs;
	}

	@Nullable
	public List<DepartmentID> getDepartmentIDs() {
		return DepartmentIDs;
	}

	public void setDepartmentIDs(@Nullable List<DepartmentID> departmentIDs) {
		DepartmentIDs = departmentIDs;
	}

	@Nullable
	public String getProviderName() {
		return ProviderName;
	}

	public void setProviderName(@Nullable String providerName) {
		ProviderName = providerName;
	}

	@Nullable
	public String getDepartmentName() {
		return DepartmentName;
	}

	public void setDepartmentName(@Nullable String departmentName) {
		DepartmentName = departmentName;
	}

	@Nullable
	public String getDate() {
		return Date;
	}

	public void setDate(@Nullable String date) {
		Date = date;
	}

	@Nullable
	public String getUnavailableDayReason() {
		return UnavailableDayReason;
	}

	public void setUnavailableDayReason(@Nullable String unavailableDayReason) {
		UnavailableDayReason = unavailableDayReason;
	}

	@Nullable
	public String getUnavailableDayComment() {
		return UnavailableDayComment;
	}

	public void setUnavailableDayComment(@Nullable String unavailableDayComment) {
		UnavailableDayComment = unavailableDayComment;
	}
}
