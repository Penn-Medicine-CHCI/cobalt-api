BEGIN;
SELECT _v.register_patch('213-more-provider-triage-recommended-feature-overrides', NULL, NULL);

-- Override booking link title, e.g. 'Schedule with Psychotherapist'
ALTER TABLE institution_feature ADD COLUMN recommendation_booking_title_override TEXT;

-- Override booking link URL, e.g. '/connect-with-support/psychotherapist'
ALTER TABLE institution_feature ADD COLUMN recommendation_booking_url_override TEXT;

COMMIT;