BEGIN;
SELECT _v.register_patch('214-ic-self-referral-overrides', NULL, NULL);

-- Optional override for copy at the top of the landing page after patient signs in to IC.
-- e.g. "Thank you for participating in this pilot. Follow the steps below to determine eligibility to self-schedule with ..."
ALTER TABLE institution ADD COLUMN integrated_care_patient_intro_override TEXT;

COMMIT;