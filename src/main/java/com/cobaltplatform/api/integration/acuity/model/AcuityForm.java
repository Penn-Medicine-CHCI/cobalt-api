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

package com.cobaltplatform.api.integration.acuity.model;

import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.List;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class AcuityForm {
	@Nullable
	private Long id;
	@Nullable
	private String name;
	@Nullable
	private String description;
	@Nullable
	private Boolean hidden;
	@Nullable
	@SerializedName("appointmentTypeIDs")
	private List<Long> appointmentTypeIds;
	@Nullable
	private List<AcuityFormField> fields;

	@NotThreadSafe
	public static class AcuityFormField {
		@Nullable
		private Long id;
		@Nullable
		private String name;
		@Nullable
		private Boolean required;
		@Nullable
		private String type;

		@Override
		public String toString() {
			return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
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
		public Boolean getRequired() {
			return required;
		}

		public void setRequired(@Nullable Boolean required) {
			this.required = required;
		}

		@Nullable
		public String getType() {
			return type;
		}

		public void setType(@Nullable String type) {
			this.type = type;
		}
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
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
	public String getDescription() {
		return description;
	}

	public void setDescription(@Nullable String description) {
		this.description = description;
	}

	@Nullable
	public Boolean getHidden() {
		return hidden;
	}

	public void setHidden(@Nullable Boolean hidden) {
		this.hidden = hidden;
	}

	@Nullable
	public List<Long> getAppointmentTypeIds() {
		return appointmentTypeIds;
	}

	public void setAppointmentTypeIds(@Nullable List<Long> appointmentTypeIds) {
		this.appointmentTypeIds = appointmentTypeIds;
	}

	@Nullable
	public List<AcuityFormField> getFields() {
		return fields;
	}

	public void setFields(@Nullable List<AcuityFormField> fields) {
		this.fields = fields;
	}
}
