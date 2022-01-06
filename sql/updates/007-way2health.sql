BEGIN;
SELECT _v.register_patch('007-way2health', NULL, NULL);

ALTER TABLE institution ADD COLUMN metadata JSONB NULL;

CREATE TABLE way2health_incident (
  way2health_incident_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  institution_id VARCHAR NOT NULL REFERENCES institution,
  incident_id BIGINT NOT NULL,
  study_id BIGINT NOT NULL,
  raw_json JSONB NOT NULL,
  created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
  last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_way2health_incident_incident_id ON way2health_incident (incident_id);
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON way2health_incident FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

END;