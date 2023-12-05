BEGIN;
SELECT _v.register_patch('137-suds-thermometer', NULL, NULL);

-- Optional title for screening prompts
ALTER TABLE screening_confirmation_prompt ADD COLUMN title_text TEXT;

INSERT INTO screening_image VALUES ('SUDS_THERMOMETER', 'SUDS Themometer');

COMMIT;