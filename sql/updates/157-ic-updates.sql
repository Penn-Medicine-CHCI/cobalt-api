BEGIN;
SELECT _v.register_patch('157-ic-updates', NULL, NULL);

-- Enables gradual "turning on" for accepting orders from specific departments.
-- We receive the full universe of orders via HL7 messages, but we should ignore any for which the
-- referring practice is not marked as patient_order_automatic_import_enabled=true
ALTER TABLE epic_department ADD COLUMN patient_order_automatic_import_enabled BOOLEAN NOT NULL DEFAULT FALSE;

-- Ability to close out orders with "patient was scheduled" reason
INSERT INTO patient_order_closure_reason VALUES ('SCHEDULED_WITH_BHS', 'Scheduled With BHS', 10);

-- Introduce concept of screening session metadata, so we can attach arbitrary data to a screening session.
-- Example: some institutions might need special UI-driven modifiers, e.g. start a "Standard" vs "Abbreviated" screening session
ALTER TABLE screening_session ADD COLUMN metadata JSONB NULL;

COMMIT;