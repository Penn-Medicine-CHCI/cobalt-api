BEGIN;
SELECT _v.register_patch('166-epic-department-synonyms', NULL, NULL);

CREATE TABLE epic_department_synonym (
  epic_department_synonym_id UUID NOT NULL PRIMARY KEY DEFAULT uuid_generate_v4(),
  epic_department_id UUID NOT NULL REFERENCES epic_department,
  name TEXT NOT NULL,
  created timestamptz NOT NULL DEFAULT now(),
  last_updated timestamptz NOT NULL
);

CREATE UNIQUE INDEX epic_department_synonym_name_idx ON epic_department_synonym USING btree (epic_department_id, name);
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON epic_department_synonym FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

COMMIT;