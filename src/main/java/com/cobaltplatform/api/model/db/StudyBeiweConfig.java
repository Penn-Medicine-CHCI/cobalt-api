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
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class StudyBeiweConfig {
	@Nullable
	private UUID studyBeiweConfigId;
	@Nullable
	private UUID studyId;

	// Whether various device options are turned on

	@Nullable
	private Boolean accelerometer;
	@Nullable
	private Boolean gps;
	@Nullable
	private Boolean calls;
	@Nullable
	private Boolean texts;
	@Nullable
	private Boolean wifi;
	@Nullable
	private Boolean bluetooth;
	@Nullable
	private Boolean powerState;
	@Nullable
	private Boolean useAnonymizedHashing;
	@Nullable
	private Boolean useGpsFuzzing;
	@Nullable
	private Boolean callClinicianButtonEnabled;
	@Nullable
	private Boolean callResearchAssistantButtonEnabled;
	@Nullable
	private Boolean ambientAudio;

	// Whether iOS-specific data streams are turned on


	/*



  -- Whether iOS-specific data streams are turned on
  proximity BOOLEAN NOT NULL DEFAULT FALSE,
  gyro BOOLEAN NOT NULL DEFAULT FALSE, -- not ios-specific anymore
  magnetometer BOOLEAN NOT NULL DEFAULT FALSE, -- not ios-specific anymore
  devicemotion BOOLEAN NOT NULL DEFAULT FALSE,
  reachability BOOLEAN NOT NULL DEFAULT TRUE,

  -- Upload over cellular data or only over WiFi (WiFi-only is default)
  allow_upload_over_cellular_data BOOLEAN NOT NULL DEFAULT FALSE,

  -- Timer variables
  accelerometer_off_duration_seconds INTEGER NOT NULL DEFAULT 10, -- min 1
  accelerometer_on_duration_seconds INTEGER NOT NULL DEFAULT 10, -- min 1
  accelerometer_frequency INTEGER NOT NULL DEFAULT 10, -- min 1
  ambient_audio_off_duration_seconds INTEGER NOT NULL DEFAULT 600, -- min 1
  ambient_audio_on_duration_seconds INTEGER NOT NULL DEFAULT 600, -- min 1
  ambient_audio_bitrate INTEGER NOT NULL DEFAULT 24000, -- min 16000
  ambient_audio_sampling_rate INTEGER NOT NULL DEFAULT 44100, -- min 16000
  bluetooth_on_duration_seconds INTEGER NOT NULL DEFAULT 60, -- min 1
  bluetooth_total_duration_seconds INTEGER NOT NULL DEFAULT 300, -- min 1
  bluetooth_global_offset_seconds INTEGER NOT NULL DEFAULT 0,
  check_for_new_surveys_frequency_seconds INTEGER NOT NULL DEFAULT 3600, -- min 30
  create_new_data_files_frequency_seconds INTEGER NOT NULL DEFAULT 900, -- min 30
  gps_off_duration_seconds INTEGER NOT NULL DEFAULT 600, -- min 1
  gps_on_duration_seconds INTEGER NOT NULL DEFAULT 60, -- min 1
  seconds_before_auto_logout INTEGER NOT NULL DEFAULT 600, -- min 1
  upload_data_files_frequency_seconds INTEGER NOT NULL DEFAULT 3600, -- min 10
  voice_recording_max_time_length_seconds INTEGER NOT NULL DEFAULT 240,
  wifi_log_frequency_seconds INTEGER NOT NULL DEFAULT 300, -- min 10
  gyro_off_duration_seconds INTEGER NOT NULL DEFAULT 600, -- min 1
  gyro_on_duration_seconds INTEGER NOT NULL DEFAULT 60, -- min 1
  gyro_frequency INTEGER NOT NULL DEFAULT 10, -- min 1

  -- iOS-specific timer variables
  magnetometer_off_duration_seconds INTEGER NOT NULL DEFAULT 600, -- min 1
  magnetometer_on_duration_seconds INTEGER NOT NULL DEFAULT 60, -- min 1
  devicemotion_off_duration_seconds INTEGER NOT NULL DEFAULT 600, -- min 1
  devicemotion_on_duration_seconds INTEGER NOT NULL DEFAULT 60, -- min 1

  -- Text strings
  about_page_text TEXT NOT NULL DEFAULT 'Placeholder About Page text',
  call_clinician_button_text TEXT NOT NULL DEFAULT 'Call My Clinician',
  consent_form_text TEXT NOT NULL DEFAULT 'I have read and understood the information about the study and all of my questions about the study have been answered by the study researchers.',
  survey_submit_success_toast_text TEXT NOT NULL DEFAULT 'Thank you for completing the survey.',

  -- Consent sections
  consent_sections JSONB NOT NULL DEFAULT '
{
    "welcome": {
        "text": "",
        "more": ""
    },
    "data_gathering": {
        "text": "",
        "more": ""
    },
    "privacy": {
        "text": "",
        "more": ""
    },
    "data_use": {
        "text": "",
        "more": ""
    },
    "time_commitment": {
        "text": "",
        "more": ""
    },
    "study_survey": {
        "text": "",
        "more": ""
    },
    "study_tasks": {
        "text": "",
        "more": ""
    },
    "withdrawing": {
        "text": "",
        "more": ""
    }
}
'::JSONB,

  -- ** BEIWE END **

	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()

	 */

}