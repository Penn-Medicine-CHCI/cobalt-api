BEGIN;
SELECT _v.register_patch('273-provider-eligibility-pools', ARRAY['173-ic-department-scheduling-overrides'], NULL);

-- A pool defines the providers that may be shown for an order routed through
-- an Epic department. Availability still comes from the department used for
-- scheduling, which may be the routing department's scheduling override.
CREATE TABLE provider_eligibility_pool (
  provider_eligibility_pool_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  institution_id VARCHAR NOT NULL REFERENCES institution(institution_id),
  name TEXT NOT NULL,
  created TIMESTAMPTZ NOT NULL DEFAULT now(),
  last_updated TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT provider_eligibility_pool_nonempty_name CHECK (LENGTH(TRIM(name)) > 0)
);

CREATE UNIQUE INDEX provider_eligibility_pool_institution_name_unique_idx
  ON provider_eligibility_pool USING btree (institution_id, LOWER(TRIM(name)));

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON provider_eligibility_pool
  FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE TABLE provider_eligibility_pool_provider (
  provider_eligibility_pool_id UUID NOT NULL
    REFERENCES provider_eligibility_pool(provider_eligibility_pool_id) ON DELETE CASCADE,
  provider_id UUID NOT NULL REFERENCES provider(provider_id),
  created TIMESTAMPTZ NOT NULL DEFAULT now(),
  last_updated TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (provider_eligibility_pool_id, provider_id)
);

CREATE INDEX provider_eligibility_pool_provider_provider_id_idx
  ON provider_eligibility_pool_provider USING btree (provider_id);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON provider_eligibility_pool_provider
  FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE FUNCTION validate_provider_eligibility_pool_provider()
RETURNS TRIGGER AS $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM provider_eligibility_pool pep
    JOIN provider p ON p.provider_id=NEW.provider_id
    WHERE pep.provider_eligibility_pool_id=NEW.provider_eligibility_pool_id
    AND pep.institution_id=p.institution_id
  ) THEN
    RAISE EXCEPTION 'Provider eligibility pool memberships must use a provider from the same institution as the pool.';
  END IF;

  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER validate_provider_eligibility_pool_provider
  BEFORE INSERT OR UPDATE OF provider_eligibility_pool_id, provider_id
  ON provider_eligibility_pool_provider
  FOR EACH ROW EXECUTE FUNCTION validate_provider_eligibility_pool_provider();

-- NULL preserves the existing provider_epic_department behavior. Assigning an
-- empty pool intentionally makes no providers eligible for the department.
ALTER TABLE epic_department
  ADD COLUMN provider_eligibility_pool_id UUID
  REFERENCES provider_eligibility_pool(provider_eligibility_pool_id);

CREATE INDEX epic_department_provider_eligibility_pool_id_idx
  ON epic_department USING btree (provider_eligibility_pool_id);

CREATE FUNCTION validate_epic_department_provider_eligibility_pool()
RETURNS TRIGGER AS $$
BEGIN
  IF NEW.provider_eligibility_pool_id IS NOT NULL AND NOT EXISTS (
    SELECT 1
    FROM provider_eligibility_pool pep
    WHERE pep.provider_eligibility_pool_id=NEW.provider_eligibility_pool_id
    AND pep.institution_id=NEW.institution_id
  ) THEN
    RAISE EXCEPTION 'Epic departments must use a provider eligibility pool from the same institution.';
  END IF;

  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER validate_epic_department_provider_eligibility_pool
  BEFORE INSERT OR UPDATE OF institution_id, provider_eligibility_pool_id
  ON epic_department
  FOR EACH ROW EXECUTE FUNCTION validate_epic_department_provider_eligibility_pool();

COMMIT;
