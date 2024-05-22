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

package com.cobaltplatform.api.model.service;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * @author Transmogrify LLC.
 */
@NotThreadSafe
public enum PatientOrderContactTypeId {
	// Welcome message (SMS/email) never delivered
	WELCOME_MESSAGE,
	// There has been some form of outreach but no screening session scheduled or started after X days.
	// This is more like an "overdue" indicator, no date/time associated
	ASSESSMENT_OUTREACH,
	// Scheduled assessment
	ASSESSMENT,
	// Scheduled outreach of type 'phone call'
	RESOURCE_FOLLOWUP,
	// If there is an SMS/email scheduled to be sent (but not yet delivered)
	RESOURCE_CHECK_IN,
	// (not live yet) if patient has not yet responded after X days to resource check-in text OR said they are not interested
	// This is more like an "overdue" indicator, no date/time associated
	RESOURCE_CHECK_IN_FOLLOWUP,
	// Scheduled outreach of type 'other'
	OTHER
}