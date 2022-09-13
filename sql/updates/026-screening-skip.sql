BEGIN;
SELECT _v.register_patch('026-screening-skip', NULL, NULL);

ALTER TABLE screening_flow_version ADD COLUMN skippable BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE screening_session ADD COLUMN skipped BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE screening_session ADD COLUMN skipped_at TIMESTAMPTZ;

-- These should have existed previously
ALTER TABLE screening_session ADD COLUMN completed_at TIMESTAMPTZ;
ALTER TABLE screening_session ADD COLUMN crisis_indicated_at TIMESTAMPTZ;

-- Update existing records appropriately
UPDATE screening_session SET completed_at=last_updated WHERE completed=TRUE;
UPDATE screening_session SET crisis_indicated_at=last_updated WHERE crisis_indicated=TRUE;

COMMIT;