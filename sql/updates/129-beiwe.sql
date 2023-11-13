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

INSERT INTO private_key_format VALUES ('PKCS8', 'PKCS#8');

CREATE TABLE encryption_keypair (
  encryption_keypair_id UUID NOT NULL PRIMARY KEY,
  public_key TEXT NOT NULL, -- Base64 encoded
  private_key TEXT NOT NULL, -- Base64 encoded
  public_key_format_id TEXT NOT NULL REFERENCES public_key_format,
  private_key_format_id TEXT NOT NULL REFERENCES private_key_format,
  key_size INTEGER NOT NULL,
  created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON encryption_keypair FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

-- Each account-study combination gets its own keypair.
ALTER TABLE account_study ADD COLUMN encryption_keypair_id UUID REFERENCES encryption_keypair;

-- Because current account_study are only throwaway testing data, it's OK to make a single shared hardcoded keypair to allow for
-- creation of a not null constraint.
INSERT INTO encryption_keypair (
  encryption_keypair_id,
  public_key,
  private_key,
  public_key_format_id,
  private_key_format_id,
  key_size
)
VALUES (
  '8fe94ffd-4679-4e42-b1d1-eebac489203e',
  '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',
  'X509',
  'PKCS8',
  2048
);

-- Apply throwaway shared keypair to existing throwaway account-study records
UPDATE account_study SET encryption_keypair_id = '8fe94ffd-4679-4e42-b1d1-eebac489203e';

-- Now we can enforce non-null
ALTER TABLE account_study ALTER COLUMN encryption_keypair_id SET NOT NULL;

-- See these Beiwe files:
-- https://github.com/onnela-lab/beiwe-backend/blob/main/database/study_models.py
-- https://github.com/onnela-lab/beiwe-backend/blob/main/constants/study_constants.py

CREATE TABLE study_beiwe_config (
  study_beiwe_config_id UUID NOT NULL PRIMARY KEY DEFAULT uuid_generate_v4(),
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

-- Add a couple of testing studies to COBALT
INSERT INTO study (study_id, institution_id, name, minutes_between_check_ins, grace_period_in_minutes)
VALUES ('37d105f1-04cd-46cd-a8c3-3910abe87e2c', 'COBALT', 'Cobalt Study', 10080, 0);

INSERT INTO study (study_id, institution_id, name, minutes_between_check_ins, grace_period_in_minutes)
VALUES ('924fb45d-804d-453d-8dc2-bbc11d4dc9d8', 'COBALT', 'Cobalt Test Study', 2, 0);

-- Permit USERNAME accounts in COBALT institution
--insert into institution_account_source (institution_account_source_id, institution_id, account_source_id, account_source_display_style_id, display_order, authentication_description, visible)
--values (uuid_generate_v4(), 'COBALT', 'USERNAME', 'TERTIARY', 4, 'Username and Password', FALSE);

-- Add a Beiwe config for each existing study
INSERT INTO study_beiwe_config (study_id) SELECT study_id FROM study;

COMMIT;