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
	ASSESSMENT_OUTREACH, // TODO: this means that if the welcome message has been sent but no screening session has been started, this should be "do it asap"
	ASSESSMENT, // patient_order_scheduled_screening scheduled and has not been started
	RESOURCE_FOLLOWUP, // patient_order_scheduled_outreach with reason RESOURCE_FOLLOWUP
	RESOURCE_CHECK_IN, // patient_order_scheduled_message_group with patient_order_scheduled_message_type_id of RESOURCE_CHECK_IN
	OTHER // patient_order_scheduled_outreach with reason OTHER
}