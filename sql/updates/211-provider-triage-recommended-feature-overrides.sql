BEGIN;
SELECT _v.register_patch('211-provider-triage-recommended-feature-overrides', NULL, NULL);

-- If a feature is recommended, support totally custom copy for the recommendation description
-- (normally it's something like "Psychotherapy Recommended" in the UI).
ALTER TABLE institution_feature ADD COLUMN recommendation_title_override TEXT;

-- If a feature is recommended, support totally custom copy for the recommendation description.
-- For example, if you are recommended Psychotherapy, we might want to have special institution-specific details about booking.
-- Can include HTML.
ALTER TABLE institution_feature ADD COLUMN recommendation_description_override TEXT;

COMMIT;