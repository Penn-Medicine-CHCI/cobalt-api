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

package com.cobaltplatform.api.model.db;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.Locale;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify LLC.
 */
@NotThreadSafe
public class VisitType {
	@Nullable
	private VisitTypeId visitTypeId;
	@Nullable
	private String description;
	@Nullable
	private Integer displayOrder;

	public enum VisitTypeId {
		INITIAL,
		FOLLOWUP,
		OTHER;

		@Nonnull
		public static VisitTypeId fromAcuityName(@Nonnull String name) {
			requireNonNull(name);

			// TODO: this is a temporary hack to support Acuity since there is no good way to tag appointment types as NPV or RPV.
			// Once we move away from Acuity we can remove this.

			// Dev:
			// cobalt=> select distinct name from appointment_type where scheduling_system_id ='ACUITY' and appointment_type_id IN (select appointment_type_id from provider_appointment_type) order by name;
			//        name
			//--------------------
			// 30-Minute Followup
			// Initial Consult
			//(2 rows)

			// Prod:
			// cobalt=> select distinct name from appointment_type where scheduling_system_id ='ACUITY' and appointment_type_id IN (select appointment_type_id from provider_appointment_type) order by name;
			//                     name
			//----------------------------------------------
			// 1:1 Appointment with Psychotherapist
			// 1:1 Initial Appointment with Psychotherapist
			// 1:1 Psychiatrist Follow-ups
			// 1:1 Psych NP Follow-Up
			// 1:1 Session with Chaplain
			// 1:1 Session with Exercise Physiologist
			// 1:1 Session with Psychiatrist
			// 1:1 Session with Resilience Coach
			// 1:1 Strength and Training Specialist
			// 1:1 with Care Manager
			// 1:1 with Dietitian
			// 1:1 with Pain Specialist
			// 1:1 with Peer
			// 1:1 with Psychiatric Nurse Practitioner
			// CCT Intake Appointment
			// The Chaplain is In - for You
			//(16 rows)

			name = name.toLowerCase(Locale.US);

			if(name.contains("followup") || name.contains("follow-up"))
				return VisitTypeId.FOLLOWUP;

			if(name.contains("initial"))
				return VisitTypeId.INITIAL;

			return VisitTypeId.OTHER;
		}
	}

	@Override
	public String toString() {
		return format("%s{visitTypeId=%s, description=%s}", getClass().getSimpleName(), getVisitTypeId(), getDescription());
	}

	@Nullable
	public VisitTypeId getVisitTypeId() {
		return visitTypeId;
	}

	public void setVisitTypeId(@Nullable VisitTypeId visitTypeId) {
		this.visitTypeId = visitTypeId;
	}

	@Nullable
	public String getDescription() {
		return description;
	}

	public void setDescription(@Nullable String description) {
		this.description = description;
	}

	@Nullable
	public Integer getDisplayOrder() {
		return displayOrder;
	}

	public void setDisplayOrder(@Nullable Integer displayOrder) {
		this.displayOrder = displayOrder;
	}
}