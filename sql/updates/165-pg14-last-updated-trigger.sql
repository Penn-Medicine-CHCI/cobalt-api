BEGIN;
SELECT _v.register_patch('165-pg14-last-updated-trigger', NULL, NULL);

-- Needed for Postgres 14+
-- Current trigger uses the string 'now' which has strange behavior on 14+.
CREATE OR REPLACE FUNCTION set_last_updated() RETURNS TRIGGER AS $$
BEGIN
	NEW.last_updated := now();
	RETURN NEW;
END;
$$ LANGUAGE plpgsql;

COMMIT;