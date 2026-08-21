BEGIN;
SELECT _v.register_patch('263-care-encounter-notes', ARRAY['262-care-navigator-appointment-cancellation'], NULL);

CREATE TABLE care_encounter_note (
	care_encounter_note_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	care_encounter_id UUID NOT NULL REFERENCES care_encounter(care_encounter_id),
	note TEXT NOT NULL,
	created_by_account_id UUID NOT NULL REFERENCES account(account_id),
	last_updated_by_account_id UUID NOT NULL REFERENCES account(account_id),
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	CONSTRAINT care_encounter_note_not_blank_check CHECK (NULLIF(BTRIM(note), '') IS NOT NULL)
);

CREATE INDEX care_encounter_note_encounter_created_idx
ON care_encounter_note(care_encounter_id, created DESC, care_encounter_note_id DESC);

-- Preserve every note created before encounter notes became first-class records.
INSERT INTO care_encounter_note (
	care_encounter_id,
	note,
	created_by_account_id,
	last_updated_by_account_id,
	created,
	last_updated
)
SELECT care_encounter_id,
	notes,
	created_by_account_id,
	last_updated_by_account_id,
	created,
	last_updated
FROM care_encounter
WHERE NULLIF(BTRIM(notes), '') IS NOT NULL;

ALTER TABLE care_encounter
DROP COLUMN notes;

CREATE TRIGGER set_last_updated
BEFORE INSERT OR UPDATE ON care_encounter_note
FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE TRIGGER care_encounter_note_footprint
AFTER INSERT OR UPDATE OR DELETE ON care_encounter_note
FOR EACH ROW EXECUTE PROCEDURE perform_footprint();

COMMIT;
