BEGIN;
SELECT _v.register_patch('253-account-alert', NULL, NULL);

ALTER TABLE institution ADD COLUMN continuing_education_url TEXT;

-- Account-specific alert assignments.
-- Use this when an alert should be shown only to selected accounts instead of
-- everyone in an institution.
CREATE TABLE account_alert (
	account_alert_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	account_id UUID NOT NULL REFERENCES account,
	alert_id UUID NOT NULL REFERENCES alert,
	active BOOLEAN NOT NULL DEFAULT TRUE,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX account_alert_unique_idx ON account_alert USING btree (account_id, alert_id);
CREATE INDEX account_alert_account_id_active_idx ON account_alert USING btree (account_id, active);
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON account_alert FOR EACH ROW EXECUTE PROCEDURE set_last_updated();


UPDATE institution
SET continuing_education_url='/pages/continuing-education'
WHERE institution_id='COBALT_COURSES';

INSERT INTO alert (alert_id, alert_type_id, title, message)
VALUES (
           '8359e4fd-2407-45f1-8036-c2fed6e2ea43',
           'INFORMATION',
           'Continuing Education',
           'Continuing Education credits are available for free for each course. <a href="/pages/continuing-education">Learn more about CE credits</a>'
       )
    ON CONFLICT (alert_id) DO UPDATE
                                  SET title = EXCLUDED.title,
                                  message = EXCLUDED.message,
                                  alert_type_id = EXCLUDED.alert_type_id;

-- This is just for local - set screening_question_id as appropriate for other envs in enterprise script
UPDATE screening_question
SET metadata = coalesce(metadata, '{}'::jsonb) || jsonb_build_object(
        'behaviorBridgeProviderQuestion', true,
        'continuingEducationAlertId', '8359e4fd-2407-45f1-8036-c2fed6e2ea43')
WHERE screening_question_id = '47b74b29-cd86-4114-8810-abb2a15a33ba';

-- This is just for local - set screening_question_id as appropriate for other envs in enterprise script
UPDATE screening_answer_option
SET metadata = coalesce(metadata, '{}'::jsonb) || jsonb_build_object(
        'behaviorBridgeProvider', true)
WHERE screening_answer_option_id = '35497b95-109a-4efd-b2d5-c71fa5e1d3a5';

COMMIT;
