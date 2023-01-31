BEGIN;
SELECT _v.register_patch('055-institution-team-member-subtitle', NULL, NULL);

ALTER TABLE institution_team_member ADD COLUMN subtitle TEXT;

COMMIT;