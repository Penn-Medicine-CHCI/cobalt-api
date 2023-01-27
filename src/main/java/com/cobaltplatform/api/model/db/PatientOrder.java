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
import java.time.Instant;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class PatientOrder {
	@Nullable
	private UUID patientOrderId;
	@Nullable
	private UUID orderImportId;
	@Nullable
	private UUID accountId;
	@Nullable
	private String encounterDepartmentId;
	@Nullable
	private String encounterDepartmentName;

	// TODO: finish
	
	//  referring_practice_id VARCHAR, -- not currently provided
	//  referring_practice_id_type VARCHAR, -- not currently provided
	//  referring_practice_name VARCHAR,
	//  ordering_provider_id VARCHAR, -- not currently provided
	//  ordering_provider_id_type VARCHAR, -- not currently provided
	//  ordering_provider_name VARCHAR,
	//  billing_provider_id VARCHAR, -- not currently provided
	//  billing_provider_id_type VARCHAR, -- not currently provided
	//  billing_provider_name VARCHAR,
	//  patient_last_name VARCHAR NOT NULL,
	//  patient_first_name VARCHAR NOT NULL,
	//  patient_mrn VARCHAR NOT NULL,
	//  patient_uid VARCHAR NOT NULL,
	//  patient_sex VARCHAR,
	//  patient_birthdate DATE,
	//  patient_address_line_1 VARCHAR,
	//  patient_address_line_2 VARCHAR,
	//  patient_city VARCHAR,
	//  patient_postal_code VARCHAR,
	//  patient_region VARCHAR, -- In the US, this is the city
	//  patient_country_code VARCHAR,
	//  primary_payor VARCHAR,
	//  primary_plan VARCHAR,
	//  order_date DATE,
	//  order_age_in_minutes INTEGER,
	//  order_id VARCHAR NOT NULL,
	//  routing VARCHAR,
	//  reason_for_referral VARCHAR,
	//  diagnosis VARCHAR,
	//  associated_diagnosis VARCHAR,
	//  callback_phone_number VARCHAR,
	//  preferred_contact_hours VARCHAR,
	//  comments VARCHAR,
	//  img_cc_recipients VARCHAR,
	//  last_active_medication_order_summary VARCHAR,
	//  medications VARCHAR,
	//  recent_psychotherapeutic_medications VARCHAR,

	@Nullable
	private Instant created;
	@Nullable
	private Instant lastUpdated;
}
