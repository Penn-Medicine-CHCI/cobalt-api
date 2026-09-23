BEGIN;
SELECT _v.register_patch(
  '272-local-only-cobalt-ic-ease-mhic-test-accounts',
  ARRAY['271-local-only-cobalt-ic-ease'],
  NULL
);

-- EASE-specific MHIC fixtures. All accounts use password "test1234".
-- Keep these aliases distinct from the PENN_IC fixtures because some legacy
-- account lookups are broader than institution_id.
UPDATE account
SET test_account=TRUE
WHERE account_id='d21fc1ef-4589-44f8-b6cd-6501901985d7'
AND institution_id='COBALT_IC_EASE';

INSERT INTO account (
  account_id,
  role_id,
  institution_id,
  account_source_id,
  first_name,
  last_name,
  display_name,
  email_address,
  password,
  locale,
  time_zone,
  active,
  test_account
) VALUES
  (
    '62f8b4af-ad44-47a5-81f8-524786af834e',
    'MHIC',
    'COBALT_IC_EASE',
    'EMAIL_PASSWORD',
    'Adam',
    'Grayson',
    'Adam Grayson',
    'agrayson+ease-mhic@xmog.com',
    '$2a$10$M2tPoJ8eQr55OW4iOfpbBOpgqFWt0LxnvVBnW1a/1LhKNA6SuUN42',
    'en-US',
    'America/New_York',
    TRUE,
    TRUE
  ),
  (
    '6c0d75ea-26d2-4e46-ac03-d912aa699517',
    'MHIC',
    'COBALT_IC_EASE',
    'EMAIL_PASSWORD',
    'Jess',
    'Gaito',
    'Jess Gaito',
    'jess.gaito+ease-mhic@xmog.com',
    '$2a$10$M2tPoJ8eQr55OW4iOfpbBOpgqFWt0LxnvVBnW1a/1LhKNA6SuUN42',
    'en-US',
    'America/New_York',
    TRUE,
    TRUE
  );

INSERT INTO account_capability (account_id, account_capability_type_id) VALUES
  ('62f8b4af-ad44-47a5-81f8-524786af834e', 'MHIC_REPORT_VIEWER'),
  ('62f8b4af-ad44-47a5-81f8-524786af834e', 'MHIC_DEPARTMENT_ADMIN'),
  ('62f8b4af-ad44-47a5-81f8-524786af834e', 'MHIC_ORDER_SERVICER'),
  ('6c0d75ea-26d2-4e46-ac03-d912aa699517', 'MHIC_REPORT_VIEWER'),
  ('6c0d75ea-26d2-4e46-ac03-d912aa699517', 'MHIC_DEPARTMENT_ADMIN'),
  ('6c0d75ea-26d2-4e46-ac03-d912aa699517', 'MHIC_ORDER_SERVICER');

COMMIT;
