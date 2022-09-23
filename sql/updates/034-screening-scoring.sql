BEGIN;
SELECT _v.register_patch('034-screening-scoring', NULL, NULL);

-- Create new 'score' JSONB column
ALTER TABLE screening_session_screening ADD COLUMN score_temp JSONB NOT NULL DEFAULT '{"overallScore":0}'::JSONB;

-- Migrate over numeric score data to JSON
UPDATE screening_session_screening SET score_temp=('{"overallScore":' || score || '}')::JSONB;

-- Temporarily drop view
DROP VIEW v_screening_session_screening;

-- Rename old 'score' integer column (don't delete yet)
ALTER TABLE screening_session_screening RENAME COLUMN score TO legacy_score;
ALTER TABLE screening_session_screening ALTER COLUMN legacy_score DROP NOT NULL;
ALTER TABLE screening_session_screening ALTER COLUMN legacy_score DROP DEFAULT;

-- Slide new 'score' JSONB column into place
ALTER TABLE screening_session_screening RENAME COLUMN score_temp TO score;

-- Now that new field is in place, recreate the view
CREATE VIEW v_screening_session_screening AS
SELECT *
FROM screening_session_screening
WHERE valid=TRUE;

COMMIT;