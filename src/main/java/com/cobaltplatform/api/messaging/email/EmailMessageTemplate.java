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

package com.cobaltplatform.api.messaging.email;

/**
 * @author Transmogrify, LLC.
 */
public enum EmailMessageTemplate {
	FREEFORM,
	ACCOUNT_VERIFICATION,
	ACCOUNT_EMAIL_VERIFICATION,
	ADMIN_CMS_CONTENT_ADDED,
	ADMIN_GROUP_SESSION_ADDED,
	APPOINTMENT_CANCELED_PATIENT,
	APPOINTMENT_CANCELED_PROVIDER,
	APPOINTMENT_CREATED_PATIENT,
	APPOINTMENT_CREATED_PROVIDER,
	APPOINTMENT_UPDATE,
	GROUP_REQUEST_SUBMITTED,
	GROUP_SESSION_CANCELED,
	GROUP_SESSION_RESERVATION_CANCELED_ATTENDEE,
	GROUP_SESSION_RESERVATION_CANCELED_FACILITATOR,
	GROUP_SESSION_RESERVATION_CREATED_ATTENDEE,
	GROUP_SESSION_RESERVATION_CREATED_FACILITATOR,
	GROUP_SESSION_RESERVATION_FOLLOWUP_ATTENDEE,
	GROUP_SESSION_RESPONSE_CREATED_FACILITATOR,
	GROUP_SESSION_LIVE_SUBMITTER,
	GROUP_SESSION_REQUEST_LIVE_SUBMITTER,
	MULTIPLE_EPIC_MATCHES,
	PASSWORD_RESET,
	PROVIDER_ASSESSMENT_SCORES,
	SUICIDE_RISK,
	USER_FEEDBACK,
	INTERACTION_REMINDER,
	APPOINTMENT_INTERACTION,
	APPOINTMENT_REMINDER_PATIENT,
	GROUP_SESSION_RESERVATION_REMINDER_ATTENDEE,
	IC_WELCOME,
	IC_WELCOME_REMINDER,
	IC_RESOURCE_CHECK_IN,
	IC_APPOINTMENT_BOOKING_REMINDER,
	MARKETING_SITE_OUTREACH
}
