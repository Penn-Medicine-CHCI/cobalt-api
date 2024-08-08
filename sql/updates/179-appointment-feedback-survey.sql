BEGIN;
SELECT _v.register_patch('179-appointment-feedback-survey', NULL, NULL);

-- Control institution-wide feedback surveys following 1:1 appointments.
-- Default of 1440 minutes (24 hours) after the appointment start date/time
ALTER TABLE institution ADD COLUMN appointment_feedback_survey_enabled BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE institution ADD COLUMN appointment_feedback_survey_url TEXT; -- e.g. "https://www.surveywebsite.com/abc"
ALTER TABLE institution ADD COLUMN appointment_feedback_survey_duration_description TEXT;  -- e.g. "1-2 minutes"
ALTER TABLE institution ADD COLUMN appointment_feedback_survey_delay_in_minutes INTEGER NOT NULL DEFAULT 1440; -- 1440 = 24 hours

-- Types of scheduled messages we might send for appointments
CREATE TABLE appointment_scheduled_message_type (
  appointment_scheduled_message_type_id VARCHAR PRIMARY KEY,
  description VARCHAR NOT NULL
);

-- Currently this is just for feedback survey followups, might be other types in the future
INSERT INTO appointment_scheduled_message_type VALUES ('FEEDBACK_SURVEY', 'Feedback Survey');

-- Keep track of scheduled messages for appointments
CREATE TABLE appointment_scheduled_message (
  appointment_scheduled_message_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  appointment_scheduled_message_type_id VARCHAR NOT NULL REFERENCES appointment_scheduled_message_type,
  appointment_id UUID NOT NULL REFERENCES appointment,
  scheduled_message_id UUID NOT NULL REFERENCES scheduled_message,
  created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX appointment_scheduled_message_unique_idx ON appointment_scheduled_message USING btree (appointment_id, scheduled_message_id);
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON appointment_scheduled_message FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

COMMIT;