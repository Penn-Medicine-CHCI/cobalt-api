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

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import static java.lang.String.format;

/**
 * @author Transmogrify LLC.
 */
@NotThreadSafe
public class ReportType {
	@Nullable
	private ReportTypeId reportTypeId;
	@Nullable
	private String description;
	@Nullable
	private Integer displayOrder;

	public enum ReportTypeId {
		PROVIDER_UNUSED_AVAILABILITY,
		PROVIDER_APPOINTMENTS,
		PROVIDER_APPOINTMENT_CANCELATIONS,
		PROVIDER_APPOINTMENTS_EAP, // Special EAP-only report
		IC_PIPELINE,
		IC_OUTREACH,
		IC_ASSESSMENT,
		IC_SAFETY_PLANNING,
		GROUP_SESSION_RESERVATION_EMAILS,
		ADMIN_ANALYTICS_VISITS,
		ADMIN_ANALYTICS_USERS,
		ADMIN_ANALYTICS_EMPLOYERS,
		ADMIN_ANALYTICS_PAGEVIEWS,
		ADMIN_ANALYTICS_USER_REFERRALS,
		ADMIN_ANALYTICS_REFERRING_DOMAINS,
		ADMIN_ANALYTICS_CLINICAL_ASSESSMENT_COMPLETION,
		ADMIN_ANALYTICS_CLINICAL_ASSESSMENT_SEVERITY,
		ADMIN_ANALYTICS_CRISIS_TRIGGERS,
		ADMIN_ANALYTICS_APPOINTMENTS_BOOKABLE,
		ADMIN_ANALYTICS_APPOINTMENTS_CLICK_TO_CALL,
		ADMIN_ANALYTICS_GROUP_SESSION_REGISTRATIONS,
		ADMIN_ANALYTICS_GROUP_SESSION_REQUESTS,
		ADMIN_ANALYTICS_GROUP_SESSIONS,
		ADMIN_ANALYTICS_RESOURCE_TOPIC_PAGEVIEWS,
		ADMIN_ANALYTICS_RESOURCE_PAGEVIEWS,
		ADMIN_ANALYTICS_TOPIC_CENTER_OVERVIEW
	}

	@Override
	public String toString() {
		return format("%s{reportTypeId=%s, description=%s, displayOrder=%s}", getClass().getSimpleName(), getReportTypeId(),
				getDescription(), getDisplayOrder());
	}

	@Nullable
	public ReportTypeId getReportTypeId() {
		return this.reportTypeId;
	}

	public void setReportTypeId(@Nullable ReportTypeId reportTypeId) {
		this.reportTypeId = reportTypeId;
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
		return this.displayOrder;
	}

	public void setDisplayOrder(@Nullable Integer displayOrder) {
		this.displayOrder = displayOrder;
	}
}