BEGIN;
SELECT _v.register_patch('129-beiwe', NULL, NULL);

CREATE TABLE public_key_format (
  public_key_format_id TEXT NOT NULL PRIMARY KEY,
  description TEXT NOT NULL
);

INSERT INTO public_key_format VALUES ('X509', 'X.509');

CREATE TABLE private_key_format (
  private_key_format_id TEXT NOT NULL PRIMARY KEY,
  description TEXT NOT NULL
);

INSERT INTO private_key_format VALUES ('PKCS_8', 'PKCS#8');

CREATE TABLE encryption_keypair (
  encryption_keypair_id UUID NOT NULL PRIMARY KEY,
  public_key TEXT NOT NULL, -- Base64 encoded
  private_key TEXT NOT NULL, -- Base64 encoded
  public_key_format_id TEXT NOT NULL REFERENCES public_key_format,
  private_key_format_id TEXT NOT NULL REFERENCES private_key_format,
  created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON encryption_keypair FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

ALTER TABLE account_study ADD COLUMN encryption_keypair_id UUID NOT NULL REFERENCES encryption_keypair;

-- See these Beiwe files:
-- https://github.com/onnela-lab/beiwe-backend/blob/main/database/study_models.py
-- https://github.com/onnela-lab/beiwe-backend/blob/main/constants/study_constants.py

CREATE TABLE study_beiwe_config (
  study_beiwe_config_id UUID NOT NULL PRIMARY KEY,
  study_id UUID NOT NULL REFERENCES study,

  -- ** BEIWE START **

  -- Whether various device options are turned on
  accelerometer BOOLEAN NOT NULL DEFAULT TRUE,
  gps BOOLEAN NOT NULL DEFAULT TRUE,
  calls BOOLEAN NOT NULL DEFAULT TRUE,
  texts BOOLEAN NOT NULL DEFAULT TRUE,
  wifi BOOLEAN NOT NULL DEFAULT TRUE,
  bluetooth BOOLEAN NOT NULL DEFAULT FALSE,
  power_state BOOLEAN NOT NULL DEFAULT TRUE,
  use_anonymized_hashing BOOLEAN NOT NULL DEFAULT TRUE,
  use_gps_fuzzing BOOLEAN NOT NULL DEFAULT FALSE,
  call_clinician_button_enabled BOOLEAN NOT NULL DEFAULT TRUE,
  call_research_assistant_button_enabled BOOLEAN NOT NULL DEFAULT TRUE,
  ambient_audio BOOLEAN NOT NULL DEFAULT FALSE,

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
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON study_beiwe_config FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

COMMIT;