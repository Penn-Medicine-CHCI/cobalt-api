BEGIN;
SELECT _v.register_patch('120-hidden-ias', NULL, NULL);

ALTER TABLE institution_account_source ADD COLUMN visible BOOLEAN NOT NULL DEFAULT TRUE;

COMMIT;