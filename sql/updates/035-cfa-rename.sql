BEGIN;
SELECT _v.register_patch('035-cfa-rename', NULL, NULL);

UPDATE support_role SET description = 'Coping First Aid Coach' WHERE support_role_id='COACH';

COMMIT;