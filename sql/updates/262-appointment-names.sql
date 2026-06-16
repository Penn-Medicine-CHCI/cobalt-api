BEGIN;
SELECT _v.register_patch('262-appointment-names', NULL, NULL);

ALTER TABLE appointment ADD COLUMN first_name TEXT NULL;
ALTER TABLE appointment ADD COLUMN last_name TEXT NULL;

UPDATE appointment app
SET first_name = a.first_name,
    last_name = a.last_name
FROM account a
WHERE a.account_id = app.account_id;

COMMIT;
